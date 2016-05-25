/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fract.nano.williamyoung.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() { return new Engine(); }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mDatePaint;
        Paint mDividerPaint;
        Paint mHighPaint;
        Paint mLowPaint;
        boolean mAmbient;

        Bitmap mBitmap;
        Bitmap mGrayBitmap;
        String mHighTemp = "";
        String mLowTemp = "";
        String mDate = "";

        Time mTime;

        private GoogleApiClient mGoogleApiClient;
        private String mDeviceNodeId;
        private static final String DATA_FETCH_PATH = "/data-fetch";
        public static final String ICON_PATH = "/icon";
        public static final String ICON_KEY = "bmp";
        public static final String TEMP_KEY = "temp";
        public static final String DATE_KEY = "date";

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        float mXOffset;
        float mYOffset;
        float mDateYOffset;
        float mDateXOffset;

        float mDividerSX;
        float mDividerSY;
        float mDividerEX;
        float mDividerEY;

        float mBitXOffset;
        float mBitYOffset;

        float mHighXOffset;
        float mHighYOffset;
        float mLowXOffset;
        float mLowYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mBurnInProtection;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Log.w("WFService", "onCreate called");

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                .setShowSystemUiTime(false)
                .build());

            Resources resources = SunshineWatchFaceService.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.date_text));

            mDividerPaint = new Paint();
            mDividerPaint.setColor(Color.WHITE);

            mHighPaint = new Paint(mTextPaint);
            mLowPaint = new Paint(mDatePaint);

            mTime = new Time();

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            //if (!mBurnInProtection && !mLowBitAmbient) { initGrayIconBitmap(); }
            initGrayIconBitmap();
        }

        private void initGrayIconBitmap() {
            if (mBitmap == null) { return; }

            mGrayBitmap = Bitmap.createBitmap(
                mBitmap.getWidth(),
                mBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(mGrayBitmap);
            Paint paint = new Paint();
            ColorMatrix cM = new ColorMatrix();
            cM.setSaturation(0);

            ColorMatrixColorFilter cMCF = new ColorMatrixColorFilter(cM);
            paint.setColorFilter(cMCF);

            canvas.drawBitmap(mBitmap, 0, 0, paint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) { return; }

            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) { return; }

            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();

            mXOffset = resources.getDimension(isRound
                ? R.dimen.digital_x_offset_round
                : R.dimen.digital_x_offset);
            mYOffset = resources.getDimension(isRound
                ? R.dimen.digital_y_offset_round
                : R.dimen.digital_y_offset);
            float textSize = resources.getDimension(R.dimen.digital_text_size);
            mTextPaint.setTextSize(textSize);

            mDateXOffset = resources.getDimension(isRound
                ? R.dimen.date_x_offset_round
                : R.dimen.date_x_offset);
            mDateYOffset = resources.getDimension(isRound
                ? R.dimen.date_y_offset_round
                : R.dimen.date_y_offset);
            float dateSize = resources.getDimension(R.dimen.date_text_size);
            mDatePaint.setTextSize(dateSize);

            mDividerSX = resources.getDimension(isRound
                ? R.dimen.divider_start_x_round
                : R.dimen.divider_start_x);
            mDividerSY = resources.getDimension(isRound
                ? R.dimen.divider_start_y_round
                : R.dimen.divider_start_y);
            mDividerEX = resources.getDimension(isRound
                ? R.dimen.divider_end_x_round
                : R.dimen.divider_end_x);
            mDividerEY = resources.getDimension(isRound
                ? R.dimen.divider_end_y_round
                : R.dimen.divider_end_y);

            mBitXOffset = resources.getDimension(isRound
                ? R.dimen.bitmap_x_offset_round
                : R.dimen.bitmap_x_offset);
            mBitYOffset = resources.getDimension(isRound
                ? R.dimen.bitmap_y_offset_round
                : R.dimen.bitmap_y_offset);

            float tempSize = resources.getDimension(isRound
                ? R.dimen.high_text_size_round
                : R.dimen.high_text_size);
            mHighXOffset = resources.getDimension(isRound
                ? R.dimen.high_temp_x_offset_round
                : R.dimen.high_temp_x_offset);
            mHighYOffset = resources.getDimension(isRound
                    ? R.dimen.high_temp_y_offset_round
                    : R.dimen.high_temp_y_offset);
            mLowXOffset = resources.getDimension(isRound
                    ? R.dimen.low_temp_x_offset_round
                    : R.dimen.low_temp_x_offset);
            mLowYOffset = resources.getDimension(isRound
                ? R.dimen.low_temp_y_offset_round
                : R.dimen.low_temp_y_offset);
            mHighPaint.setTextSize(tempSize);
            mLowPaint.setTextSize(tempSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;

                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mBackgroundPaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mDividerPaint.setAntiAlias(!inAmbientMode);
                    mHighPaint.setAntiAlias(!inAmbientMode);
                    mLowPaint.setAntiAlias(!inAmbientMode);
                }

                if (mGrayBitmap == null) { initGrayIconBitmap(); }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode() || mBurnInProtection || mLowBitAmbient) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = String.format("%02d:%02d", mTime.hour, mTime.minute);

            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            canvas.drawText(mDate, mDateXOffset, mDateYOffset, mDatePaint);

            canvas.drawLine(mDividerSX, mDividerSY, mDividerEX, mDividerEY, mDividerPaint);

            if (mBitmap != null){
                if (isInAmbientMode()) {
                    canvas.drawBitmap(mGrayBitmap, mBitXOffset, mBitYOffset, null);
                } else {
                    canvas.drawBitmap(mBitmap, mBitXOffset, mBitYOffset, null);
                }
            }

            canvas.drawText(mHighTemp, mHighXOffset, mHighYOffset, mHighPaint);
            canvas.drawText(mLowTemp, mLowXOffset, mLowYOffset, mLowPaint);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() { return isVisible() && !isInAmbientMode(); }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);

                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    mDeviceNodeId = pickBestNode(nodes.getNodes());
                    Log.w("onConnectedThread", "TargetNodeID: " + mDeviceNodeId);

                    if (mDeviceNodeId != null) {
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, mDeviceNodeId, DATA_FETCH_PATH, new byte[0])
                            .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.e("onConnectedThread", "Message Failed: " + sendMessageResult.getStatus().getStatusMessage());
                                    } else {
                                        Log.w("onConnectedThread", "Message Success");
                                    }
                                }
                            });
                    }
                }

                public String pickBestNode(List<Node> nodes) {
                    String best = null;
                    for (Node node : nodes) {
                        Log.w("pickBestNode", node.getId() + ":" + node.getDisplayName());
                        if (node.isNearby()) { return node.getId(); }
                        best = node.getId();
                    }

                    return best;
                }
            }).start();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) { continue; }

                String path = dataEvent.getDataItem().getUri().getPath();

                if (ICON_PATH.equals(path)) {
                    DataMapItem dMI = DataMapItem.fromDataItem(dataEvent.getDataItem());
                    Asset iconAsset = dMI.getDataMap().getAsset(ICON_KEY);

                    String[] temp = dMI.getDataMap().getStringArray(TEMP_KEY);

                    if (iconAsset != null) { Log.w("onDataChanged", "Asset acquired"); }
                    Log.w("onDataChanged", temp[0] + ":" + temp[1]);

                    if (temp.length == 2) {
                        mHighTemp = temp[0];
                        mLowTemp = temp[1];
                    } else {
                        Log.e("onDataChanged", "Missing Temperature");
                    }

                    String date = dMI.getDataMap().getString(DATE_KEY);
                    if (date != null) { mDate = date; }

                    new LoadBitmapAsyncTask().execute(iconAsset);
                }
            }
        }

        private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {
            @Override
            protected Bitmap doInBackground(Asset... params) {
                if (params.length > 0) {
                    Asset asset = params[0];
                    InputStream assetIS = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset)
                        .await()
                        .getInputStream();

                    if (assetIS == null) {
                        Log.e("LoadBitmapTask", "Requested an unknown Asset.");
                        return null;
                    }

                    return Bitmap.createScaledBitmap(BitmapFactory.decodeStream(assetIS), 80, 80, false);
                } else {
                    Log.e("LoadBitmapTask", "Asset must be non-null");
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    Log.w("LoadBitmapTask", "Setting Icon");

                    mBitmap = bitmap;
                    invalidate();
                }
            }
        }
    }
}