package com.example.digitalwellbeing.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Day keys are plain yyyyMMdd integers in the device's local time zone.
 * Storing a key instead of a raw timestamp keeps "today" stable when the
 * user's clock crosses midnight mid-session (proposal 3.7).
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static int dayKey(long epochMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMillis);
        return c.get(Calendar.YEAR) * 10_000
                + (c.get(Calendar.MONTH) + 1) * 100
                + c.get(Calendar.DAY_OF_MONTH);
    }

    public static int today() {
        return dayKey(System.currentTimeMillis());
    }

    public static long startOfDay(long epochMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static int minuteOfDay(long epochMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMillis);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** 1 = Monday ... 7 = Sunday. Calendar uses 1 = Sunday, hence the shift. */
    public static int isoDayOfWeek(long epochMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMillis);
        int day = c.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }

    /** Day key n days before the given key, via a real Calendar so month ends work. */
    public static int dayKeyOffset(int dayKey, int deltaDays) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(dayKey / 10_000, (dayKey / 100 % 100) - 1, dayKey % 100);
        c.add(Calendar.DAY_OF_MONTH, deltaDays);
        return dayKey(c.getTimeInMillis());
    }

    public static String formatDuration(long millis) {
        long totalMinutes = millis / 60_000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return String.format(Locale.US, "%dm", minutes);
        }
        return String.format(Locale.US, "%dh %02dm", hours, minutes);
    }
}
