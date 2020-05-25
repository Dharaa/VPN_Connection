package org.strongswan.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.strongswan.android.R;
import org.strongswan.android.model.country.ResultItem;

import java.util.ArrayList;


public class CountryAdapter extends BaseAdapter {

    private final Context mActivity;
    private final ArrayList<ResultItem> mCountryList;

    public CountryAdapter(Context activity, ArrayList<ResultItem> countryList) {
        mActivity = activity;
        mCountryList = countryList;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getCount() {
        return mCountryList.size();
    }

    @Override
    public Object getItem(int position) {
        return mCountryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(mActivity).inflate(R.layout.item_country, parent, false);
        TextView mTvCountryName = (TextView) convertView.findViewById(R.id.mTvCountryName);
        TextView ivThird = (TextView) convertView.findViewById(R.id.iv_third);
        mTvCountryName.setText(mCountryList.get(position).getName());
        if (position % 2 == 0) {
            ivThird.setText("08:00-20:00 normal/high speed");
        } else {
            ivThird.setText("20:00-08:00 PM crowded/low speed");
        }
        return convertView;
    }
}