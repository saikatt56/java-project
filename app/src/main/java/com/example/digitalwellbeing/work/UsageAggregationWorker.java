package com.example.digitalwellbeing.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.usage.UsageStatsCollector;
import com.example.digitalwellbeing.util.PermissionUtils;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/**
 * Periodically snapshots UsageStatsManager into Room. Running on a schedule
 * rather than polling keeps battery cost low (proposal 3.8, battery risk).
 */
public class UsageAggregationWorker extends Worker {

    /** Usage records older than this are pruned. */
    private static final int RETENTION_DAYS = 90;

    public UsageAggregationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!PermissionUtils.hasUsageAccess(context)) {
            // Nothing to read yet; retry later instead of failing permanently.
            return Result.retry();
        }

        try {
            UsageStatsCollector collector = new UsageStatsCollector(context);
            long now = System.currentTimeMillis();

            // Today plus yesterday: a job that runs just after midnight must
            // still close out the previous day's totals.
            List<UsageRecord> today = collector.collectForDay(now);
            List<UsageRecord> yesterday = collector.collectForDay(now - 86_400_000L);

            AppDatabase db = AppDatabase.get(context);
            db.usageDao().upsertAll(today);
            db.usageDao().upsertAll(yesterday);
            db.usageDao().deleteBefore(TimeUtils.dayKeyOffset(TimeUtils.today(), -RETENTION_DAYS));

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
