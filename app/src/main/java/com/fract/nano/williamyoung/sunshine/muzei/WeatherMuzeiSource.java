package com.fract.nano.williamyoung.sunshine.muzei;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.fract.nano.williamyoung.sunshine.MainActivity;
import com.fract.nano.williamyoung.sunshine.Utility;
import com.fract.nano.williamyoung.sunshine.data.WeatherContract;
import com.fract.nano.williamyoung.sunshine.sync.SunshineSyncAdapter;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;

public class WeatherMuzeiSource extends MuzeiArtSource {
    public WeatherMuzeiSource() {
        super("WeatherMuzeiSource");
    }

    private static final String[] WEATHER_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        boolean dataUpdated = intent != null && SunshineSyncAdapter.ACTION_DATA_UPDATED.equals(intent.getAction());

        if (dataUpdated && isEnabled()) {
            onUpdate(UPDATE_REASON_OTHER);
        }
    }

    @Override
    protected void onUpdate(int reason) {
        String query = Utility.getPreferredLocation(this);
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(query, System.currentTimeMillis());

        Cursor cursor = getContentResolver().query(weatherUri, WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            String desc = cursor.getString(INDEX_SHORT_DESC);

            String imageUrl = Utility.getImageUrlForWeatherCondition(weatherId);

            if (imageUrl != null) {
                publishArtwork(new Artwork.Builder()
                    .imageUri(Uri.parse(imageUrl))
                    .title(desc)
                    .byline(query)
                    .viewIntent(new Intent(this, MainActivity.class))
                    .build());
            }
        }

        cursor.close();
    }
}
