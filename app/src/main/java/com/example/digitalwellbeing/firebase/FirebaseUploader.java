package com.example.digitalwellbeing.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.AppRule;
import com.example.digitalwellbeing.data.entity.FocusMode;
import com.example.digitalwellbeing.data.entity.FocusSession;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.util.ParticipantId;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles uploading all local database contents to Firebase Realtime Database.
 */
public class FirebaseUploader {

    private static final String TAG = "FirebaseUploader";

    public interface UploadCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void uploadAllData(Context context, UploadCallback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        AppDatabase.IO.execute(() -> {
            try {
                String uuid = ParticipantId.get(appContext);

                FirebaseDatabase dbInstance = getDatabaseInstance();

                DatabaseReference ref = dbInstance
                        .getReference("participants")
                        .child(uuid);

                AppDatabase db = AppDatabase.get(appContext);

                List<UsageRecord> usageRecords = db.usageDao().all();
                List<AppRule> appRules = db.ruleDao().all();
                List<FocusMode> focusModes = db.focusDao().allModes();
                List<FocusSession> focusSessions = db.focusDao().allSessions();
                List<InterventionEvent> interventionEvents = db.interventionDao().all();

                Map<String, Object> updates = new HashMap<>();

                // Metadata
                updates.put("metadata/participantUuid", uuid);
                updates.put("metadata/lastSyncedAt", System.currentTimeMillis());

                // Usage Records
                for (UsageRecord r : usageRecords) {
                    String key = "usageRecords/" + sanitizeKey(r.packageName) + "_" + r.dayKey;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.id);
                    map.put("dayKey", r.dayKey);
                    map.put("packageName", safeStr(r.packageName));
                    map.put("appLabel", safeStr(r.appLabel));
                    map.put("category", r.category);
                    map.put("foregroundMs", r.foregroundMs);
                    map.put("launchCount", r.launchCount);
                    map.put("updatedAt", r.updatedAt);
                    updates.put(key, map);
                }

                // App Rules
                for (AppRule rule : appRules) {
                    String key = "appRules/" + sanitizeKey(rule.packageName);
                    Map<String, Object> map = new HashMap<>();
                    map.put("packageName", safeStr(rule.packageName));
                    map.put("enabled", rule.enabled);
                    map.put("blocklisted", rule.blocklisted);
                    map.put("dailyLimitMinutes", rule.dailyLimitMinutes);
                    map.put("sessionLimitMinutes", rule.sessionLimitMinutes);
                    map.put("allowedStartMinute", rule.allowedStartMinute);
                    map.put("allowedEndMinute", rule.allowedEndMinute);
                    map.put("interventionType", rule.interventionType);
                    map.put("bypassAllowed", rule.bypassAllowed);
                    updates.put(key, map);
                }

                // Focus Modes
                for (FocusMode mode : focusModes) {
                    String key = "focusModes/" + mode.id;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", mode.id);
                    map.put("name", safeStr(mode.name));
                    map.put("blockedPackages", safeStr(mode.blockedPackages));
                    map.put("startMinute", mode.startMinute);
                    map.put("endMinute", mode.endMinute);
                    map.put("daysMask", mode.daysMask);
                    map.put("enabled", mode.enabled);
                    map.put("strict", mode.strict);
                    updates.put(key, map);
                }

                // Focus Sessions
                for (FocusSession session : focusSessions) {
                    String key = "focusSessions/" + session.id;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", session.id);
                    map.put("focusModeId", session.focusModeId);
                    map.put("startedAt", session.startedAt);
                    map.put("endedAt", session.endedAt);
                    map.put("contextTag", safeStr(session.contextTag));
                    updates.put(key, map);
                }

                // Intervention Events
                for (InterventionEvent event : interventionEvents) {
                    String key = "interventionEvents/" + event.id;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", event.id);
                    map.put("timestamp", event.timestamp);
                    map.put("packageName", safeStr(event.packageName));
                    map.put("interventionType", event.interventionType);
                    map.put("reason", safeStr(event.reason));
                    map.put("outcome", event.outcome);
                    map.put("dwellMs", event.dwellMs);
                    map.put("focusModeName", safeStr(event.focusModeName));
                    updates.put(key, map);
                }

                Log.d(TAG, "Uploading " + updates.size() + " entries to Firebase for participant " + uuid);

                ref.updateChildren(updates, (error, ref1) -> {
                    if (error != null) {
                        Log.e(TAG, "Firebase update failed: " + error.getMessage(), error.toException());
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFailure(error.toException()));
                        }
                    } else {
                        Log.d(TAG, "Firebase upload completed successfully");
                        if (callback != null) {
                            mainHandler.post(callback::onSuccess);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload data", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    private static FirebaseDatabase getDatabaseInstance() {
        try {
            return FirebaseDatabase.getInstance("https://java-4687c-default-rtdb.asia-southeast1.firebasedatabase.app");
        } catch (Exception e) {
            Log.w(TAG, "Regional FirebaseDatabase.getInstance() failed, trying default", e);
            return FirebaseDatabase.getInstance();
        }
    }

    public static String sanitizeKey(String key) {
        if (key == null) return "unknown";
        return key.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_");
    }

    private static String safeStr(String str) {
        return str == null ? "" : str;
    }
}
