package com.fract.nano.williamyoung.sunshine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.widget.TextView;
import android.widget.Toast;

import com.fract.nano.williamyoung.sunshine.data.WeatherContract;
import com.fract.nano.williamyoung.sunshine.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

public class MainActivityFragment extends Fragment implements
    LoaderManager.LoaderCallbacks<Cursor>,
    SharedPreferences.OnSharedPreferenceChangeListener,
    MessageApi.MessageListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    public MainActivityFragment() {}

    public final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    private ForecastAdapter adapt;
    private static final int my_loader_id = 0;
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private RecyclerView rv;

    private RecyclerView.LayoutManager layoutManager;
    private TextView empty;
    private boolean mUseTodayLayout, mAutoSelectView;
    private boolean mHoldForTransition;
    private long mInitialSelectedDate = -1;

    private GoogleApiClient mGoogleApiClient;
    private static final String DATA_FETCH_PATH = "/data-fetch";
    public static final String ICON_PATH = "/icon";
    public static final String ICON_KEY = "bmp";
    public static final String TEMP_KEY = "temp";
    public static final String STAMP_KEY = "timestamp";
    private Bitmap mTodayBitmap;
    private String[] mTodayTemps = new String[2];

    private int mChoiceMode;

    private static final String SELECTED_KEY = "listPosition";
    private static final String[] FORECAST_COLUMNS = {
        WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
        WeatherContract.WeatherEntry.COLUMN_DATE,
        WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.LocationEntry.COLUMN_COORD_LAT,
        WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (!mGoogleApiClient.isConnected()) { mGoogleApiClient.connect(); }

        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        
        if (id == R.id.action_map) {
            showMap();
            return true;
        }

        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.MainActivityFragment, 0, 0);
        mChoiceMode = a.getInt(R.styleable.MainActivityFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.MainActivityFragment_autoSelectView, false);
        // Do we do sharedElementTransitions?
        mHoldForTransition = a.getBoolean(R.styleable.MainActivityFragment_sharedElementTransitions, false);
        a.recycle();
    }

    private void showMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != adapt) {
            Cursor c = adapt.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }

        }
    }

    private void updateWeather() { SunshineSyncAdapter.syncImmediately(getActivity()); }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(DATA_FETCH_PATH)) {
//            Toast.makeText(getActivity(), "Message Received", Toast.LENGTH_SHORT).show();
            Log.w("onMessageReceived", "Sending Data");
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
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetLocalNodeResult result = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
                Log.w("onConnected", "LocalNodeID: " + result.getNode().getId());
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) { Wearable.MessageApi.removeListener(mGoogleApiClient, this); }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("MAFragment", "onConnectionFailed: " + connectionResult.getErrorCode());
    }

    public interface Callback {
        //allows activities to be notified of item selection
        // also sends in item viewHolder for mIconView access
        void onItemSelected(Uri dateUri, ForecastAdapter.ViewHolder vh);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        
        if (adapt != null) { adapt.setUseTodayLayout(mUseTodayLayout); }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        empty = (TextView) root.findViewById(R.id.textview_empty);

        rv = (RecyclerView) root.findViewById(R.id.recyclerview_forecast);
        rv.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(layoutManager);

        adapt = new ForecastAdapter(getActivity(), new ForecastAdapter.onClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ViewHolder vh) {
                String locSetting = Utility.getPreferredLocation(getActivity());
                // vh important callback for mIconView access
                ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locSetting, date), vh);
            }
        }, empty, mChoiceMode);
        adapt.setUseTodayLayout(mUseTodayLayout);

        rv.setAdapter(adapt);

        final View parallaxView = root.findViewById(R.id.parallax_bar);

        if (null != parallaxView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxView.getHeight();

                        if (dy > 0) {
                            // scrolling up
                            parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                        } else {
                            // scrolling down
                            parallaxView.setTranslationY(Math.min(0,parallaxView.getTranslationY() - dy / 2));
                        }
                    }
                });
            }
        }

        final AppBarLayout appbarView = (AppBarLayout) root.findViewById(R.id.appbar);
        if (null != appbarView) {
            ViewCompat.setElevation(appbarView, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (0 == rv.computeVerticalScrollOffset()) {
                            appbarView.setElevation(0);
                        } else {
                            appbarView.setElevation(appbarView.getTargetElevation());
                        }
                    }
                });
            }
        }


        if (savedInstanceState != null) { adapt.onRestoreInstanceState(savedInstanceState); }
        
        return root;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (null != rv) { rv.clearOnScrollListeners(); }

        if (mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        adapt.onSaveInstanceState(saveState);
        super.onSaveInstanceState(saveState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We hold for transition here just in-case the activity
        // needs to be re-created. In a standard return transition,
        // this doesn't actually make a difference.
        if (mHoldForTransition) { getActivity().supportPostponeEnterTransition(); }
        getLoaderManager().initLoader(my_loader_id, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(my_loader_id, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
        
        return new CursorLoader(getActivity(), weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in
        adapt.swapCursor(data);

        data.moveToFirst();
        int todayIcon = data.getInt(COL_WEATHER_CONDITION_ID);
        mTodayBitmap = BitmapFactory.decodeResource(getResources(), Utility.getArtResourceForWeatherCondition(todayIcon));
        mTodayTemps[0] = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MAX_TEMP));
        mTodayTemps[1] = Utility.formatTemperature(getActivity(), data.getDouble(COL_WEATHER_MIN_TEMP));

        Log.w("onLoadFinished", mTodayTemps[0] + ":" + mTodayTemps[1]);
        if (mTodayBitmap != null) { Log.w("onLoadFinished", "Bitmap acquired"); }

        updateEmptyView();

        if (data.getCount() == 0) {
            // start enter transition
            getActivity().supportStartPostponedEnterTransition();
        } else {
            rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (rv.getChildCount() > 0) {
                        rv.getViewTreeObserver().removeOnPreDrawListener(this);

                        int pos = adapt.getSelectedItemPosition();
                        if (pos == RecyclerView.NO_POSITION && mInitialSelectedDate != -1) {
                            Cursor data = adapt.getCursor();
                            int count = data.getCount();
                            int dateColumn = data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);

                            for (int i = 0; i < count; i++) {
                                data.moveToPosition(i);
                                if (data.getLong(dateColumn) == mInitialSelectedDate) {
                                    pos = i;
                                    break;
                                }
                            }
                        }

                        if (pos == RecyclerView.NO_POSITION) { pos = 0; }

                        rv.smoothScrollToPosition(pos);
                        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(pos);

                        if (vh != null && mAutoSelectView) { adapt.selectView(vh); }

                        // once we have children views in our ViewHolder
                        if (mHoldForTransition) { getActivity().supportStartPostponedEnterTransition(); }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap cursor with nothing (reset)
        adapt.swapCursor(null);
    }

    private void updateEmptyView() {
        if (adapt.getItemCount() == 0) {
            if (null != empty) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty;
                @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
                switch (location) {
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.empty_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.empty_error;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                        message = R.string.empty_invalid;
                        break;
                    default:
                        if (!Utility.isNetworkAvailable(getActivity())) {
                            message = R.string.empty_network;
                        }
                }
                empty.setText(message);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        if (key.equals(getString(R.string.pref_location_status_key))) { updateEmptyView(); }
    }

    public void setInitialSelectedDate(long initialSelectedDate) { mInitialSelectedDate = initialSelectedDate; }
}