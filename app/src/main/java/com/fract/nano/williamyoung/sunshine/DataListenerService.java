package com.fract.nano.williamyoung.sunshine;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fract.nano.williamyoung.sunshine.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DataListenerService extends WearableListenerService {
    private static final String TAG = "DataListenerService";

    private static final String DATA_FETCH_PATH = "/data-fetch";
    public static final String ICON_PATH = "/icon";
    public static final String ICON_KEY = "bmp";
    public static final String TEMP_KEY = "temp";
    public static final String DATE_KEY = "date";
    public static final String STAMP_KEY = "timestamp";
    GoogleApiClient mGoogleApiClient;

    Bitmap mTodayBitmap;
    String[] mTodayTemps = new String[2];
    String mTodayDate;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    // these indices must match the projection
    private static final int COL_WEATHER_CONDITION_ID = 0;
    private static final int COL_WEATHER_MAX_TEMP = 1;
    private static final int COL_WEATHER_MIN_TEMP = 2;

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.w(TAG, "onMessageReceived: " + messageEvent);

        if (messageEvent.getPath().equals(DATA_FETCH_PATH)) {
            String locationSetting = Utility.getPreferredLocation(getApplicationContext());
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
            Cursor data = getApplicationContext().getContentResolver().query(weatherForLocationUri, NOTIFY_WEATHER_PROJECTION, null, null, sortOrder);

            if (data == null || !data.moveToFirst()) { return; }

            int todayIcon = data.getInt(COL_WEATHER_CONDITION_ID);
            mTodayBitmap = BitmapFactory.decodeResource(getResources(), Utility.getArtResourceForWeatherCondition(todayIcon));
            mTodayTemps[0] = Utility.formatTemperature(getApplicationContext(), data.getDouble(COL_WEATHER_MAX_TEMP));
            mTodayTemps[1] = Utility.formatTemperature(getApplicationContext(), data.getDouble(COL_WEATHER_MIN_TEMP));

            long currentTime = System.currentTimeMillis();
            SimpleDateFormat sDF = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.US);
            mTodayDate = sDF.format(currentTime);

            Log.w("onMessageReceived", mTodayTemps[0] + ":" + mTodayTemps[1]);
            if (mTodayBitmap != null) { Log.w("onMessageReceived", "Bitmap acquired"); }
            Log.w("onMessageReceived", mTodayDate);

            if (mTodayBitmap != null && mGoogleApiClient.isConnected()) {
                sendData(asAsset(mTodayBitmap));
            }
        }
    }

    private static Asset asAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;

        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void sendData(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(ICON_PATH);
        dataMap.getDataMap().putAsset(ICON_KEY, asset);
        dataMap.getDataMap().putStringArray(TEMP_KEY, mTodayTemps);
        dataMap.getDataMap().putString(DATE_KEY, mTodayDate);
        dataMap.getDataMap().putLong(STAMP_KEY, System.currentTimeMillis());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
            .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    if (!dataItemResult.getStatus().isSuccess()) {
                        Log.w("sendData", "Data Send Failed");
                    } else {
                        Log.w("sendData", "Success!");
                    }
                }
            });

        Wearable.DataApi.deleteDataItems(mGoogleApiClient, request.getUri());
    }
}