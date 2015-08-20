package com.utyf.pmetro.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.utyf.pmetro.R;

import java.util.List;

/**
 * Created by Utyf on 17.08.2015.
 *
 */

public class ContextMenuAdapter extends BaseAdapter {
    private final Context context;
    private final List<ContextMenuItem> listContextMenuItems;

    public ContextMenuAdapter(Context context, List<ContextMenuItem> listContextMenuItems) {
        super();
        this.context = context;
        this.listContextMenuItems = listContextMenuItems;
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
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView_menu);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.textView_menu);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.setImageDrawable(listContextMenuItems.get(position).getDrawable());
        viewHolder.textView.setText(listContextMenuItems.get(position).getText());
        return convertView;

    }

    @Override
    public int getCount() {
        return listContextMenuItems.size();
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
