package com.example.digitalwellbeing.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.rules.Decision;
import com.example.digitalwellbeing.usage.UsageStatsCollector;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/**
 * Hard-block screen. The emergency bypass is deliberately available but
 * effortful and always logged -- a silently defeated block would make the
 * intervention data meaningless (proposal 3.4, 3.8).
 */
public class BlockActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_BYPASS_ALLOWED = "bypass_allowed";
    public static final String EXTRA_INTERVENTION_TYPE = "intervention_type";

    private static final String BYPASS_PHRASE = "I need this now";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long eventId;
    private long shownAt;
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1L);
        packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
        shownAt = System.currentTimeMillis();

        TextView appNameTextView = findViewById(R.id.app_name);
        TextView appUsageStatsTextView = findViewById(R.id.app_usage_stats);
        loadAppUsageInfo(appNameTextView, appUsageStatsTextView);

        String reason = getIntent().getStringExtra(EXTRA_REASON);
        ((TextView) findViewById(R.id.block_reason)).setText(reasonText(reason));

        View bypassGroup = findViewById(R.id.bypass_group);
        EditText phrase = findViewById(R.id.bypass_phrase);
        Button bypass = findViewById(R.id.bypass_button);
        bypassGroup.setVisibility(
                getIntent().getBooleanExtra(EXTRA_BYPASS_ALLOWED, false) ? View.VISIBLE : View.GONE);

        bypass.setOnClickListener(v -> {
            if (!BYPASS_PHRASE.contentEquals(phrase.getText().toString().trim())) {
                Toast.makeText(this, R.string.bypass_phrase_wrong, Toast.LENGTH_SHORT).show();
                return;
            }
            finishWithOutcome(InterventionEvent.OUTCOME_BYPASSED);
        });

        ((Button) findViewById(R.id.close_button)).setOnClickListener(
                v -> finishWithOutcome(InterventionEvent.OUTCOME_CANCELLED));

        // Backing out of a block counts as cancelling, not as a bypass.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithOutcome(InterventionEvent.OUTCOME_CANCELLED);
            }
        });
    }

    private void loadAppUsageInfo(TextView appNameTextView, TextView appUsageStatsTextView) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        AppDatabase.IO.execute(() -> {
            UsageStatsCollector collector = new UsageStatsCollector(this);
            String label = collector.labelOf(packageName);

            long foregroundMs = 0;
            int launchCount = 0;

            List<UsageRecord> records = collector.collectForDay(System.currentTimeMillis());
            UsageRecord target = null;
            for (UsageRecord r : records) {
                if (packageName.equals(r.packageName)) {
                    target = r;
                    break;
                }
            }

            if (target != null) {
                foregroundMs = target.foregroundMs;
                launchCount = target.launchCount;
                if (target.appLabel != null && !target.appLabel.isEmpty()) {
                    label = target.appLabel;
                }
            } else {
                UsageRecord dbRecord = AppDatabase.get(this).usageDao()
                        .recordForApp(TimeUtils.today(), packageName);
                if (dbRecord != null) {
                    foregroundMs = dbRecord.foregroundMs;
                    launchCount = dbRecord.launchCount;
                    if (dbRecord.appLabel != null && !dbRecord.appLabel.isEmpty()) {
                        label = dbRecord.appLabel;
                    }
                }
            }

            final String displayLabel = label;
            final long displayMs = foregroundMs;
            final int displayLaunches = launchCount;

            mainHandler.post(() -> {
                if (appNameTextView != null) {
                    appNameTextView.setText(displayLabel);
                }
                if (appUsageStatsTextView != null) {
                    String opensStr = getResources().getQuantityString(
                            R.plurals.launch_count_fmt, displayLaunches, displayLaunches);
                    String usageText = getString(R.string.app_usage_today_fmt,
                            TimeUtils.formatDuration(displayMs), opensStr);
                    appUsageStatsTextView.setText(usageText);
                }
            });
        });
    }

    private String reasonText(String reason) {
        if (reason == null) {
            return getString(R.string.block_generic);
        }
        switch (reason) {
            case Decision.REASON_DAILY_LIMIT:
                return getString(R.string.block_daily_limit);
            case Decision.REASON_SESSION_LIMIT:
                return getString(R.string.block_session_limit);
            case Decision.REASON_OUTSIDE_WINDOW:
                return getString(R.string.block_outside_window);
            case Decision.REASON_FOCUS_MODE:
                return getString(R.string.block_focus_mode);
            case Decision.REASON_BLOCKLIST:
                return getString(R.string.block_blocklist);
            default:
                return getString(R.string.block_generic);
        }
    }

    protected void finishWithOutcome(int outcome) {
        long dwell = System.currentTimeMillis() - shownAt;
        long id = eventId;
        if (id > 0) {
            AppDatabase.IO.execute(() ->
                    AppDatabase.get(this).interventionDao().recordOutcome(id, outcome, dwell));
        }
        finish();
    }

    /** Home button is not intercepted; the screen simply ends as cancelled. */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finishWithOutcome(InterventionEvent.OUTCOME_CANCELLED);
    }
}
