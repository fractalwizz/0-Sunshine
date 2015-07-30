package com.fract.nano.williamyoung.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity implements MainActivityFragment.Callback{

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private String mLocation;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLocation = Utility.getPreferredLocation(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (findViewById(R.id.weather_detail_container) != null) {
            // Activity in two-pane mode
            mTwoPane = true;
            Log.w("MainActivity", "Choosing dual-pane");
            
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, new DetailActivity.DetailFragment(), DETAILFRAGMENT_TAG)
                    .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        MainActivityFragment forecastFragment = ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast));
        
        forecastFragment.setUseTodayLayout(!mTwoPane);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String loc = Utility.getPreferredLocation(this);
        
        if (loc != null && !loc.equals(mLocation)) {
            MainActivityFragment ff = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            
            if (null != ff) {
                ff.onLocationChanged();
            }
            
            DetailActivity.DetailFragment df = (DetailActivity.DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            
            if (null != df) {
                df.onLocationChanged(loc);
            }
            
            mLocation = loc;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent setIntent = new Intent(this, SettingsActivity.class);
            startActivity(setIntent);
            
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailActivity.DetailFragment.DETAIL_URI, contentUri);

            DetailActivity.DetailFragment fragment = new DetailActivity.DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                .setData(contentUri);
            startActivity(intent);
        }
    }

    private void openPreferredLocation() {
        String location = Utility.getPreferredLocation(this);
    }
}