package com.fract.nano.williamyoung.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.widget.TextView;

import com.fract.nano.williamyoung.sunshine.data.WeatherContract;
import com.fract.nano.williamyoung.sunshine.sync.SunshineSyncAdapter;

public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    public MainActivityFragment() {}

    public final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    private ForecastAdapter adapt;
    private static final int my_loader_id = 0;
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private int pos = RecyclerView.NO_POSITION;
    private RecyclerView rv;

    private RecyclerView.LayoutManager layoutManager;
    private TextView empty;
    private boolean mUseTodayLayout;

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
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

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
        
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if ( null != adapt ) {
            Cursor c = adapt.getCursor();
            if ( null != c ) {
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

    private void updateWeather() {
        /*String location = Utility.getPreferredLocation(getActivity());
        //new FetchWeatherTask(getActivity()).execute(location);
        Intent intent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        intent.putExtra(SunshineService.SERVICE_KEY, location);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, alarmIntent);

        Intent mServiceIntent = new Intent(getActivity(), SunshineService.class);
        mServiceIntent.putExtra(SunshineService.SERVICE_KEY, location);

        getActivity().startService(mServiceIntent);*/

        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public interface Callback {
        //allows activities to be notified of item selection
        public void onItemSelected(Uri dateUri);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        
        if (adapt != null) {
            adapt.setUseTodayLayout(mUseTodayLayout);
        }
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
                ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locSetting, date));
                pos = vh.getAdapterPosition();
            }
        }, empty);
        adapt.setUseTodayLayout(mUseTodayLayout);

        rv.setAdapter(adapt);
//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
//                pos = position;
//
//                if (cursor != null) {
//                    String locationSetting = Utility.getPreferredLocation(getActivity());
//
//                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));
//                }
//            }
//        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            pos = savedInstanceState.getInt(SELECTED_KEY);
        }
        
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        if (pos != RecyclerView.NO_POSITION) {
            saveState.putInt(SELECTED_KEY, pos);
        }

        super.onSaveInstanceState(saveState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
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

        updateEmptyView();
        
        if (pos != RecyclerView.NO_POSITION) {
            rv.smoothScrollToPosition(pos);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap cursor with nothing (reset)
        adapt.swapCursor(null);
    }

    private void updateEmptyView() {
        if ( adapt.getItemCount() == 0 ) {
            if ( null != empty ) {
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
        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }
}
