package com.example.digitalwellbeing.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.usage.UsageStatsCollector;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/**
 * Soft intervention: a short breathing pause, then an explicit choice to
 * continue or leave. Based on the design-friction idea discussed in the
 * literature review -- the delay is short on purpose, since the point is to
 * interrupt habitual opening, not to punish the user.
 */
public class BreathingActivity extends AppCompatActivity {

    private static final long PAUSE_MILLIS = 8_000L;
    private static final long BREATH_CYCLE_MILLIS = 4_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long eventId;
    private long shownAt;
    private String packageName;
    private CountDownTimer timer;
    private ObjectAnimator breathAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        eventId = getIntent().getLongExtra(BlockActivity.EXTRA_EVENT_ID, -1L);
        packageName = getIntent().getStringExtra(BlockActivity.EXTRA_PACKAGE);
        shownAt = System.currentTimeMillis();

        TextView appNameTextView = findViewById(R.id.app_name);
        TextView appUsageStatsTextView = findViewById(R.id.app_usage_stats);
        loadAppUsageInfo(appNameTextView, appUsageStatsTextView);

        TextView countdown = findViewById(R.id.countdown);
        View circle = findViewById(R.id.breath_circle);
        Button continueButton = findViewById(R.id.continue_anyway);
        Button leaveButton = findViewById(R.id.leave_button);

        continueButton.setEnabled(false);
        startBreathAnimation(circle);

        timer = new CountDownTimer(PAUSE_MILLIS, 1_000L) {
            @Override
            public void onTick(long remaining) {
                countdown.setText(String.valueOf(remaining / 1000 + 1));
            }

            @Override
            public void onFinish() {
                countdown.setText(R.string.breathing_done);
                continueButton.setEnabled(true);
            }
        }.start();

        continueButton.setOnClickListener(v -> {
            record(InterventionEvent.OUTCOME_CONTINUED);
            openTargetApp();
            finish();
        });

        leaveButton.setOnClickListener(v -> {
            record(InterventionEvent.OUTCOME_CANCELLED);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                record(InterventionEvent.OUTCOME_CANCELLED);
                finish();
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

    private void startBreathAnimation(View circle) {
        breathAnimator = ObjectAnimator.ofPropertyValuesHolder(circle,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0.6f, 1f),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.6f, 1f));
        breathAnimator.setDuration(BREATH_CYCLE_MILLIS);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathAnimator.start();
    }

    private void openTargetApp() {
        if (packageName == null) {
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        }
    }

    private void record(int outcome) {
        long dwell = System.currentTimeMillis() - shownAt;
        long id = eventId;
        if (id > 0) {
            AppDatabase.IO.execute(() ->
                    AppDatabase.get(this).interventionDao().recordOutcome(id, outcome, dwell));
        }
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        if (breathAnimator != null) {
            breathAnimator.cancel();
        }
        super.onDestroy();
    }
}
