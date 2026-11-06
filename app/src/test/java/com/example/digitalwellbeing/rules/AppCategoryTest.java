package com.example.digitalwellbeing.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppCategoryTest {

    private Context context;

    @Before
    public void setUp() {
        context = new TestContext();
    }

    @Test
    public void categoryOtherCannotBeDeleted() {
        AppCategory.deleteCategory(context, AppCategory.OTHER);
        assertFalse(AppCategory.isDeleted(context, AppCategory.OTHER));
    }

    @Test
    public void fallbackCategoryGuessingOperatesCorrectly() {
        assertEquals(AppCategory.SOCIAL, AppCategory.guess("com.facebook.katana"));
        assertEquals(AppCategory.OTHER, AppCategory.guess("com.unknown.app"));

        assertEquals(AppCategory.SOCIAL, AppCategory.guess(context, "com.facebook.katana"));

        // Delete SOCIAL category
        AppCategory.deleteCategory(context, AppCategory.SOCIAL);
        assertTrue(AppCategory.isDeleted(context, AppCategory.SOCIAL));

        // When SOCIAL is deleted, guess should fallback to OTHER
        assertEquals(AppCategory.OTHER, AppCategory.guess(context, "com.facebook.katana"));
    }

    @Test
    public void unassignAppFromOtherClearsExplicitOverride() {
        String pkg = "com.facebook.katana";

        // Unassigning from SOCIAL moves app to OTHER by writing override = OTHER
        AppCategory.unassignApp(context, pkg, AppCategory.SOCIAL);
        assertEquals(AppCategory.OTHER, AppCategory.guess(context, pkg));
        List<String> appsInOther = AppCategory.getAppsForCategory(context, AppCategory.OTHER);
        assertTrue(appsInOther.contains(pkg));

        // Unassigning from OTHER removes the explicit override preference
        AppCategory.unassignApp(context, pkg, AppCategory.OTHER);
        // Guess should fall back to default seed category (SOCIAL)
        assertEquals(AppCategory.SOCIAL, AppCategory.guess(context, pkg));
        List<String> appsInOtherAfter = AppCategory.getAppsForCategory(context, AppCategory.OTHER);
        assertFalse(appsInOtherAfter.contains(pkg));
    }

    private static class TestContext extends ContextWrapper {
        private final SharedPreferences fakePrefs = new FakeSharedPreferences();

        public TestContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return fakePrefs;
        }
    }

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(map);
        }

        @Override
        public String getString(String key, String defValue) {
            return map.containsKey(key) ? (String) map.get(key) : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return map.containsKey(key) ? new HashSet<>((Set<String>) map.get(key)) : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return map.containsKey(key) ? (Integer) map.get(key) : defValue;
        }

        @Override public long getLong(String key, long defValue) { return defValue; }
        @Override public float getFloat(String key, float defValue) { return defValue; }
        @Override public boolean getBoolean(String key, boolean defValue) { return false; }
        @Override public boolean contains(String key) { return map.containsKey(key); }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> changes = new HashMap<>();
                private final Set<String> removes = new HashSet<>();

                @Override
                public Editor putString(String key, String value) {
                    changes.put(key, value);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, Set<String> values) {
                    changes.put(key, values != null ? new HashSet<>(values) : null);
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    changes.put(key, value);
                    return this;
                }

                @Override public Editor putLong(String key, long value) { return this; }
                @Override public Editor putFloat(String key, float value) { return this; }
                @Override public Editor putBoolean(String key, boolean value) { return this; }

                @Override
                public Editor remove(String key) {
                    removes.add(key);
                    return this;
                }

                @Override
                public Editor clear() {
                    map.clear();
                    return this;
                }

                @Override public boolean commit() { apply(); return true; }

                @Override
                public void apply() {
                    for (String key : removes) {
                        map.remove(key);
                    }
                    map.putAll(changes);
                }
            };
        }

        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }
}
