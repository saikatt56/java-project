package com.example.digitalwellbeing.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.AppRule;
import com.example.digitalwellbeing.notify.NotificationHelper;
import com.example.digitalwellbeing.usage.UsageStatsCollector;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/**
 * Raises the 80% and 100% overuse alerts. Each threshold fires once per app
 * per day, tracked in SharedPreferences so a restart does not re-notify.
 */
public class LimitWatchWorker extends Worker {

    private static final String PREFS = "limit_alerts";

    public LimitWatchWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.get(context);
        UsageStatsCollector collector = new UsageStatsCollector(context);
        int today = TimeUtils.today();

        List<AppRule> rules = db.ruleDao().active();
        for (AppRule rule : rules) {
            if (rule.dailyLimitMinutes <= 0) {
                continue;
            }
            long usedMs = db.usageDao().foregroundMsForApp(today, rule.packageName);
            int usedMinutes = (int) (usedMs / 60_000L);
            int percent = 100 * usedMinutes / rule.dailyLimitMinutes;

            int threshold = percent >= 100 ? 100 : (percent >= 80 ? 80 : 0);
            if (threshold == 0 || alreadyAlerted(context, rule.packageName, today, threshold)) {
                continue;
            }
            NotificationHelper.showLimitWarning(
                    context, collector.labelOf(rule.packageName), threshold);
            markAlerted(context, rule.packageName, today, threshold);
        }
        return Result.success();
    }

    private boolean alreadyAlerted(Context context, String pkg, int dayKey, int threshold) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(key(pkg, dayKey, threshold), false);
    }

    private void markAlerted(Context context, String pkg, int dayKey, int threshold) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key(pkg, dayKey, threshold), true)
                .apply();
    }

    private String key(String pkg, int dayKey, int threshold) {
        return dayKey + ":" + pkg + ":" + threshold;
    }
}
