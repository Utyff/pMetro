package com.utyf.pmetro;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.utyf.pmetro.settings.SET;
import com.utyf.pmetro.settings.SettingsActivity;
import com.utyf.pmetro.map.Delay;
import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.map.TRP;
import com.utyf.pmetro.util.ContextMenuAdapter;
import com.utyf.pmetro.util.ContextMenuItem;
import com.utyf.pmetro.util.StationsNum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MapActivity extends AppCompatActivity {

    //public static AssetManager asset;
    public static MapActivity  mapActivity;
    public static File         fileDir;
    public static File         catalogDir;
    public static File         cacheDir;
    public static boolean  debugMode;
    public static int      numberOfCores = Runtime.getRuntime().availableProcessors();
    public static long     maxMemory = Runtime.getRuntime().maxMemory()/1024/1024;
    public static String   versionNum;
    public static int      buildNum;
    //public static String   buildDate;
    public static String   errorMessage="";
    public static long     calcTime; //, calcBTime;
    public static long     makeRouteTime;
    public  Map_View mapView;
    private Menu     menu;

    private final static int DelayFirst = Menu.FIRST;
    private final static int DelaySize = 9;
    private final static int TransportFirst = DelayFirst+DelaySize;
    private final static int TransportSize = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //asset = getAssets();
        mapActivity = this;
        SET.load();

        fileDir = getExternalFilesDir(null);
        boolean bl = Environment.getExternalStorageState().toLowerCase().equals("mounted");
        if( fileDir==null || !bl )   fileDir = getFilesDir();
        catalogDir = new File(fileDir+"/catalog");
        cacheDir = getCacheDir();

        getBuild();
        isOnline(true);

        mapView = new Map_View(this);
        setContentView(mapView);
        //registerForContextMenu(mapView);

        if( !MapData.isReady ) loadMapFile();

        if( maxMemory<127 )
            Toast.makeText(this, "Low memory\n"+Long.toString(maxMemory)+"Mb RAM available.", Toast.LENGTH_LONG).show();

        ActionBar mActionBar = getSupportActionBar();
        if( mActionBar!=null ) {
            //View viewBar = getLayoutInflater().inflate(R.layout.action_bar, null);
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setDisplayShowCustomEnabled(true);
            //mActionBar.setCustomView(viewBar);
            mActionBar.setCustomView(R.layout.action_bar);

            //TextView mTitleTextView = (TextView) viewBar.findViewById(R.id.title_text);
            //mTitleTextView.setText("My Own Title");
            //ImageButton imageButton = (ImageButton) mCustomView.findViewById(R.id.imageButton);
        } else
            Log.e("MapActivity /106", "Can't get action bar");
    }

    // ----- custom context menu -----
    public void showStationsMenu(StationsNum[] _stns) {
        List<ContextMenuItem> contextMenuItems;
        final Dialog customDialog;
        final StationsNum[] stns = _stns;

        LayoutInflater inflater;
        View child;
        ListView listView;
        ContextMenuAdapter adapter;

        inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        child = inflater.inflate(R.layout.listview_stations_context_menu, null);
        listView = (ListView) child.findViewById(R.id.listView_stations_context_menu);

        contextMenuItems = new ArrayList<>();
        for( StationsNum stn : stns )
            contextMenuItems.add(new ContextMenuItem(MapData.map.getLine(stn.trp,stn.line).Color, TRP.getStationName(stn)));

        adapter = new ContextMenuAdapter(this, contextMenuItems);
        listView.setAdapter(adapter);

        customDialog = new Dialog(this);
        //customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setTitle(getString(R.string.choose_station));
        customDialog.setContentView(child);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                customDialog.dismiss();
                mapView.selectStation(stns[position]);
            }
        });

        customDialog.show();
    }

    public boolean isOnline() {
        return isOnline(false);
    }

    public boolean isOnline(boolean quite) {
        ConnectivityManager cm =
                (ConnectivityManager) mapActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if( netInfo != null && netInfo.isConnectedOrConnecting() )   return true;
        if( !quite ) Toast.makeText(mapActivity,getString(R.string.no_internet),Toast.LENGTH_SHORT).show();
        return false;
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
        setMenu();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();

        if( id>=DelayFirst && id<DelayFirst+DelaySize )  {
            item.setChecked(true);
            Delay.setType( id-DelayFirst );
            mapView.redraw();
            return true;
        }

        if( id>=TransportFirst && id<TransportFirst+TransportSize )  {
            if( item.isChecked() ) {
                TRP.removeActive(id-TransportFirst);
                item.setChecked(false);
            } else
                if( TRP.addActive(id-TransportFirst) )
                     item.setChecked(true);

            TRP.resetRoute();
            mapView.redraw();
            return true;
        }

        switch( id ) {
             case R.id.action_none:
                 item.setChecked(true);
                 Delay.setType( -1 );
                 mapView.redraw();
                 return true;
             case R.id.action_open:
                 runMapSelect();
                 return true;
             case R.id.action_settings:
                 intent = new Intent(MapActivity.mapActivity, SettingsActivity.class);
                 MapActivity.mapActivity.startActivity(intent);
                 return true;
             case R.id.action_about:
                 intent = new Intent(MapActivity.mapActivity, AboutActivity.class);
                 MapActivity.mapActivity.startActivity(intent);
                 return true;
            default:
                 return super.onOptionsItemSelected(item);
        }
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
        intent = new Intent(MapActivity.mapActivity, SettingsActivity.class);
        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, com.utyf.pmetro.settings.CatalogManagement.class.getName() );
        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        MapActivity.mapActivity.startActivity(intent);
    }

    private void resetMenu() {  // cleanup menu after previous map
        int i;
        if( menu==null ) return;

        SubMenu sub = menu.findItem(R.id.action_wait_time).getSubMenu();
        for( i=DelayFirst; i<DelayFirst+DelaySize; i++ )  sub.removeItem(i);

        sub = menu.findItem(R.id.action_transport).getSubMenu();
        for( i=TransportFirst; i<TransportFirst+TransportSize; i++ )  sub.removeItem(i);
    }

    public void setMenu() {
        if( MapData.isReady ) {  // add menu section "Wait time" and "Transports"
            resetMenu();
            setDelays();
            setTRPMenu();
        }
    }

    private void setDelays() {
        int i;
        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_wait_time).getSubMenu();

        //for( i=DelayFirst; i<DelayFirst+DelaySize; i++ )  sub.removeItem(i); // cleanup menu after previous map
        for( i=0; i<Delay.getSize(); i++ )
            sub.add(R.id.action_group_wait_time, i + DelayFirst, i + DelayFirst, Delay.getName(i));

        sub.setGroupCheckable(R.id.action_group_wait_time, true, true);  // mark group as single-checkable
        menu.findItem(DelayFirst).setChecked(true);                      // choose first item
    }

    private void setTRPMenu() {
        int i;

        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_transport).getSubMenu();

        //for( i=TransportFirst; i<TransportFirst+TransportSize; i++ )  sub.removeItem(i); // cleanup menu after previous map
        for( i=0; i<TRP.getSize(); i++ )
            //noinspection ConstantConditions
            sub.add(0, i+TransportFirst, i+TransportFirst, TRP.getTRP(i).Type).setCheckable(true);

        setAllowedTRP();
        setActiveTRP();
    }

    public void setAllowedTRP() {
        int i;
        MenuItem item;

        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_transport).getSubMenu();

        for( i=0; (item=sub.findItem(TransportFirst+(i)))!=null; i++ )
            item.setEnabled( TRP.isAllowed(i) );
    }

    public void setActiveTRP() {
        int i=0;
        MenuItem item;

        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_transport).getSubMenu();

        while( (item=sub.findItem(TransportFirst+i))!=null )
            item.setChecked( TRP.isActive(i++) );
    }

    public void onBackPressed() {
        if( !MapData.mapBack() )  super.onBackPressed();
    }

    private void loadMapFile() {
        resetMenu();
        MapData.Load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SET.load();

        if( SET.newMapFile!=null && !SET.newMapFile.isEmpty() ) {
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
        cleanCache();
        super.onDestroy();
    }

    private void cleanCache() {
        File[]  fls = cacheDir.listFiles();
        for( File fl : fls ) {
            if( !fl.isDirectory() ) {
                // noinspection ResultOfMethodCallIgnored
                fl.delete();
                Log.e("Cache cleanup", fl.getName());
            }
        }
    }
}
