package com.fract.nano.williamyoung.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fract.nano.williamyoung.sunshine.data.WeatherContract;
import org.w3c.dom.Text;

public class ForecastAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean mUseTodayLayout;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descView;
        public final TextView highView;
        public final TextView lowView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() { return 2; }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutID = -1;

        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                layoutID = R.layout.list_item_forecast_today;
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                layoutID = R.layout.list_item_forecast;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutID, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }
    
    // Fill-in views with cursor contents
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int weatherId = cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int viewType = getItemViewType(cursor.getPosition());
        //int fallbackIconId;

        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                //fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                break;
            }
            default: {
                //fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
                break;
            }
        }

//        Glide.with(mContext)
//            .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
//            .error(fallbackIconId)
//            .crossFade()
//            .into(viewHolder.iconView);

        long date = cursor.getLong(MainActivityFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, date));

        String forecast = cursor.getString(MainActivityFragment.COL_WEATHER_DESC);
        viewHolder.descView.setText(forecast);
        viewHolder.descView.setContentDescription(context.getString(R.string.a11y_forecast, forecast));

        viewHolder.iconView.setContentDescription(forecast);

        boolean isMetric = Utility.isMetric(context);

        String high = Utility.formatTemperature(context, cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP), isMetric);
        viewHolder.highView.setText(high);
        viewHolder.highView.setContentDescription(context.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(context, cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP), isMetric);
        viewHolder.lowView.setText(low);
        viewHolder.lowView.setContentDescription(context.getString(R.string.a11y_low_temp, low));
    }
}