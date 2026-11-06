package com.example.digitalwellbeing.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.dao.UsageDao;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.export.CsvExporter;
import com.example.digitalwellbeing.firebase.FirebaseUploader;
import com.example.digitalwellbeing.util.TimeUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard: today's total, a seven-day trend and the per-app breakdown
 * (proposal 3.4, analytics and visualization).
 */
public class MainActivity extends AppCompatActivity implements UsageAdapter.OnAppSelected {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView totalToday;
    private BarChart weeklyChart;
    private UsageAdapter adapter;

    private final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        int takeFlags = result.getData().getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if (takeFlags == 0) {
                            takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        }
                        try {
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (SecurityException e) {
                            // Ignored
                        }
                        exportCsvToUri(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalToday = findViewById(R.id.total_today);
        weeklyChart = findViewById(R.id.weekly_chart);

        RecyclerView list = findViewById(R.id.app_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsageAdapter(this);
        list.setAdapter(adapter);

        Button export = findViewById(R.id.export_button);
        export.setOnClickListener(v -> exportCsv());

        Button upload = findViewById(R.id.upload_button);
        upload.setOnClickListener(v -> uploadToFirebase());

        Button history = findViewById(R.id.history_button);
        history.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        Button settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        configureChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (com.example.digitalwellbeing.util.PasswordManager.hasPassword(this)
                && !com.example.digitalwellbeing.util.PasswordManager.isUnlocked()) {
            startActivity(new Intent(this, PasswordActivity.class));
            return;
        }
        loadDashboard();
    }

    /** All Room access happens on the shared IO executor, never on the UI thread. */
    private void loadDashboard() {
        AppDatabase.IO.execute(() -> {
            if (com.example.digitalwellbeing.util.PermissionUtils.hasUsageAccess(this)) {
                try {
                    com.example.digitalwellbeing.usage.UsageStatsCollector collector = new com.example.digitalwellbeing.usage.UsageStatsCollector(this);
                    long now = System.currentTimeMillis();
                    List<UsageRecord> todayRecords = collector.collectForDay(now);
                    List<UsageRecord> yesterdayRecords = collector.collectForDay(now - 86_400_000L);

                    AppDatabase db = AppDatabase.get(this);
                    db.usageDao().upsertAll(todayRecords);
                    db.usageDao().upsertAll(yesterdayRecords);
                } catch (Exception e) {
                    // Ignore collection errors
                }
            }

            UsageDao dao = AppDatabase.get(this).usageDao();
            int today = TimeUtils.today();
            long total = dao.totalForegroundMs(today);
            List<UsageRecord> records = dao.forDay(today);
            List<UsageDao.DayTotal> week =
                    dao.dailyTotals(TimeUtils.dayKeyOffset(today, -6), today);

            mainHandler.post(() -> {
                totalToday.setText(TimeUtils.formatDuration(total));
                adapter.submit(records);
                renderWeeklyChart(week);
            });
        });
    }

    private void configureChart() {
        weeklyChart.getDescription().setEnabled(false);
        weeklyChart.setFitBars(true);
        weeklyChart.getAxisRight().setEnabled(false);
        weeklyChart.getAxisLeft().setAxisMinimum(0f);
        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void renderWeeklyChart(List<UsageDao.DayTotal> totals) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < totals.size(); i++) {
            UsageDao.DayTotal day = totals.get(i);
            // Chart in hours: minutes produce an unreadable axis over a week.
            entries.add(new BarEntry(i, day.totalMs / 3_600_000f));
            labels.add(String.valueOf(day.dayKey % 100));
        }
        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.hours_per_day));
        dataSet.setDrawValues(false);
        weeklyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        weeklyChart.setData(new BarData(dataSet));
        weeklyChart.invalidate();
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        exportLauncher.launch(intent);
    }

    private void exportCsvToUri(Uri uri) {
        AppDatabase.IO.execute(() -> {
            try {
                int today = TimeUtils.today();
                String dirName = new CsvExporter(this)
                        .exportToDirectory(uri, TimeUtils.dayKeyOffset(today, -29), today);
                mainHandler.post(() -> Toast.makeText(this,
                        getString(R.string.export_done, dirName != null ? dirName : "selected location"),
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this,
                        R.string.export_failed, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void uploadToFirebase() {
        Toast.makeText(this, R.string.upload_started, Toast.LENGTH_SHORT).show();
        FirebaseUploader.uploadAllData(this, new FirebaseUploader.UploadCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.upload_done, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.upload_failed, e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onAppSelected(UsageRecord record) {
        Intent intent = new Intent(this, AppRuleActivity.class);
        intent.putExtra(AppRuleActivity.EXTRA_PACKAGE, record.packageName);
        intent.putExtra(AppRuleActivity.EXTRA_LABEL, record.appLabel);
        startActivity(intent);
    }
}
