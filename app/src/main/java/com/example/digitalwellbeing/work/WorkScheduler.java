package com.example.digitalwellbeing.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Registers the periodic jobs. WorkManager is used rather than AlarmManager
 * so the schedule survives process death and reboot (proposal 3.2, 3.8).
 */
public final class WorkScheduler {

    private static final String WORK_AGGREGATION = "usage_aggregation";
    private static final String WORK_LIMITS = "limit_watch";
    private static final String WORK_SUMMARY = "daily_summary";

    private WorkScheduler() {
    }

    public static void schedulePeriodicWork(Context context) {
        WorkManager wm = WorkManager.getInstance(context);

        // 15 minutes is the platform minimum for periodic work.
        wm.enqueueUniquePeriodicWork(
                WORK_AGGREGATION,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(
                        UsageAggregationWorker.class, 15, TimeUnit.MINUTES)
                        .build());

        wm.enqueueUniquePeriodicWork(
                WORK_LIMITS,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(
                        LimitWatchWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(new Constraints.Builder()
                                .setRequiresBatteryNotLow(true)
                                .build())
                        .build());

        wm.enqueueUniquePeriodicWork(
                WORK_SUMMARY,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(DailySummaryWorker.class, 1, TimeUnit.DAYS)
                        .setInitialDelay(millisUntilNextMorning(), TimeUnit.MILLISECONDS)
                        .build());
    }

    /** Delay until the next 08:00 local time. */
    private static long millisUntilNextMorning() {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 8);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        if (target.getTimeInMillis() <= System.currentTimeMillis()) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }
        return target.getTimeInMillis() - System.currentTimeMillis();
    }
}
