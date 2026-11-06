package com.example.digitalwellbeing.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.dao.UsageDao;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.rules.AppCategory;
import com.example.digitalwellbeing.util.TimeUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private BarChart chart7DaysCategory;
    private BarChart chart30DaysCategory;
    private BarChart chart7Days;
    private BarChart chart30Days;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        chart7DaysCategory = findViewById(R.id.chart_7_days_category);
        chart30DaysCategory = findViewById(R.id.chart_30_days_category);
        chart7Days = findViewById(R.id.chart_7_days);
        chart30Days = findViewById(R.id.chart_30_days);

        loadHistoryData();
    }

    private void loadHistoryData() {
        AppDatabase.IO.execute(() -> {
            UsageDao dao = AppDatabase.get(this).usageDao();
            int today = TimeUtils.today();
            int start7 = TimeUtils.dayKeyOffset(today, -6);
            int start30 = TimeUtils.dayKeyOffset(today, -29);

            List<UsageRecord> records7 = dao.forRange(start7, today);
            List<UsageRecord> records30 = dao.forRange(start30, today);

            List<CategoryTotalData> catTotals7 = computeCategoryTotals(records7);
            List<CategoryTotalData> catTotals30 = computeCategoryTotals(records30);

            List<UsageDao.AppTotal> totals7 = dao.appTotalsForRange(start7, today);
            List<UsageDao.AppTotal> totals30 = dao.appTotalsForRange(start30, today);

            mainHandler.post(() -> {
                renderCategoryChart(chart7DaysCategory, catTotals7, getString(R.string.hours_label));
                renderCategoryChart(chart30DaysCategory, catTotals30, getString(R.string.hours_label));
                renderAppChart(chart7Days, totals7, getString(R.string.hours_label));
                renderAppChart(chart30Days, totals30, getString(R.string.hours_label));
            });
        });
    }

    private List<CategoryTotalData> computeCategoryTotals(List<UsageRecord> records) {
        Map<Integer, Long> map = new HashMap<>();
        if (records != null) {
            for (UsageRecord r : records) {
                int cat = AppCategory.guess(this, r.packageName);
                long current = map.containsKey(cat) ? map.get(cat) : 0L;
                map.put(cat, current + r.foregroundMs);
            }
        }
        List<CategoryTotalData> list = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue() > 0) {
                CategoryTotalData data = new CategoryTotalData();
                data.categoryId = entry.getKey();
                data.categoryName = AppCategory.label(this, entry.getKey());
                data.totalMs = entry.getValue();
                list.add(data);
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(b.totalMs, a.totalMs));
        return list;
    }

    private void renderCategoryChart(BarChart chart, List<CategoryTotalData> catTotals, String label) {
        chart.getDescription().setEnabled(false);
        chart.setFitBars(true);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        chart.setExtraBottomOffset(20f);

        if (catTotals == null || catTotals.isEmpty()) {
            chart.clear();
            chart.setNoDataText(getString(R.string.no_history_data));
            chart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int count = Math.min(catTotals.size(), 15);
        for (int i = 0; i < count; i++) {
            CategoryTotalData item = catTotals.get(i);
            float hours = item.totalMs / 3_600_000f;
            entries.add(new BarEntry(i, hours));
            labels.add(item.categoryName);
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setValueTextSize(9f);

        xAxis.setLabelCount(count);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.setData(new BarData(dataSet));
        chart.invalidate();
    }

    private void renderAppChart(BarChart chart, List<UsageDao.AppTotal> appTotals, String label) {
        chart.getDescription().setEnabled(false);
        chart.setFitBars(true);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-90f);
        chart.setExtraBottomOffset(15f);

        if (appTotals == null || appTotals.isEmpty()) {
            chart.clear();
            chart.setNoDataText(getString(R.string.no_history_data));
            chart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int count = Math.min(appTotals.size(), 15);
        for (int i = 0; i < count; i++) {
            UsageDao.AppTotal item = appTotals.get(i);
            float hours = item.totalMs / 3_600_000f;
            entries.add(new BarEntry(i, hours));
            String name = (item.appLabel != null && !item.appLabel.isEmpty()) ? item.appLabel : item.packageName;
            labels.add(name);
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setValueTextSize(9f);

        xAxis.setLabelCount(count);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.setData(new BarData(dataSet));
        chart.invalidate();
    }

    private static class CategoryTotalData {
        int categoryId;
        String categoryName;
        long totalMs;
    }
}
