package com.example.digitalwellbeing.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Generates and stores the anonymous participant identifier used in research
 * exports (proposal 1.3, 3.4). No name, email, phone number or advertising ID
 * is collected; the UUID is random and local to the installation.
 */
public final class ParticipantId {

    private static final String PREFS = "wellbeing_prefs";
    private static final String KEY_UUID = "participant_uuid";
    private static final String KEY_CONSENT = "research_consent";

    private ParticipantId() {
    }

    public static String get(Context context) {
        SharedPreferences prefs = prefs(context);
        String existing = prefs.getString(KEY_UUID, null);
        if (existing != null) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_UUID, generated).apply();
        return generated;
    }

    /** Regenerating the UUID detaches all future exports from earlier ones. */
    public static String regenerate(Context context) {
        String generated = UUID.randomUUID().toString();
        prefs(context).edit().putString(KEY_UUID, generated).apply();
        return generated;
    }

    public static boolean hasResearchConsent(Context context) {
        return prefs(context).getBoolean(KEY_CONSENT, false);
    }

    public static void setResearchConsent(Context context, boolean consented) {
        prefs(context).edit().putBoolean(KEY_CONSENT, consented).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
