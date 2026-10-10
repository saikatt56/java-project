package com.example.digitalwellbeing.rules;

import java.util.HashMap;
import java.util.Map;

/**
 * Coarse categories used for the dashboard breakdown. Android's own
 * ApplicationInfo.category is unreliable across OEM builds, so a small
 * seed map is used and the user can override the result per app.
 */
public final class AppCategory {

    public static final int OTHER = 0;
    public static final int SOCIAL = 1;
    public static final int ENTERTAINMENT = 2;
    public static final int PRODUCTIVITY = 3;
    public static final int COMMUNICATION = 4;
    public static final int GAMES = 5;

    private static final Map<String, Integer> SEED = new HashMap<>();

    static {
        SEED.put("com.facebook.katana", SOCIAL);
        SEED.put("com.instagram.android", SOCIAL);
        SEED.put("com.zhiliaoapp.musically", SOCIAL);     // TikTok
        SEED.put("com.twitter.android", SOCIAL);
        SEED.put("com.reddit.frontpage", SOCIAL);
        SEED.put("com.google.android.youtube", ENTERTAINMENT);
        SEED.put("com.netflix.mediaclient", ENTERTAINMENT);
        SEED.put("com.spotify.music", ENTERTAINMENT);
        SEED.put("com.whatsapp", COMMUNICATION);
        SEED.put("org.telegram.messenger", COMMUNICATION);
        SEED.put("com.google.android.gm", COMMUNICATION);
        SEED.put("com.microsoft.office.word", PRODUCTIVITY);
        SEED.put("com.google.android.keep", PRODUCTIVITY);
    }

    private AppCategory() {
    }

    public static int guess(String packageName) {
        Integer known = SEED.get(packageName);
        return known != null ? known : OTHER;
    }

    public static String label(int category) {
        switch (category) {
            case SOCIAL:
                return "Social media";
            case ENTERTAINMENT:
                return "Entertainment";
            case PRODUCTIVITY:
                return "Productivity";
            case COMMUNICATION:
                return "Communication";
            case GAMES:
                return "Games";
            default:
                return "Other";
        }
    }
}
