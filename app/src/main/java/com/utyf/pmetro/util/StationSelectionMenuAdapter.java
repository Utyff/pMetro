package com.utyf.pmetro.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.utyf.pmetro.R;

/**
 * Created by Utyf on 17.08.2015.
 *
 */

public class StationSelectionMenuAdapter extends BaseAdapter {
    private final Context context;
    private final StationSelectionMenuItem[] stationSelectionMenuItems;

    public StationSelectionMenuAdapter(Context context, StationSelectionMenuItem[] stationSelectionMenuItems) {
        super();
        this.context = context;
        this.stationSelectionMenuItems = stationSelectionMenuItems;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater;
        ViewHolder viewHolder;
        if (convertView == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.station_context_menu_item, parent, false);
            viewHolder.imageView = convertView.findViewById(R.id.imageView_menu);
            viewHolder.textView = convertView.findViewById(R.id.textView_menu);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.setImageDrawable(stationSelectionMenuItems[position].getDrawable());
        viewHolder.textView.setText(stationSelectionMenuItems[position].getText());
        return convertView;

    }

    @Override
    public int getCount() {
        return stationSelectionMenuItems.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
