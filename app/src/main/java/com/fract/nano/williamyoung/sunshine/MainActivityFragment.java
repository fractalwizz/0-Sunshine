package com.fract.nano.williamyoung.sunshine;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<String> list = new ArrayList<>();
        list.add("Today - Sunny - 88 / 33");
        list.add("Tomorrow - Sunny - 86 / 45");
        list.add("Tues - Cloudy - 81 / 55");
        list.add("Wed - Rainy - 71 / 33");
        list.add("Thur - Stormy - 66 / 23");
        list.add("Fri - Snowy - 10 / -3");
        list.add("Sat - Sunny - 89 - 70");

        ArrayAdapter<String> adapt = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, list);
        ListView lv = (ListView) root.findViewById(R.id.listview_forecast);
        lv.setAdapter(adapt);

        return root;
    }
}
