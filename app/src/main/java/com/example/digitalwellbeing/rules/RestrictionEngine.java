package com.example.digitalwellbeing.rules;

import com.example.digitalwellbeing.data.entity.AppRule;

/**
 * Pure-Java rule evaluation. Deliberately free of Android imports so that the
 * same logic can be driven by the accessibility service, by scheduled work and
 * by JUnit tests on the JVM (proposal 3.4 -- "the rule engine should be
 * independent of the UI").
 */
public class RestrictionEngine {

    /**
     * Evaluates one rule. Rules are checked strongest first: a blocklisted app
     * is blocked even if it also happens to be inside its allowed window.
     *
     * @param rule     the user's rule, or null when the app is unrestricted
     * @param snapshot current usage and clock state
     */
    public Decision evaluate(AppRule rule, UsageSnapshot snapshot) {
        if (rule == null || !rule.enabled) {
            return Decision.allow(0);
        }

        int percent = percentOfDailyLimit(rule, snapshot);

        if (rule.blocklisted) {
            return new Decision(InterventionType.BLOCK, Decision.REASON_BLOCKLIST,
                    rule.bypassAllowed, percent);
        }

        if (snapshot.blockedByFocusMode) {
            return new Decision(InterventionType.BLOCK, Decision.REASON_FOCUS_MODE,
                    rule.bypassAllowed, percent);
        }

        if (rule.hasTimeWindow() && !withinWindow(rule, snapshot.minuteOfDay)) {
            return new Decision(InterventionType.BLOCK, Decision.REASON_OUTSIDE_WINDOW,
                    rule.bypassAllowed, percent);
        }

        if (rule.dailyLimitMinutes > 0 && snapshot.minutesUsedToday >= rule.dailyLimitMinutes) {
            return new Decision(InterventionType.BLOCK, Decision.REASON_DAILY_LIMIT,
                    rule.bypassAllowed, percent);
        }

        if (rule.sessionLimitMinutes > 0
                && snapshot.minutesInCurrentSession >= rule.sessionLimitMinutes) {
            return new Decision(InterventionType.BLOCK, Decision.REASON_SESSION_LIMIT,
                    rule.bypassAllowed, percent);
        }

        // Under every hard limit: apply the configured soft intervention, if any.
        if (InterventionType.isSoft(rule.interventionType)) {
            return new Decision(rule.interventionType, Decision.REASON_NONE, true, percent);
        }

        return Decision.allow(percent);
    }

    /**
     * True when the current minute falls inside the allowed window. Windows
     * that wrap past midnight (e.g. 22:00-06:00) are supported.
     */
    public boolean withinWindow(AppRule rule, int minuteOfDay) {
        int start = rule.allowedStartMinute;
        int end = rule.allowedEndMinute;
        if (start == end) {
            return true;
        }
        if (start < end) {
            return minuteOfDay >= start && minuteOfDay < end;
        }
        return minuteOfDay >= start || minuteOfDay < end;
    }

    /** True when the schedule's day mask covers the given ISO day (1 = Monday). */
    public static boolean matchesDay(int daysMask, int isoDayOfWeek) {
        return (daysMask & (1 << (isoDayOfWeek - 1))) != 0;
    }

    private int percentOfDailyLimit(AppRule rule, UsageSnapshot snapshot) {
        if (rule.dailyLimitMinutes <= 0) {
            return 0;
        }
        long pct = 100L * snapshot.minutesUsedToday / rule.dailyLimitMinutes;
        return (int) Math.min(pct, 999L);
    }
}
