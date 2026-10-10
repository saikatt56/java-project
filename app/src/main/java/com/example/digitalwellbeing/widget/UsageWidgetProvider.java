package com.example.digitalwellbeing.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.data.AppDatabase;
import com.example.digitalwellbeing.data.entity.UsageRecord;
import com.example.digitalwellbeing.ui.MainActivity;
import com.example.digitalwellbeing.util.TimeUtils;

import java.util.List;

/** Home-screen widget: today's total and the most-used app (proposal 3.5). */
public class UsageWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        // Database access is moved off the broadcast thread; the widget is
        // then updated from the worker once the values are known.
        AppDatabase.IO.execute(() -> {
            int today = TimeUtils.today();
            long total = AppDatabase.get(context).usageDao().totalForegroundMs(today);
            List<UsageRecord> records = AppDatabase.get(context).usageDao().forDay(today);
            String topApp = records.isEmpty() ? "-" : records.get(0).appLabel;

            for (int widgetId : widgetIds) {
                RemoteViews views = new RemoteViews(
                        context.getPackageName(), R.layout.widget_usage);
                views.setTextViewText(R.id.widget_total, TimeUtils.formatDuration(total));
                views.setTextViewText(R.id.widget_top_app, topApp);
                views.setOnClickPendingIntent(R.id.widget_root, openApp(context));
                manager.updateAppWidget(widgetId, views);
            }
        });
    }

    private PendingIntent openApp(Context context) {
        return PendingIntent.getActivity(
                context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
