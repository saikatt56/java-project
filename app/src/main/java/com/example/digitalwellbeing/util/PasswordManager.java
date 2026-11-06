package com.example.digitalwellbeing.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class PasswordManager {

    private static final String PREFS = "wellbeing_prefs";
    private static final String KEY_PASSWORD = "app_password";
    private static boolean unlocked = false;

    private PasswordManager() {
    }

    public static boolean hasPassword(Context context) {
        String pwd = prefs(context).getString(KEY_PASSWORD, null);
        return !TextUtils.isEmpty(pwd);
    }

    public static String getPassword(Context context) {
        return prefs(context).getString(KEY_PASSWORD, null);
    }

    public static void setPassword(Context context, String password) {
        prefs(context).edit().putString(KEY_PASSWORD, password).apply();
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static void setUnlocked(boolean u) {
        unlocked = u;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
