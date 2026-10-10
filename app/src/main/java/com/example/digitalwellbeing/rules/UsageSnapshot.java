package com.example.digitalwellbeing.rules;

/**
 * Everything the rule engine needs to know about the current moment, passed
 * in rather than read from Android APIs so the engine stays unit-testable
 * (proposal 3.7, unit testing).
 */
public class UsageSnapshot {

    /** Minutes the app has been in the foreground today. */
    public final int minutesUsedToday;

    /** Minutes of the current continuous session, 0 if not running. */
    public final int minutesInCurrentSession;

    /** Minutes since local midnight. */
    public final int minuteOfDay;

    /** 1 = Monday ... 7 = Sunday (ISO-8601). */
    public final int isoDayOfWeek;

    /** Name of the active focus mode, or null. */
    public final String activeFocusMode;

    /** True when the active focus mode blocks this package. */
    public final boolean blockedByFocusMode;

    public UsageSnapshot(int minutesUsedToday,
                         int minutesInCurrentSession,
                         int minuteOfDay,
                         int isoDayOfWeek,
                         String activeFocusMode,
                         boolean blockedByFocusMode) {
        this.minutesUsedToday = minutesUsedToday;
        this.minutesInCurrentSession = minutesInCurrentSession;
        this.minuteOfDay = minuteOfDay;
        this.isoDayOfWeek = isoDayOfWeek;
        this.activeFocusMode = activeFocusMode;
        this.blockedByFocusMode = blockedByFocusMode;
    }
}
