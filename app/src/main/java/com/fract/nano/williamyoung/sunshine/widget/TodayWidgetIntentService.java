package com.fract.nano.williamyoung.sunshine.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.fract.nano.williamyoung.sunshine.MainActivity;
import com.fract.nano.williamyoung.sunshine.R;
import com.fract.nano.williamyoung.sunshine.Utility;
import com.fract.nano.williamyoung.sunshine.data.WeatherContract;

public class TodayWidgetIntentService extends IntentService {
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;

    public TodayWidgetIntentService() {
        super("TodayWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve Today widget ids: widgets to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, TodayWidgetProvider.class));

        // Get data from ContentProvider
        String loc = Utility.getPreferredLocation(this);
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(loc, System.currentTimeMillis());

        Cursor data = getContentResolver().query(weatherUri, FORECAST_COLUMNS, null, null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if (data == null) { return; }

        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Extract weather data from cursor
        int id = data.getInt(INDEX_WEATHER_ID);
        int artResourceId = Utility.getArtResourceForWeatherCondition(id);

        String description = data.getString(INDEX_SHORT_DESC);

        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        String formattedMax = Utility.formatTemperature(this, maxTemp);
        String formattedMin = Utility.formatTemperature(this, minTemp);

        data.close();

        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int widgetLayout = R.layout.widget_today_small;

            if (minWidth >= 110 && minWidth < 220){
                widgetLayout = R.layout.widget_today;
            } else if (minWidth >= 220){
                widgetLayout = R.layout.widget_today_large;
            }

            RemoteViews views = new RemoteViews(getPackageName(), widgetLayout);

            views.setImageViewResource(R.id.widget_image, artResourceId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, description);
            }

            if (minWidth >= 220){
                views.setTextViewText(R.id.widget_text_desc, description);
            }

            views.setTextViewText(R.id.widget_text_high, formattedMax);

            if (minWidth >= 110){
                views.setTextViewText(R.id.widget_text_low, formattedMin);
            }

            Intent launchintent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchintent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_image, description);
    }
}
