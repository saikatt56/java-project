package com.example.digitalwellbeing;

import android.app.Application;

import com.example.digitalwellbeing.notify.NotificationHelper;
import com.example.digitalwellbeing.work.WorkScheduler;

/**
 * Application entry point. Creates notification channels and registers the
 * periodic WorkManager jobs described in section 3.2 of the proposal.
 */
public class WellbeingApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        WorkScheduler.schedulePeriodicWork(this);
    }
}
