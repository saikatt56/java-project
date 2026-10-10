package com.example.digitalwellbeing.rules;

/** Result of evaluating a rule against a {@link UsageSnapshot}. */
public class Decision {

    public static final String REASON_NONE = "NONE";
    public static final String REASON_BLOCKLIST = "BLOCKLIST";
    public static final String REASON_DAILY_LIMIT = "DAILY_LIMIT";
    public static final String REASON_SESSION_LIMIT = "SESSION_LIMIT";
    public static final String REASON_OUTSIDE_WINDOW = "OUTSIDE_WINDOW";
    public static final String REASON_FOCUS_MODE = "FOCUS_MODE";

    public final int interventionType;
    public final String reason;
    public final boolean bypassAllowed;

    /** Percentage of the daily limit already used, 0 when no limit is set. */
    public final int limitPercentUsed;

    public Decision(int interventionType, String reason, boolean bypassAllowed, int limitPercentUsed) {
        this.interventionType = interventionType;
        this.reason = reason;
        this.bypassAllowed = bypassAllowed;
        this.limitPercentUsed = limitPercentUsed;
    }

    public static Decision allow(int limitPercentUsed) {
        return new Decision(InterventionType.NONE, REASON_NONE, true, limitPercentUsed);
    }

    public boolean interrupts() {
        return interventionType != InterventionType.NONE;
    }

    public boolean isHardBlock() {
        return interventionType == InterventionType.BLOCK;
    }
}
