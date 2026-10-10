package com.example.digitalwellbeing.usage;

/**
 * Tracks the current continuous session for session-limit rules. Kept in
 * memory in the watcher service: a session is by definition interrupted when
 * the process dies, and persisting it would produce false positives after a
 * reboot.
 */
public class SessionTracker {

    private String currentPackage;
    private long sessionStartedAt;

    /** Call whenever the foreground package changes. */
    public synchronized void onForegroundChanged(String packageName, long nowMillis) {
        if (packageName == null || !packageName.equals(currentPackage)) {
            currentPackage = packageName;
            sessionStartedAt = nowMillis;
        }
    }

    public synchronized int minutesInSession(String packageName, long nowMillis) {
        if (currentPackage == null || !currentPackage.equals(packageName)) {
            return 0;
        }
        return (int) ((nowMillis - sessionStartedAt) / 60_000L);
    }

    public synchronized String currentPackage() {
        return currentPackage;
    }

    public synchronized void reset() {
        currentPackage = null;
        sessionStartedAt = 0L;
    }
}
