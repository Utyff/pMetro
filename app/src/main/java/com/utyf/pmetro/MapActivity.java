package com.utyf.pmetro;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utyf.pmetro.map.Delay;
import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.map.routing.RouteInfo;
import com.utyf.pmetro.settings.AlarmReceiver;
import com.utyf.pmetro.settings.CatalogList;
import com.utyf.pmetro.settings.SET;
import com.utyf.pmetro.settings.SettingsActivity;
import com.utyf.pmetro.util.LanguageUpdater;
import com.utyf.pmetro.util.RouteListItem;
import com.utyf.pmetro.util.StationSelectionMenuItem;
import com.utyf.pmetro.util.StationsNum;

import java.io.File;


public class MapActivity extends AppCompatActivity implements StationContextMenuFragment.Listener,
        RouteSelectionDialogFragment.Listener, StationSelectionDialogFragment.Listener {

    public static MapActivity  mapActivity;
    public static File         fileDir;
    public static File         catalogDir;
    public static File         cacheDir;
    public static String       catalogFile;
    public final static String shortCatalogFile = "Files.xml";
    public static boolean  debugMode;
    public static int      numberOfCores = Runtime.getRuntime().availableProcessors();
    public static long     maxMemory     = Runtime.getRuntime().maxMemory()/1024/1024;
    public static String   versionNum;
    public static int      buildNum;
    public static String   errorMessage="";
    // TODO: 08.07.2016 Move mapView into a fragment, which stores view with a single scheme
    public  Map_View mapView;
    private Menu     menu;
    private MapData mapData;

    private final static int DelayFirst = Menu.FIRST;
    private final static int DelaySize = 9;
    private final static int TransportFirst = DelayFirst+DelaySize;
    private final static int TransportSize = 99;
    private final static String MAP_DATA_FRAGMENT_TAG = "mapDataFragment";
    private MapDataFragment mapDataFragment;

    private LanguageUpdater languageUpdater;

    private MenuCreatedListener menuCreatedListener;

    interface MenuCreatedListener {
        void onMenuCreated();
    }
//    private AutoCompleteTextView actvFrom, actvTo;

//    String[] languages={"Android ","java","IOS","SQL","JDBC","Web services"};

    public static void getDirs(Context cntx) {
        fileDir = cntx.getExternalFilesDir(null);
        boolean bl = Environment.getExternalStorageState().toLowerCase().equals("mounted");
        if( fileDir==null || !bl )   fileDir = cntx.getFilesDir();
        catalogDir = new File(fileDir+"/catalog");
        cacheDir = cntx.getCacheDir();
        catalogFile = catalogDir + "/"+shortCatalogFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_placeholder);

        mapActivity = this;

        getBuild();
        SET.load(this);
        getDirs(this);
        languageUpdater = new LanguageUpdater(this, SET.lang);

        // isOnline(true);
        if( SET.cat_upd.equals("On start program") )
            CatalogList.updateAll(true, this);

        // try to restore recent map data
        FragmentManager manager = getFragmentManager();
        mapDataFragment = (MapDataFragment) manager.findFragmentByTag(MAP_DATA_FRAGMENT_TAG);
        if (mapDataFragment == null) {
            FragmentTransaction transaction = manager.beginTransaction();
            mapDataFragment = new MapDataFragment();
            transaction.add(mapDataFragment, MAP_DATA_FRAGMENT_TAG);
            transaction.commit();
        }
        getMapDataFromFragment();
        //registerForContextMenu(mapView);

        if( maxMemory<127 )
            Toast.makeText(this, "Low memory\n"+Long.toString(maxMemory)+"Mb RAM available.", Toast.LENGTH_LONG).show();

/*        ActionBar mActionBar = getSupportActionBar();
        if( mActionBar!=null ) {
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setDisplayShowCustomEnabled(true);
            //View viewBar = getLayoutInflater().inflate(R.layout.action_bar, null);
            //mActionBar.setCustomView(viewBar);
            mActionBar.setCustomView(R.layout.action_bar);
            View viewBar = mActionBar.getCustomView();

//            actvFrom = (AutoCompleteTextView) viewBar.findViewById(R.id.editTextFrom);
//            actvTo   = (AutoCompleteTextView) viewBar.findViewById(R.id.editTextTo);
//            ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,languages);

//            actvFrom.setAdapter(adapter);
//            actvFrom.setThreshold(1);

            ImageButton imageButton = (ImageButton) viewBar.findViewById(R.id.imageButton);
        } else
            Log.e("MapActivity /106", "Can't get action bar"); // */
    }

    private void showLoadingView() {
        findViewById(R.id.loading).bringToFront();
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        FrameLayout parent = (FrameLayout)findViewById(R.id.parent);
        parent.removeView(mapView);
    }

    private void showMapView() {
        mapView = new Map_View(MapActivity.this, mapData);
        mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        FrameLayout parent = (FrameLayout)findViewById(R.id.parent);
        findViewById(R.id.loading).setVisibility(View.INVISIBLE);
        parent.addView(mapView);

        registerForContextMenu(mapView);

        fillMenuItems();
    }

    private void showLoadingFailureView() {
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);

        TextView text = (TextView)findViewById(R.id.text);
        text.setText(R.string.map_not_loaded);
    }

    private void getMapDataFromFragment() {
        showLoadingView();
        mapDataFragment.getMapDataAsync(new MapDataFragment.MapDataCallback() {
            @Override
            public void onMapDataLoaded(MapData mapData) {
                final MapData mapDataCopy = mapData;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MapActivity.this.mapData = mapDataCopy;
                        showMapView();
                    }
                });
            }

            @Override
            public void onMapDataFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoadingFailureView();
                        loadFail();
                    }
                });
            }
        });
    }

    @Override
    public void onInfoSelected(StationsNum stn) {
        mapData.map.showStationInfo(stn);
    }

    @Override
    public void onStartSelected(StationsNum stn) {
        mapData.routingState.setStart(stn);
    }

    @Override
    public void onFinishSelected(StationsNum stn) {
        mapData.routingState.setEnd(stn);
    }

    @Override
    public void onViaSelected(StationsNum stn) {
//        mapData.routingState.addVia(stn);
    }

    @Override
    public void onAvoidSelected(StationsNum stn) {
//        mapData.routingState.addBlocked(stn);
    }

    public void stationContextMenu(final StationsNum stn) {
        DialogFragment fragment = new StationContextMenuFragment();
        Bundle arguments = new Bundle();
        arguments.putString("station_name", mapData.transports.getStationName(stn));
        arguments.putParcelable("station_num", stn);
        fragment.setArguments(arguments);
        fragment.show(getFragmentManager(), "stationContextMenu");
    }

    @Override
    public void onRouteSelected(int position) {
        if (position == 0)
            mapData.routingState.showBestRoute();
        else
            mapData.routingState.showAlternativeRoute(position - 1);
    }

    public void showRouteSelectionMenu(RouteInfo[] bestRoutes) {
        RouteListItem[] items = new RouteListItem[bestRoutes.length];
        for (int i = 0; i < bestRoutes.length; ++i) {
            items[i] = RouteListItem.createRouteListItem(bestRoutes[i], mapData);
        }

        DialogFragment fragment = new RouteSelectionDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArray("items", items);
        fragment.setArguments(arguments);
        fragment.show(getFragmentManager(), "routeSelectionDialog");
    }

    @Override
    public void onStationSelected(StationsNum stn, boolean isLongTap) {
        mapView.selectStation(stn, isLongTap);
    }

    public void showStationSelectionMenu(final StationsNum[] stations, final boolean isLongTap) {
        StationSelectionMenuItem stationSelectionMenuItems[] = new StationSelectionMenuItem[stations.length];
        for (int i = 0; i < stations.length; ++i) {
            StationsNum stn = stations[i];
            int color = mapData.map.getLine(stn.trp, stn.line).getColor();
            String name = mapData.transports.getStationName(stn);
            stationSelectionMenuItems[i] = new StationSelectionMenuItem(color, name);
        }

        DialogFragment fragment = new StationSelectionDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArray("items", stationSelectionMenuItems);
        arguments.putParcelableArray("stations", stations);
        arguments.putBoolean("isLongTap", isLongTap);
        fragment.setArguments(arguments);
        fragment.show(getFragmentManager(), "routeSelectionDialog");
    }

    private void getBuild() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo( getPackageName(), 0);
            versionNum = info.versionName;
            buildNum = info.versionCode;

            //ApplicationInfo ai = manager.getApplicationInfo(getPackageName(), 0);
            //ZipFile zf = new ZipFile(ai.sourceDir);
            //ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            //long time = ze.getTime();
            // buildNum = "build: "+(time/(60*100000)-237900); // +1 on each 100 min
            //buildDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new java.util.Date(time));
            //zf.close();
        } catch (Exception e) { versionNum = "unknown"; }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_map, menu);
        if (menuCreatedListener != null)
            menuCreatedListener.onMenuCreated();

        return super.onCreateOptionsMenu(menu);
    }

    public void onDelayItemSelected(int type) {
        Delay.setType(type);
        resetRoute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();

        if( id>=DelayFirst && id<DelayFirst+DelaySize )  {
            if (!item.isChecked()) {
                item.setChecked(true);
                onDelayItemSelected(id - DelayFirst);
            }
            return true;
        }

        if( id>=TransportFirst && id<TransportFirst+TransportSize )  {
            if( item.isChecked() ) {
                mapData.routingState.removeActive(id-TransportFirst);
                item.setChecked(false);
            } else {
                if (mapData.routingState.addActive(id - TransportFirst))
                    item.setChecked(true);
            }

            resetRoute();
            return true;
        }

        switch( id ) {
             case R.id.action_none:
                 item.setChecked(true);
                 onDelayItemSelected(-1);
                 return true;
             case R.id.action_open:
                 runMapSelect();
                 return true;
             case R.id.action_settings:
                 intent = new Intent(this, SettingsActivity.class);
                 this.startActivity(intent);
                 return true;
             case R.id.action_about:
                 intent = new Intent(this, AboutActivity.class);
                    intent.putExtra("MapAuthors", mapData.cty.MapAuthors);
                 this.startActivity(intent);
                 return true;
            default:
                 return super.onOptionsItemSelected(item);
        }
    }

    private void resetRoute() {
        mapData.routingState.resetRoute();
        mapData.map.clearStationTimes();
        mapView.redraw();
    }

    /*public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //mapView.myContextMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        mapView.selectedStation(item.getItemId()-1);
        return true;
    }//*/

    public void loadFail() {
        Toast.makeText(this, "Select map.", Toast.LENGTH_LONG).show();
        runMapSelect();
    }

    private void runMapSelect() {
        Intent intent;
        intent = new Intent(this, SettingsActivity.class);
        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, com.utyf.pmetro.settings.CatalogManagement.class.getName() );
        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        this.startActivity(intent);
    }

    private void resetMenu() {  // cleanup menu after previous map
        int i;
        if (menu == null) {
            Log.e("resetMenu", "Menu must exist at this point");
            return;
        }

        SubMenu sub = menu.findItem(R.id.action_wait_time).getSubMenu();
        for( i=DelayFirst; i<DelayFirst+DelaySize; i++ )  sub.removeItem(i);

        sub = menu.findItem(R.id.action_transport).getSubMenu();
        for( i=TransportFirst; i<TransportFirst+TransportSize; i++ )  sub.removeItem(i);
    }

    public void fillMenuItems() {
        // add menu section "Wait time" and "Transports"
        if (menu != null) {
            resetMenu();
            setDelays();
            setTRPMenu();
            menuCreatedListener = null;
        }
        else {
            // Fill menu later, after it has been created
            menuCreatedListener = new MenuCreatedListener() {
                @Override
                public void onMenuCreated() {
                    fillMenuItems();
                }
            };
        }
    }

    private void setDelays() {
        int i;
        if (menu == null) {
            Log.e("setDelays", "Menu must exist at this point");
            return;
        }
        SubMenu sub = menu.findItem(R.id.action_wait_time).getSubMenu();

        //for( i=DelayFirst; i<DelayFirst+DelaySize; i++ )  sub.removeItem(i); // cleanup menu after previous map
        for( i=0; i<Delay.getSize(); i++ )
            sub.add(R.id.action_group_wait_time, i + DelayFirst, i + DelayFirst, Delay.getName(i));

        sub.setGroupCheckable(R.id.action_group_wait_time, true, true);  // mark group as single-checkable
        menu.findItem(DelayFirst).setChecked(true);                      // choose first item
    }

    private void setTRPMenu() {
        if (menu == null) {
            Log.e("resetMenu", "Menu must exist at this point");
            return;
        }
        SubMenu sub = menu.findItem(R.id.action_transport).getSubMenu();

        //for( i=TransportFirst; i<TransportFirst+TransportSize; i++ )  sub.removeItem(i); // cleanup menu after previous map
        for (int i = 0; i < mapData.transports.getSize(); i++) {
            sub.add(0, i + TransportFirst, i + TransportFirst, mapData.transports.getTRP(i).getType()).setCheckable(true);
        }

        setActiveTRP();
    }

    public void setActiveTRP() {
        int i=0;
        MenuItem item;

        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_transport).getSubMenu();

        while( (item=sub.findItem(TransportFirst+i))!=null )
            item.setChecked( mapData.routingState.isActive(i++) );
    }

    public void onBackPressed() {
        if( !mapData.mapBack() )  super.onBackPressed();
    }

    private void loadMapFile() {
        resetMenu();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(mapDataFragment);
        mapDataFragment = new MapDataFragment();
        transaction.add(mapDataFragment, MAP_DATA_FRAGMENT_TAG);
        transaction.commit();
        getMapDataFromFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (languageUpdater.isUpdateNeeded(SET.lang)) {
            recreate();
            return;
        }
        SET.load(this);
        // Reload map if it selected from catalog
        if (SET.newMapFile != null && !SET.newMapFile.isEmpty()) {
            SET.mapFile = SET.newMapFile;
            loadMapFile();
        }
        SET.newMapFile = null;
    }

    @Override
    protected void onPause() {
        SET.save();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapActivity = null;
        super.onDestroy();
    }

    public void setUpdateScheduler() {

        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Log.w("MapActivity /391", "Set scheduler to - "+SET.cat_upd);

        switch (SET.cat_upd) {
            case "Weekly":
                manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
                break;
            case "Daily":
                manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                //manager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis()+60000, 60*1000, pendingIntent);
                break;
            case "On start program":
                manager.cancel(pendingIntent);
                break;
            case "Manually":
                manager.cancel(pendingIntent);
                break;
            default:
                Log.e("MapActivity /401", "Incorrect scheduler value");
        }

        //SimpleDateFormat sdf = new SimpleDateFormat("hh mm ss");
        //Toast.makeText(this, "Alarm Set "+SET.cat_upd +" "+sdf.format(new Date()), Toast.LENGTH_SHORT).show();
    }
}
