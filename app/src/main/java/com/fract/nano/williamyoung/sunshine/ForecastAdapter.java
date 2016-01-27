package com.fract.nano.williamyoung.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fract.nano.williamyoung.sunshine.data.WeatherContract;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean mUseTodayLayout;

    private Cursor mCursor;
    private final Context mContext;

    private final onClickHandler mClickHandler;
    private final View mEmpty;

    private final ItemChoiceManager mICM;

    public ForecastAdapter(Context context, onClickHandler clickHandler, TextView empty, int choiceMode) {
        mContext = context;
        mClickHandler = clickHandler;
        mEmpty = empty;

        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    // particular list item
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mDateView;
        public final TextView mDescView;
        public final TextView mHighView;
        public final TextView mLowView;

        public ViewHolder(View view) {
            super(view);
            mIconView = (ImageView) view.findViewById(R.id.list_item_icon);
            mDateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            mDescView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            mHighView = (TextView) view.findViewById(R.id.list_item_high_textview);
            mLowView = (TextView) view.findViewById(R.id.list_item_low_textview);

            view.setOnClickListener(this);
        }

        // get date about list item and pass to mClickHandler
        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            mCursor.moveToPosition(pos);
            int dateIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            // calls overridden function in Fragment
            mClickHandler.onClick(mCursor.getLong(dateIndex), this);

            mICM.onClick(this);
        }
    }

    // defines a Handler and the onClick function
    public interface onClickHandler { void onClick(Long date, ViewHolder vh); }

    public void setUseTodayLayout(boolean useTodayLayout) { mUseTodayLayout = useTodayLayout; }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public ForecastAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            int layoutID = -1;

            switch (viewType) {
                case VIEW_TYPE_TODAY:
                    layoutID = R.layout.list_item_forecast_today;
                    break;
                case VIEW_TYPE_FUTURE_DAY:
                    layoutID = R.layout.list_item_forecast;
                    break;
            }

            View view = LayoutInflater.from(parent.getContext()).inflate(layoutID, parent, false);

            ForecastAdapter.ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);

            return viewHolder;
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(ForecastAdapter.ViewHolder viewHolder, int position) {
        mCursor.moveToPosition(position);

        int weatherId = mCursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(position);
        boolean useLongToday;

        switch (viewType) {
            case VIEW_TYPE_TODAY:
                viewHolder.mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                useLongToday = true;
                break;
            default:
                //viewHolder.mIconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
                viewHolder.mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
                useLongToday = false;
                break;
        }

        // add unique TransitionName to each mIconView in ForecastAdapter
        ViewCompat.setTransitionName(viewHolder.mIconView, "iconView" + position);

//        Glide.with(mContext)
//            .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
//            .error(fallbackIconId)
//            .crossFade()
//            .into(viewHolder.iconView);

        long date = mCursor.getLong(MainActivityFragment.COL_WEATHER_DATE);
        viewHolder.mDateView.setText(Utility.getFriendlyDayString(mContext, date, useLongToday));

        String forecast = mCursor.getString(MainActivityFragment.COL_WEATHER_DESC);
        viewHolder.mDescView.setText(forecast);
        viewHolder.mDescView.setContentDescription(mContext.getString(R.string.a11y_forecast, forecast));

        viewHolder.mIconView.setContentDescription(forecast);

        boolean isMetric = Utility.isMetric(mContext);

        String high = Utility.formatTemperature(mContext, mCursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP));
        viewHolder.mHighView.setText(high);
        viewHolder.mHighView.setContentDescription(mContext.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(mContext, mCursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP));
        viewHolder.mLowView.setText(low);
        viewHolder.mLowView.setContentDescription(mContext.getString(R.string.a11y_low_temp, low));

        mICM.onBindViewHolder(viewHolder, position);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) { mICM.onSaveInstanceState(outState); }

    public int getSelectedItemPosition() { return mICM.getSelectedItemPosition(); }

    public Cursor getCursor() { return mCursor; }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmpty.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return (null == mCursor) ? 0 : mCursor.getCount();
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolder) {
            ViewHolder vfh = (ViewHolder) viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }
}