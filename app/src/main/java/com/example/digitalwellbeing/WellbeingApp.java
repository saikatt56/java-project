package com.example.digitalwellbeing;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.notify.NotificationHelper;
import com.example.digitalwellbeing.usage.UsageStatsCollector;
import com.example.digitalwellbeing.util.PasswordManager;
import com.example.digitalwellbeing.util.PermissionUtils;
import com.example.digitalwellbeing.work.WorkScheduler;

import java.util.List;

/**
 * Application entry point. Creates notification channels and registers the
 * periodic WorkManager jobs described in section 3.2 of the proposal.
 */
public class WellbeingApp extends Application implements Application.ActivityLifecycleCallbacks {

    private int activeActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        WorkScheduler.schedulePeriodicWork(this);
        registerActivityLifecycleCallbacks(this);

        // Fetch usage data on app startup
        AppDatabase.IO.execute(() -> {
            if (PermissionUtils.hasUsageAccess(this)) {
                try {
                    UsageStatsCollector collector = new UsageStatsCollector(this);
                    long now = System.currentTimeMillis();
                    List<UsageRecord> today = collector.collectForDay(now);
                    List<UsageRecord> yesterday = collector.collectForDay(now - 86_400_000L);
                    AppDatabase db = AppDatabase.get(this);
                    db.usageDao().upsertAll(today);
                    db.usageDao().upsertAll(yesterday);
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        activeActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        activeActivityCount--;
        if (activeActivityCount == 0) {
            PasswordManager.setUnlocked(false);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
