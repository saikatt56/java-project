package com.example.digitalwellbeing.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.AppRule;
import com.example.digitalwellbeing.data.entity.FocusMode;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.rules.Decision;
import com.example.digitalwellbeing.rules.InterventionType;
import com.example.digitalwellbeing.rules.RestrictionEngine;
import com.example.digitalwellbeing.rules.UsageSnapshot;
import com.example.digitalwellbeing.ui.BlockActivity;
import com.example.digitalwellbeing.ui.BreathingActivity;
import com.example.digitalwellbeing.usage.SessionTracker;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/**
 * Watches window-state changes to learn which app is in the foreground and
 * launches the intervention screen when a rule matches.
 *
 * Scope note: this service reads only the package name of the active window.
 * It does not inspect text, form fields or screen content. See
 * docs/ACCESSIBILITY.md -- AccessibilityService is a privileged facility
 * intended to assist users with disabilities, and a project that plans public
 * distribution must review current platform policy before relying on it.
 */
public class ForegroundAppWatcherService extends AccessibilityService {

    private static final String TAG = "AppWatcher";

    /** Ignore repeat events for the same package inside this window. */
    private static final long DEBOUNCE_MS = 1_500L;

    private final RestrictionEngine engine = new RestrictionEngine();
    private final FocusModeResolver focusResolver = new FocusModeResolver();
    private final SessionTracker sessionTracker = new SessionTracker();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lastPackage;
    private long lastEventAt;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Foreground watcher connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null
                || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) {
            return;
        }

        String packageName = event.getPackageName().toString();
        long now = System.currentTimeMillis();

        if (packageName.equals(getPackageName())) {
            return; // never intervene on ourselves
        }
        if (packageName.equals(lastPackage) && now - lastEventAt < DEBOUNCE_MS) {
            return;
        }
        lastPackage = packageName;
        lastEventAt = now;
        sessionTracker.onForegroundChanged(packageName, now);

        AppDatabase.IO.execute(() -> evaluate(packageName, now));
    }

    /** Runs off the main thread: Room queries must not block the UI thread. */
    private void evaluate(String packageName, long now) {
        AppDatabase db = AppDatabase.get(this);
        AppRule rule = db.ruleDao().byPackage(packageName);
        if (rule == null || !rule.enabled) {
            return;
        }

        int minuteOfDay = TimeUtils.minuteOfDay(now);
        int isoDay = TimeUtils.isoDayOfWeek(now);

        List<FocusMode> modes = db.focusDao().enabledModes();
        FocusMode active = focusResolver.activeMode(modes, minuteOfDay, isoDay);
        boolean focusBlocks = focusResolver.blocks(active, packageName);

        long usedMs = db.usageDao().foregroundMsForApp(TimeUtils.today(), packageName);
        UsageSnapshot snapshot = new UsageSnapshot(
                (int) (usedMs / 60_000L),
                sessionTracker.minutesInSession(packageName, now),
                minuteOfDay,
                isoDay,
                active == null ? null : active.name,
                focusBlocks);

        Decision decision = engine.evaluate(rule, snapshot);
        if (!decision.interrupts()) {
            return;
        }

        InterventionEvent logEntry = new InterventionEvent();
        logEntry.timestamp = now;
        logEntry.packageName = packageName;
        logEntry.interventionType = decision.interventionType;
        logEntry.reason = decision.reason;
        logEntry.outcome = InterventionEvent.OUTCOME_SHOWN;
        logEntry.focusModeName = snapshot.activeFocusMode;
        long eventId = db.interventionDao().insert(logEntry);

        mainHandler.post(() -> launchIntervention(packageName, decision, eventId));
    }

    private void launchIntervention(String packageName, Decision decision, long eventId) {
        Intent intent = decision.isHardBlock()
                ? new Intent(this, BlockActivity.class)
                : new Intent(this, BreathingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(BlockActivity.EXTRA_PACKAGE, packageName);
        intent.putExtra(BlockActivity.EXTRA_REASON, decision.reason);
        intent.putExtra(BlockActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(BlockActivity.EXTRA_BYPASS_ALLOWED, decision.bypassAllowed);
        intent.putExtra(BlockActivity.EXTRA_INTERVENTION_TYPE, decision.interventionType);
        startActivity(intent);

        if (decision.interventionType == InterventionType.BLOCK) {
            // Return the user to where they were rather than leaving the blocked
            // app underneath the overlay.
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }

    @Override
    public void onInterrupt() {
        sessionTracker.reset();
    }
}
