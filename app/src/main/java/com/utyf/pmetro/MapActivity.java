package com.utyf.pmetro;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import com.utyf.pmetro.settings.SET;
import com.utyf.pmetro.settings.SettingsActivity;
import com.utyf.pmetro.map.Delay;
import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.map.TRP;

import java.io.File;
import java.text.DateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class MapActivity extends Activity {

    public static AssetManager asset;
    public static MapActivity  mapActivity;
    public static File         fileDir;
    public static File         catalogDir;
    public static File         cacheDir;
    public static NetworkInfo  netInfo;
    public static boolean  debugMode;
    public static int      numberOfCores = Runtime.getRuntime().availableProcessors();
    public static long     maxMemory = Runtime.getRuntime().maxMemory()/1024/1024;
    public static String   versionNum;
    public static int      buildNum;
    public static String   buildDate;
    public static String   errorMessage="";
    public static long     calcTime; //, calcBTime;
    public static long     makeRouteTime;
    public  Map_View mapView;
    private Menu     menu;

    final int DelayFirst = Menu.FIRST;
    final int DelaySize = 9;
    final int TransportFirst = DelayFirst+DelaySize;
    final int TransportSize = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asset = getAssets();
        mapActivity = this;
        SET.load();
//SET.mapFile="Moscow_pix.pmz";

        fileDir = getExternalFilesDir(null);
        boolean bl = Environment.getExternalStorageState().toLowerCase().equals("mounted");
        if( fileDir==null || !bl )   fileDir = getFilesDir();
        catalogDir = new File(fileDir+"/catalog");
        cacheDir = getCacheDir();

        getBuild();

        isOnline(true);

        mapView = new Map_View(this);
        setContentView(mapView);

        if( !MapData.isReady ) loadMapFile();

        if( maxMemory<127 )
            Toast.makeText(this, "Low memory\n"+Long.toString(maxMemory)+"Mb RAM available.", Toast.LENGTH_LONG).show();
    }

    public static boolean isOnline() {
        return isOnline(false);
    }

    public static boolean isOnline(boolean quite) {
        ConnectivityManager cm =
                (ConnectivityManager) mapActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        netInfo = cm.getActiveNetworkInfo();
        if( netInfo != null && netInfo.isConnectedOrConnecting() )   return true;    // todo   check for mobile network
        if( !quite ) Toast.makeText(mapActivity,"Not connected to Internet",Toast.LENGTH_SHORT).show();
        return false;
    }

    private void getBuild() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo( getPackageName(), 0);
            versionNum = info.versionName;
            buildNum = info.versionCode;

            ApplicationInfo ai = manager.getApplicationInfo(getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            long time = ze.getTime();
            // buildNum = "build: "+(time/(60*100000)-237900); // +1 on each 100 min
            buildDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new java.util.Date(time));
            zf.close();
        } catch (Exception e) { versionNum = "unknown";  buildDate=""; }
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

    public void loadFail() {
        Toast.makeText(this, "Select map.", Toast.LENGTH_LONG).show();
        Intent intent;
        intent = new Intent(MapActivity.mapActivity, SettingsActivity.class);
        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, com.utyf.pmetro.settings.CatalogManagement.class.getName() );
        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        MapActivity.mapActivity.startActivity(intent);
    }

    public void resetMenu() {  // cleanup menu after previous map
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

    public void setDelays() {
        int i;
        if( menu==null ) return;
        SubMenu sub = menu.findItem(R.id.action_wait_time).getSubMenu();

        //for( i=DelayFirst; i<DelayFirst+DelaySize; i++ )  sub.removeItem(i); // cleanup menu after previous map
        for( i=0; i<Delay.getSize(); i++ )
            sub.add(R.id.action_group_wait_time, i + DelayFirst, i + DelayFirst, Delay.getName(i));

        sub.setGroupCheckable(R.id.action_group_wait_time, true, true);  // mark group as single-checkable
        menu.findItem(DelayFirst).setChecked(true);                      // choose first item
    }

    public void setTRPMenu() {
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

    void loadMapFile() {
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

    void cleanCache() {
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
