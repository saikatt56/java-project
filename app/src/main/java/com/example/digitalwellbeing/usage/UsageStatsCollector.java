package com.example.digitalwellbeing.usage;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.rules.AppCategory;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads per-app foreground time from UsageStatsManager and launch counts from
 * UsageEvents. Two users can spend the same total minutes in an app with very
 * different opening patterns, so both are collected (proposal 3.4).
 */
public class UsageStatsCollector {

    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;

    public UsageStatsCollector(Context context) {
        this.context = context.getApplicationContext();
        this.usageStatsManager = (UsageStatsManager)
                this.context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = this.context.getPackageManager();
    }

    /** Collects records for the local day containing {@code atMillis}. */
    public List<UsageRecord> collectForDay(long atMillis) {
        long start = TimeUtils.startOfDay(atMillis);
        long end = Math.min(start + 86_400_000L, System.currentTimeMillis());
        int dayKey = TimeUtils.dayKey(atMillis);

        Map<String, Long> foreground = queryForegroundMs(start, end);
        Map<String, Integer> launches = queryLaunchCounts(start, end);

        List<UsageRecord> out = new ArrayList<>();
        for (Map.Entry<String, Long> entry : foreground.entrySet()) {
            String pkg = entry.getKey();
            long ms = entry.getValue();
            // Sub-second entries are scheduler noise, not real use.
            if (ms < 1_000L || isSelfOrSystem(pkg)) {
                continue;
            }
            UsageRecord record = new UsageRecord();
            record.dayKey = dayKey;
            record.packageName = pkg;
            record.appLabel = labelOf(pkg);
            record.category = AppCategory.guess(pkg);
            record.foregroundMs = ms;
            Integer count = launches.get(pkg);
            record.launchCount = count == null ? 0 : count;
            record.updatedAt = System.currentTimeMillis();
            out.add(record);
        }
        return out;
    }

    /**
     * queryUsageStats can return several buckets for the same package, so the
     * values are summed rather than overwritten.
     */
    private Map<String, Long> queryForegroundMs(long start, long end) {
        Map<String, Long> totals = new HashMap<>();
        if (usageStatsManager == null) {
            return totals;
        }
        List<UsageStats> stats =
                usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) {
            return totals;
        }
        for (UsageStats s : stats) {
            // Drop buckets that belong to an earlier day than the one requested.
            if (s.getLastTimeUsed() < start) {
                continue;
            }
            Long previous = totals.get(s.getPackageName());
            long value = (previous == null ? 0L : previous) + s.getTotalTimeInForeground();
            totals.put(s.getPackageName(), value);
        }
        return totals;
    }

    /** One launch = one MOVE_TO_FOREGROUND / ACTIVITY_RESUMED event. */
    private Map<String, Integer> queryLaunchCounts(long start, long end) {
        Map<String, Integer> counts = new HashMap<>();
        if (usageStatsManager == null) {
            return counts;
        }
        UsageEvents events = usageStatsManager.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String previousPackage = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() != UsageEvents.Event.ACTIVITY_RESUMED) {
                continue;
            }
            String pkg = event.getPackageName();
            // Activity-to-activity moves inside one app are not a new opening.
            if (pkg.equals(previousPackage)) {
                continue;
            }
            previousPackage = pkg;
            Integer current = counts.get(pkg);
            counts.put(pkg, current == null ? 1 : current + 1);
        }
        return counts;
    }

    /** Package in the foreground right now, or null if it cannot be determined. */
    public String currentForegroundPackage() {
        if (usageStatsManager == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        UsageEvents events = usageStatsManager.queryEvents(now - 10_000L, now);
        UsageEvents.Event event = new UsageEvents.Event();
        String latest = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                latest = event.getPackageName();
            }
        }
        return latest;
    }

    public String labelOf(String packageName) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    /**
     * Filters out this app and system components with no launcher entry. The
     * launcher itself is excluded because its foreground time is an artefact
     * of switching apps, not deliberate use.
     */
    private boolean isSelfOrSystem(String packageName) {
        if (context.getPackageName().equals(packageName)) {
            return true;
        }
        return packageManager.getLaunchIntentForPackage(packageName) == null;
    }
}
