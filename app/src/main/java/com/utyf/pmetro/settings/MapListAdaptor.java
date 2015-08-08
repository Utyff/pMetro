package com.utyf.pmetro.settings;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.utyf.pmetro.R;

import java.util.ArrayList;

/**
 * Created by Utyf on 16.04.2015.
 *
 */

public class MapListAdaptor extends BaseAdapter {

    ArrayList<MapFile>  mapFiles;
    LayoutInflater  inflater;

    public MapListAdaptor(ArrayList<MapFile>  mpFiles, LayoutInflater _inflater) {
        mapFiles = mpFiles;
        inflater = _inflater;
    }

    @Override
    public int getCount() {
        return mapFiles.size();
    }

    @Override
    public MapFile getItem(int i) {
        return mapFiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    @SuppressLint("InflateParams")
    public View getView(int position, View view, ViewGroup viewGroup) {
        MapFile mapFile = mapFiles.get(position);
        TextView text;

        if( view==null )
            view = inflater.inflate(R.layout.map_item, null);

        text = (TextView) view.findViewById( R.id.city );
        text.setText( mapFile.cityName );
        text = (TextView) view.findViewById( R.id.modified );
        text.setText( mapFile.date );
        text = (TextView) view.findViewById( R.id.file_size );
        text.setText( Long.toString( mapFile.size/1024 ) );
        text = (TextView) view.findViewById( R.id.comment );
        text.setText( mapFile.comment );
        text = (TextView) view.findViewById( R.id.map_name );
        text.setText( mapFile.mapName );
        text = (TextView) view.findViewById( R.id.country );
        text.setText( mapFile.country );

        return view;
    }
}
