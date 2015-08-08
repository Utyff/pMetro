package com.utyf.pmetro.settings;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.utyf.pmetro.R;
import com.utyf.pmetro.util.Util;

import java.util.ArrayList;

/**
 * Created by Utyf on 15.04.2015.
 *
 */

public class CatalogExpListAdaptor extends BaseExpandableListAdapter {

    static ArrayList<ArrayList<CatalogFile>> catFilesGroups;
    static ArrayList<String> countries;
    public LayoutInflater inflater;

    public CatalogExpListAdaptor(ArrayList<String> _countries, ArrayList<ArrayList<CatalogFile>> catGrp,
                                 LayoutInflater _inflater) {
        countries = _countries;
        catFilesGroups = catGrp;
        inflater = _inflater;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    @SuppressLint("InflateParams")
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View view, ViewGroup parent) {
        CatalogFile catFile = catFilesGroups.get(groupPosition).get(childPosition);
        TextView text;

        if( view==null )
            view = inflater.inflate(R.layout.catalog_item, null);

        text = (TextView) view.findViewById( R.id.city );
        text.setText( catFile.CityName );
        text = (TextView) view.findViewById( R.id.modified );
        text.setText( Util.milli2string( catFile.PmzDate ) );
        text = (TextView) view.findViewById( R.id.file_size );
        text.setText( Integer.toString( catFile.PmzSize/1024 ) );
        text = (TextView) view.findViewById( R.id.comment );
        text.setText( catFile.MapComment );
        text = (TextView) view.findViewById( R.id.map_name );
        text.setText( catFile.MapName );

    /*    convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              clickView = v;
            }
        }); //*/

        return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return catFilesGroups.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return countries.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return countries.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
        if( view==null )
            view = inflater.inflate(R.layout.catalog_group, null);

        TextView v = (TextView)view.findViewById(R.id.country);
        if (v != null)  v.setText(countries.get(groupPosition));

        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
