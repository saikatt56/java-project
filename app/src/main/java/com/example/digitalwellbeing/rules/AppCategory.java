package com.example.digitalwellbeing.rules;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coarse categories used for the dashboard breakdown. Android's own
 * ApplicationInfo.category is unreliable across OEM builds, so a small
 * seed map is used and the user can override, create, or delete categories.
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

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences("app_categories_prefs", Context.MODE_PRIVATE);
    }

    public static int guess(String packageName) {
        Integer known = SEED.get(packageName);
        return known != null ? known : OTHER;
    }

    public static int guess(Context context, String packageName) {
        if (context != null && packageName != null) {
            SharedPreferences prefs = getPrefs(context);
            int override = prefs.getInt("app_cat_" + packageName, -1);
            if (override != -1) {
                if (!isDeleted(context, override)) {
                    return override;
                }
            }
        }
        int seedCat = guess(packageName);
        if (context != null && isDeleted(context, seedCat)) {
            return OTHER;
        }
        return seedCat;
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

    public static String label(Context context, int category) {
        if (context != null) {
            if (category >= 100) {
                Map<Integer, String> custom = getCustomCategoriesMap(context);
                String name = custom.get(category);
                if (name != null) {
                    return name;
                }
            }
        }
        return label(category);
    }

    public static boolean isDeleted(Context context, int categoryId) {
        if (context == null || categoryId == OTHER) return false;
        SharedPreferences prefs = getPrefs(context);
        Set<String> deleted = prefs.getStringSet("deleted_cats", new HashSet<>());
        return deleted.contains(String.valueOf(categoryId));
    }

    public static List<CategoryItem> getAllCategories(Context context) {
        List<CategoryItem> list = new ArrayList<>();
        int[] builtInIds = {SOCIAL, ENTERTAINMENT, PRODUCTIVITY, COMMUNICATION, GAMES, OTHER};
        for (int id : builtInIds) {
            if (!isDeleted(context, id)) {
                list.add(new CategoryItem(id, label(id), false));
            }
        }

        if (context != null) {
            Map<Integer, String> custom = getCustomCategoriesMap(context);
            for (Map.Entry<Integer, String> entry : custom.entrySet()) {
                if (!isDeleted(context, entry.getKey())) {
                    list.add(new CategoryItem(entry.getKey(), entry.getValue(), true));
                }
            }
        }
        return list;
    }

    public static Map<Integer, String> getCustomCategoriesMap(Context context) {
        Map<Integer, String> map = new HashMap<>();
        SharedPreferences prefs = getPrefs(context);
        Set<String> set = prefs.getStringSet("custom_cats", new HashSet<>());
        for (String item : set) {
            String[] parts = item.split(":", 2);
            if (parts.length == 2) {
                try {
                    int id = Integer.parseInt(parts[0]);
                    map.put(id, parts[1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public static void createCategory(Context context, String name) {
        if (name == null || name.trim().isEmpty()) return;
        SharedPreferences prefs = getPrefs(context);
        Set<String> set = new HashSet<>(prefs.getStringSet("custom_cats", new HashSet<>()));
        int nextId = prefs.getInt("next_cat_id", 100);
        set.add(nextId + ":" + name.trim());
        prefs.edit()
                .putStringSet("custom_cats", set)
                .putInt("next_cat_id", nextId + 1)
                .apply();
    }

    public static void deleteCategory(Context context, int categoryId) {
        if (context == null || categoryId == OTHER) return;
        SharedPreferences prefs = getPrefs(context);

        Set<String> deleted = new HashSet<>(prefs.getStringSet("deleted_cats", new HashSet<>()));
        deleted.add(String.valueOf(categoryId));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("deleted_cats", deleted);

        if (categoryId >= 100) {
            Set<String> customSet = new HashSet<>(prefs.getStringSet("custom_cats", new HashSet<>()));
            String matchPrefix = categoryId + ":";
            customSet.removeIf(s -> s.startsWith(matchPrefix));
            editor.putStringSet("custom_cats", customSet);
        }

        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            if (key.startsWith("app_cat_")) {
                Object val = all.get(key);
                if (val instanceof Integer && (Integer) val == categoryId) {
                    editor.remove(key);
                }
            }
        }
        editor.apply();
    }

    public static void assignApp(Context context, String packageName, int categoryId) {
        if (packageName == null || context == null) return;
        getPrefs(context).edit().putInt("app_cat_" + packageName, categoryId).apply();
    }

    public static void unassignApp(Context context, String packageName) {
        unassignApp(context, packageName, OTHER);
    }

    public static void unassignApp(Context context, String packageName, int currentCategoryId) {
        if (packageName == null || context == null) return;
        if (currentCategoryId == OTHER) {
            getPrefs(context).edit().remove("app_cat_" + packageName).apply();
        } else {
            getPrefs(context).edit().putInt("app_cat_" + packageName, OTHER).apply();
        }
    }

    public static List<String> getAppsForCategory(Context context, int categoryId) {
        List<String> apps = new ArrayList<>();
        if (context == null || isDeleted(context, categoryId)) return apps;

        SharedPreferences prefs = getPrefs(context);
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith("app_cat_")) {
                Object val = entry.getValue();
                if (val instanceof Integer && (Integer) val == categoryId) {
                    String pkg = entry.getKey().substring("app_cat_".length());
                    apps.add(pkg);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : SEED.entrySet()) {
            String pkg = entry.getKey();
            int currentCat = guess(context, pkg);
            if (currentCat == categoryId && !apps.contains(pkg)) {
                apps.add(pkg);
            }
        }
        return apps;
    }

    public static class CategoryItem {
        public final int id;
        public final String name;
        public final boolean isCustom;

        public CategoryItem(int id, String name, boolean isCustom) {
            this.id = id;
            this.name = name;
            this.isCustom = isCustom;
        }
    }
}
