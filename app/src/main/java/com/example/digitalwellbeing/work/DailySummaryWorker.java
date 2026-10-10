package com.example.digitalwellbeing.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.dao.UsageDao;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.notify.NotificationHelper;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/** Posts the end-of-day summary and updates the streak counter. */
public class DailySummaryWorker extends Worker {

    private static final String PREFS = "streaks";
    private static final String KEY_STREAK = "current_streak";
    private static final String KEY_LAST_DAY = "last_counted_day";
    private static final String KEY_TARGET = "daily_target_minutes";

    public DailySummaryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.get(context);

        int day = TimeUtils.dayKeyOffset(TimeUtils.today(), -1);
        long totalMs = db.usageDao().totalForegroundMs(day);
        List<UsageRecord> records = db.usageDao().forDay(day);

        String topApp = records.isEmpty() ? "-" : records.get(0).appLabel;
        int streak = updateStreak(context, day, totalMs);

        String body = "Total " + TimeUtils.formatDuration(totalMs)
                + " \u00b7 most used: " + topApp
                + (streak > 0 ? " \u00b7 " + streak + "-day streak" : "");
        NotificationHelper.showSummary(context, "Yesterday's usage", body);

        return Result.success();
    }

    /** A day counts toward the streak when total screen time is under target. */
    private int updateStreak(Context context, int dayKey, long totalMs) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int target = prefs.getInt(KEY_TARGET, 0);
        if (target <= 0) {
            return 0; // no target set, so nothing to count
        }
        if (prefs.getInt(KEY_LAST_DAY, 0) == dayKey) {
            return prefs.getInt(KEY_STREAK, 0); // already counted
        }
        int minutes = (int) (totalMs / 60_000L);
        int streak = minutes <= target ? prefs.getInt(KEY_STREAK, 0) + 1 : 0;
        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putInt(KEY_LAST_DAY, dayKey)
                .apply();
        return streak;
    }
}
