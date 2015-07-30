package com.fract.nano.williamyoung.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import com.fract.nano.williamyoung.sunshine.data.WeatherContract;

public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public MainActivityFragment() {
    }

    private ForecastAdapter adapt;
    private static final int my_loader_id = 0;
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
    private int pos;
    private ListView lv;
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String loc = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_default_location_default));

        Uri mapURI = Uri.parse("geo:0,0?").buildUpon()
            .appendQueryParameter("q", loc)
            .build();
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapURI);
        
        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Log.d("NOPE", "Couldn't call " + loc + ", no receiving apps installed!");
        }
    }

    private void updateWeather() {
        String location = Utility.getPreferredLocation(getActivity());
        new FetchWeatherTask(getActivity()).execute(location);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        adapt = new ForecastAdapter(getActivity(), null, 0);
        adapt.setUseTodayLayout(mUseTodayLayout);

        lv = (ListView) root.findViewById(R.id.listview_forecast);
        lv.setAdapter(adapt);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                pos = position;
                
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));
                }
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            pos = savedInstanceState.getInt(SELECTED_KEY);
        }
        
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        if (pos != ListView.INVALID_POSITION) {
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
        
        if (pos != ListView.INVALID_POSITION) {
            lv.smoothScrollToPosition(pos);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap cursor with nothing (reset)
        adapt.swapCursor(null);
    }
}
