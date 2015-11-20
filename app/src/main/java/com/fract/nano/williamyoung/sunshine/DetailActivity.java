package com.fract.nano.williamyoung.sunshine;

import android.net.Uri;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fract.nano.williamyoung.sunshine.data.WeatherContract;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putParcelable(DetailFragment.DETAIL_URI, getIntent().getData());

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                .add(R.id.weather_detail_container, fragment)
                .commit();
        }
    }

    public static class DetailFragment extends Fragment implements LoaderCallbacks<Cursor> {

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        private ImageView iconView;
        private TextView dateView;
        private TextView descView;
        private TextView highView;
        private TextView lowView;
        private TextView humidView;
        private TextView humidLabelView;
        private TextView windView;
        private TextView windLabelView;
        private TextView pressView;
        private TextView pressLabelView;

        static final String DETAIL_URI = "URI";
        private Uri mUri;
        private ShareActionProvider mShareActionProvider;
        private String weather;
        private static final int DETAIL_LOADER = 0;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            
            if (arguments != null) {
                mUri = arguments.getParcelable(DETAIL_URI);
            }

            View view = inflater.inflate(R.layout.fragment_detail, container, false);

            iconView = (ImageView) view.findViewById(R.id.detail_icon);
            dateView = (TextView) view.findViewById(R.id.detail_date_textview);
            descView = (TextView) view.findViewById(R.id.detail_desc_textview);
            highView = (TextView) view.findViewById(R.id.detail_high_textview);
            lowView = (TextView) view.findViewById(R.id.detail_low_textview);
            humidView = (TextView) view.findViewById(R.id.detail_humidity_textview);
            humidLabelView = (TextView) view.findViewById(R.id.detail_humidity_label);
            windView = (TextView) view.findViewById(R.id.detail_wind_textview);
            windLabelView = (TextView) view.findViewById(R.id.detail_wind_label);
            pressView = (TextView) view.findViewById(R.id.detail_pressure_textview);
            pressLabelView = (TextView) view.findViewById(R.id.detail_press_label);

            return view;
        }

        private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
        };

        private static final int COL_ID = 0;
        private static final int COL_WEATHER_DATE = 1;
        private static final int COL_WEATHER_DESC = 2;
        private static final int COL_WEATHER_MAX_TEMP = 3;
        private static final int COL_WEATHER_MIN_TEMP = 4;
        private static final int COL_WEATHER_HUMID = 5;
        private static final int COL_WEATHER_WIND = 6;
        private static final int COL_WEATHER_PRESS = 7;
        private static final int COL_WEATHER_DEGREE = 8;
        private static final int COL_WEATHER_ID = 9;

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
            if (getActivity() instanceof DetailActivity) {
                menuInflater.inflate(R.menu.detail_fragment, menu);

                MenuItem item = menu.findItem(R.id.menu_item_share);
                item.setIntent(createShareForecastIntent());
            }
        }

        private Intent createShareForecastIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, weather + "#SunshineApp");
            
            return shareIntent;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (null != mUri) {
                return new CursorLoader(getActivity(), mUri, FORECAST_COLUMNS,
                    null, null, null);
            }
            
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.moveToFirst()) {

                boolean isMetric = Utility.isMetric(getActivity());
                int weatherID = data.getInt(DetailFragment.COL_WEATHER_ID);

                iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherID));

//                Glide.with(this)
//                    .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherID))
//                    .error(Utility.getArtResourceForWeatherCondition(weatherID))
//                    .crossFade()
//                    .into(iconView);

                long date = data.getLong(DetailFragment.COL_WEATHER_DATE);
                dateView.setText(Utility.getFriendlyDayString(getActivity(), date));

                String forecast = data.getString(DetailFragment.COL_WEATHER_DESC);
                descView.setText(forecast);
                descView.setContentDescription(getString(R.string.a11y_forecast, forecast));

                String high = Utility.formatTemperature(getActivity(), data.getDouble(DetailFragment.COL_WEATHER_MAX_TEMP), isMetric);
                highView.setText(high);
                highView.setContentDescription(getString(R.string.a11y_high_temp, high));

                String low = Utility.formatTemperature(getActivity(), data.getDouble(DetailFragment.COL_WEATHER_MIN_TEMP), isMetric);
                lowView.setText(low);
                lowView.setContentDescription(getString(R.string.a11y_low_temp, low));

                float humid = data.getFloat(DetailFragment.COL_WEATHER_HUMID);
                //humidView.setText(Utility.getFormattedHumid(getActivity(), humid));
                humidView.setText(getActivity().getString(R.string.f_humidity, humid));
                humidView.setContentDescription(humidView.getText());
                humidLabelView.setContentDescription(humidView.getContentDescription());

                float wind = data.getFloat(DetailFragment.COL_WEATHER_WIND);
                float degree = data.getFloat(DetailFragment.COL_WEATHER_DEGREE);
                windView.setText(Utility.getFormattedWind(getActivity(), wind, degree));
                windView.setContentDescription(windView.getText());
                windLabelView.setContentDescription(windView.getContentDescription());

                float press = data.getFloat(DetailFragment.COL_WEATHER_PRESS);
                //pressView.setText(Utility.getFormattedPressure(getActivity(), press));
                pressView.setText(getActivity().getString(R.string.f_pressure, press));
                pressView.setContentDescription(pressView.getText());
                pressLabelView.setContentDescription(pressView.getContentDescription());

                //#Shareintent
                weather = String.format("%s - %s - %s/%s",
                        Utility.getFriendlyDayString(getActivity(), date),
                        forecast,
                        high,
                        low);

                iconView.setContentDescription(getString(R.string.a11y_forecast_icon, weather));

                if (mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(createShareForecastIntent());
                }
            }

            AppCompatActivity activity = (AppCompatActivity) getActivity();
            Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);

            if (activity instanceof DetailActivity) {
                activity.supportStartPostponedEnterTransition();

                if (null != toolbar) {
                    activity.setSupportActionBar(toolbar);
                    activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                    activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                if (null != toolbar) {
                    Menu menu = toolbar.getMenu();
                    if (null != menu) menu.clear();
                    toolbar.inflateMenu(R.menu.detail_fragment);

                    MenuItem menuItem = menu.findItem(R.id.menu_item_share);
                    Log.w("DetailActivityMenu", "menuitem?");
                    menuItem.setIntent(createShareForecastIntent());
                }
            }
        }

        void onLocationChanged( String newLocation ) {
            // replace the uri, since the location has changed
            Uri uri = mUri;
            
            if (null != uri) {
                long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
                Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
                mUri = updatedUri;
                
                getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    }
}
