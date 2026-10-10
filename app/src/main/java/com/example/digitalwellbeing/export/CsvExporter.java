package com.example.digitalwellbeing.export;

import android.content.Context;

import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.InterventionEvent;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.rules.AppCategory;
import com.example.digitalwellbeing.rules.InterventionType;
import com.example.digitalwellbeing.util.ParticipantId;
import com.example.digitalwellbeing.util.TimeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Manual CSV export for research use (proposal 3.2, export layer).
 *
 * Exports carry the generated participant UUID only. No account name, email,
 * phone number or device identifier is written, and app labels are left out
 * of the intervention file because a custom-named app can identify a person.
 */
public class CsvExporter {

    private final Context context;

    public CsvExporter(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Writes both files into the app's external files dir and returns them.
     */
    public File[] exportAll(int fromDayKey, int toDayKey) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create export directory");
        }
        String uuid = ParticipantId.get(context);
        String stamp = String.valueOf(TimeUtils.today());

        File usageFile = new File(dir, "usage_" + stamp + ".csv");
        File eventFile = new File(dir, "interventions_" + stamp + ".csv");

        writeUsage(usageFile, uuid, fromDayKey, toDayKey);
        writeInterventions(eventFile, uuid, fromDayKey, toDayKey);

        return new File[]{usageFile, eventFile};
    }

    private void writeUsage(File file, String uuid, int from, int to) throws IOException {
        List<UsageRecord> records = AppDatabase.get(context).usageDao().forRange(from, to);
        try (Writer writer = newWriter(file)) {
            writer.write("participant_uuid,day,package_name,category,foreground_minutes,launch_count\n");
            for (UsageRecord r : records) {
                writer.write(String.format(Locale.US, "%s,%d,%s,%s,%d,%d%n",
                        uuid,
                        r.dayKey,
                        escape(r.packageName),
                        escape(AppCategory.label(r.category)),
                        r.foregroundMinutes(),
                        r.launchCount));
            }
        }
    }

    private void writeInterventions(File file, String uuid, int from, int to) throws IOException {
        long fromMillis = dayKeyToMillis(from);
        long toMillis = dayKeyToMillis(to) + 86_400_000L;
        List<InterventionEvent> events =
                AppDatabase.get(context).interventionDao().inRange(fromMillis, toMillis);

        try (Writer writer = newWriter(file)) {
            writer.write("participant_uuid,timestamp_ms,day,package_name,"
                    + "intervention_type,reason,outcome,dwell_ms,focus_mode\n");
            for (InterventionEvent e : events) {
                writer.write(String.format(Locale.US, "%s,%d,%d,%s,%s,%s,%s,%d,%s%n",
                        uuid,
                        e.timestamp,
                        TimeUtils.dayKey(e.timestamp),
                        escape(e.packageName),
                        escape(InterventionType.label(e.interventionType)),
                        escape(e.reason),
                        outcomeLabel(e.outcome),
                        e.dwellMs,
                        escape(e.focusModeName)));
            }
        }
    }

    private Writer newWriter(File file) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    private static long dayKeyToMillis(int dayKey) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(dayKey / 10_000, (dayKey / 100 % 100) - 1, dayKey % 100);
        return c.getTimeInMillis();
    }

    static String outcomeLabel(int outcome) {
        switch (outcome) {
            case InterventionEvent.OUTCOME_CANCELLED:
                return "CANCELLED";
            case InterventionEvent.OUTCOME_CONTINUED:
                return "CONTINUED";
            case InterventionEvent.OUTCOME_BYPASSED:
                return "BYPASSED";
            default:
                return "SHOWN";
        }
    }

    /**
     * Minimal RFC 4180 quoting so a comma in a label cannot shift columns.
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
