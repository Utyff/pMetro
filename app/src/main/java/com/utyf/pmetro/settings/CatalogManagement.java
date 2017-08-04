package com.utyf.pmetro.settings;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.utyf.pmetro.R;

/**
 * Created by Utyf on 13.04.2015.
 *
 */

public class CatalogManagement extends Fragment{
    public static CatalogManagement cat;
    private ProgressBar pBar;
    private TextView tvUpdate, tvChanges;
    private ListView lvMap;  // Shows list of downloaded maps
    private MapListAdaptor lvAdapterMap;
    private ExpandableListView elvCat;  // Show list of maps that are available for downloading
    private ImageButton btn;
    Handler pbHandler;
    private LayoutInflater inflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater infl, ViewGroup vgp, Bundle saved) {
        final Activity act = getActivity();
        View        view;
        inflater = infl;
        view = infl.inflate(R.layout.catalog, vgp, false);

        pBar = (ProgressBar) view.findViewById(R.id.progressBar);
        lvMap = (ListView) view.findViewById(R.id.tab1);
        // Handles long clicks
        registerForContextMenu(lvMap);
        // Handles short clicks
        lvMap.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                loadMap(position);
            }
        });

        elvCat = (ExpandableListView) view.findViewById(R.id.elvCatalog);
        registerForContextMenu(elvCat);
        elvCat.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
                view.showContextMenu();
                return true;
            }
        });

        tvChanges = (TextView) view.findViewById(R.id.changeDate);
        tvUpdate = (TextView) view.findViewById(R.id.updateDate);
        btn = (ImageButton )view.findViewById(R.id.updateButton);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { CatalogList.startUpdate(false,act); }
        });

        catalogMapUpdate();
        catalogUpdate();

        TabHost tabHost = (TabHost) view.findViewById(R.id.tabHost);
        tabHost.setup();  // инициализация
        TabHost.TabSpec tabSpec;

        tabSpec = tabHost.newTabSpec("map"); // создаем вкладку и указываем тег
        tabSpec.setIndicator(getString(R.string.local_map));
        tabSpec.setContent(R.id.tab1); // указываем id компонента из FrameLayout, он и станет содержимым
        tabHost.addTab(tabSpec);       // добавляем в корневой элемент

        tabSpec = tabHost.newTabSpec("cat");
        tabSpec.setIndicator(getString(R.string.catalog));
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);

        /* обработчик переключения вкладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                //Toast.makeText(MapActivity.mapActivity, "tabId = " + tabId, Toast.LENGTH_SHORT).show();
                Log.w("CATALOG","selected tab - "+tabId);
            }
        }); // */

        pbHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case 0:  // download started
                        pBar.setVisibility(View.VISIBLE);
                        btn.setImageResource(R.mipmap.ic_action_cancel);
                        break;
                    case 1:  // set download progress
                        pBar.setMax(msg.arg2);
                        pBar.setProgress(msg.arg1);
                        break;
                    case 2:  // catalog update finished
                        // pBar.setVisibility(View.GONE);
                        // btn.setImageResource(R.mipmap.ic_action_refresh);
                        catalogUpdate();
                        break;
                    case 3:  // maps update finished
                        // pBar.setVisibility(View.GONE);
                        // btn.setImageResource(R.mipmap.ic_action_refresh);
                        catalogMapUpdate();
                    case 4:  // download fail
                        pBar.setVisibility(View.GONE);
                        btn.setImageResource(R.mipmap.ic_action_refresh);
                        break;
                }
            }
        };
        return view;
    }
/*
    void catalogMapAdd() {

    }
    void catalogMapDelete() {

    }
*/

    /**
     * Post maps update action
     */
    void catalogMapUpdate() {
        MapList.loadData();
        if( MapList.isLoaded() ) {
            lvAdapterMap = new MapListAdaptor(MapList.mapFiles, inflater);
            lvMap.setAdapter(lvAdapterMap);
        }
        pBar.setVisibility(View.GONE);
        btn.setImageResource(R.mipmap.ic_action_refresh);
    }

    /**
     * Post catalog update action
     */
    private void catalogUpdate() {
        tvChanges.setText( CatalogList.getLastChanges() );
        tvUpdate.setText( CatalogList.getLastUpdate() );
        if( !CatalogList.isLoaded() ) CatalogList.loadData();
        if( CatalogList.isLoaded() ) {
            CatalogExpListAdaptor elvAdapter = new CatalogExpListAdaptor(CatalogList.countries,
                    CatalogList.catFilesGroup, inflater);
            elvCat.setAdapter(elvAdapter);
        }
        pBar.setVisibility(View.GONE);
        btn.setImageResource(R.mipmap.ic_action_refresh);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        if( v.getId()==R.id.elvCatalog ) {
            ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            // If a group is clicked, then childPos is -1
            if (childPos >= 0) {
                inflater.inflate(R.menu.catalog_context_menu, menu);

                CatalogFile cf = CatalogList.getCatFile(groupPos, childPos);
                if (cf != null) menu.setHeaderTitle(cf.CityName + ",  map: " + cf.MapName);
            }
        } else if( v.getId()==R.id.tab1 ) {
            inflater.inflate(R.menu.map_context_menu, menu);

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            MapFile mf = lvAdapterMap.getItem(info.position);
            menu.setHeaderTitle( mf.cityName+",  map: " + mf.mapName );
        }
    }

    // Passes chosen map to MapActivity and finishes current activity
    // position is index in the list view
    private void loadMap(int position) {
        MapFile mf = lvAdapterMap.getItem(position);
        SET.newMapFile = mf.fileShortName;
        SettingsActivity.exit = true;
        getActivity().finish();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if( item.getItemId()==R.id.catalog_load_file ) {
            ExpandableListView.ExpandableListContextMenuInfo info =
                    (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            if ( groupPos>=0 && childPos>=0 )
                CatalogList.downloadMap(CatalogList.catFilesGroup.get(groupPos).get(childPos));
            return true;
        }
        else if( item.getItemId()==R.id.map_delete_file ) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            MapList.deleteFile(info.position);
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        cat = this;
        if( !CatalogList.isLoaded() ) {
            CatalogList.loadFileInfo();
            CatalogList.loadData();
        }
    }
    @Override
    public void onStop() {
        cat = null;
        CatalogList.eraseData();
        super.onStop();
    }
}
