package com.utyf.pmetro;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


import android.app.AlarmManager;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.utyf.pmetro.settings.AlarmReceiver;
import com.utyf.pmetro.settings.CatalogList;
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
    public static long     makeRouteTime;
    public  Map_View mapView;
    private Menu     menu;

    private final static int DelayFirst = Menu.FIRST;
    private final static int DelaySize = 9;
    private final static int TransportFirst = DelayFirst+DelaySize;
    private final static int TransportSize = 99;
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

        mapActivity = this;

        getBuild();
        SET.load(this);
        getDirs(this);

        // isOnline(true);
        if( SET.cat_upd.equals("On start program") )
            CatalogList.updateAll(true, this);

        mapView = new Map_View(this);
        setContentView(mapView);
        //registerForContextMenu(mapView);

        if( !MapData.isReady ) loadMapFile();

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

    // ----- custom context menu -----
    public void showStationsMenu(final StationsNum[] stns) {
        List<ContextMenuItem> contextMenuItems;
        final Dialog customDialog;

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
        customDialog.setTitle(R.string.choose_station);
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
                 intent = new Intent(this, SettingsActivity.class);
                 this.startActivity(intent);
                 return true;
             case R.id.action_about:
                 intent = new Intent(this, AboutActivity.class);
                 this.startActivity(intent);
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
        intent = new Intent(this, SettingsActivity.class);
        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, com.utyf.pmetro.settings.CatalogManagement.class.getName() );
        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        this.startActivity(intent);
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
        for( i=0; i<TRP.getSize(); i++ ) {
            TRP trp = TRP.getTRP(i);
            if( trp!=null )
                sub.add(0, i + TransportFirst, i + TransportFirst, trp.Type).setCheckable(true);
        }

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
        SET.load(this);

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
