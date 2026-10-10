package com.example.digitalwellbeing.rules;

/** Strength of the intervention applied when a rule matches (proposal 3.4). */
public final class InterventionType {

    /** No interruption; the attempt is only logged. */
    public static final int NONE = 0;

    /** Soft: short breathing / pause screen, then continue or leave. */
    public static final int BREATHING = 1;

    /** Soft: one-tap "do you still want to open this?" prompt. */
    public static final int CONFIRM = 2;

    /** Hard: full-screen block with an optional emergency bypass. */
    public static final int BLOCK = 3;

    private InterventionType() {
    }

    public static boolean isSoft(int type) {
        return type == BREATHING || type == CONFIRM;
    }

    public static String label(int type) {
        switch (type) {
            case BREATHING:
                return "Breathing pause";
            case CONFIRM:
                return "Confirmation prompt";
            case BLOCK:
                return "Hard block";
            default:
                return "Log only";
        }
    }
}
