package com.utyf.pmetro.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtInteger;

/**
 * Created by Utyf on 17.04.2015.
 *
 *  Application settings stored in the com.utyf.pmetro_preferences.xml
 *
 */

public class SET {

    static final String KEY_ROUTE_DIFFERENCE = "Route_difference";
    static final String KEY_ROUTE_MAX_TRANSFERS = "Route_max_transfers";
    static final String KEY_CATALOG_STORAGE = "Catalog_storage";
    static final String KEY_LANGUAGE = "Language";
    static final String KEY_CATALOG_SITE = "Catalog_site";
    static final String KEY_SITE_MAP_PATH = "Site_map_path";
    static final String KEY_CATALOG_LIST = "Catalog_list";
    static final String KEY_CATALOG_UPDATE = "Catalog_update";
    static final String KEY_CAT_UPD_CURRENT = "Catalog_update_current";
    static final String KEY_CAT_UPD_LAST = "Catalog_update_last";
    static final String KEY_MAP_FILE = "Map_file";
    static final String KEY_HW_ACCELERATION = "HW_Acceleration";
    static final String KEY_BUILD_NUM = "Build_number";

    public static int    rDif = 3;
    public static int    maxTransfer = 5;
    public static String storage = "Local";
    public static String lang = "en";
    public static String site = "http://pmetro.su";
    public static String mapPath = "/download";
    public static String catalogList = "/Files.xml";
    public static String cat_upd = "Weekly";
    public static String cat_upd_current = "";
    public static long cat_date_last = 0;   // time of last catalog update
    public static String mapFile = "";
    public static String newMapFile;
    public static boolean hw_acceleration=true;
    public static int buildNum = 0;  // version of setting file

    public static void load(Context cntx) {
        SharedPreferences sp = cntx.getSharedPreferences("com.utyf.pmetro_preferences", 0);

        String rDifString = sp.getString(KEY_ROUTE_DIFFERENCE, "");
        if (!rDifString.isEmpty()) {
            rDif = ExtInteger.parseInt(rDifString);
        }

        String maxTransferString = sp.getString(KEY_ROUTE_MAX_TRANSFERS, "");
        if (!maxTransferString.isEmpty()) {
            maxTransfer = ExtInteger.parseInt(maxTransferString);
        }

        storage = sp.getString(KEY_CATALOG_STORAGE, storage);
        lang = sp.getString(KEY_LANGUAGE, lang);
        site = sp.getString(KEY_CATALOG_SITE, site);
        mapPath = sp.getString(KEY_SITE_MAP_PATH, mapPath);
        catalogList = sp.getString(KEY_CATALOG_LIST, catalogList);
        cat_upd = sp.getString(KEY_CATALOG_UPDATE, cat_upd);
        cat_upd_current = sp.getString(KEY_CAT_UPD_CURRENT, cat_upd_current);
        cat_date_last = sp.getLong(KEY_CAT_UPD_LAST, cat_date_last);
        mapFile = sp.getString(KEY_MAP_FILE, mapFile);
        hw_acceleration = sp.getBoolean(KEY_HW_ACCELERATION, hw_acceleration);
        buildNum = sp.getInt(KEY_BUILD_NUM, buildNum);

        if( MapActivity.mapActivity==null ) return;

        checkUpdateScheduler();
        if( buildNum!=MapActivity.buildNum ) { // upgrade settings
            site = site.replaceAll("[/]+$","");
            if( site.toLowerCase().endsWith("pmetro.su/download") )
                site = "http://pmetro.su";
            save();
        }
        buildNum = MapActivity.buildNum;
    }

    static boolean checkUpdateScheduler() {
        if( MapActivity.mapActivity==null ) return false;

        if( cat_upd.equals(cat_upd_current) )  return false; //  && buildNum==MapActivity.buildNum

        cat_upd_current = cat_upd;
        MapActivity.mapActivity.setUpdateScheduler();
        save();
        return true;
    }

    public static void save() {
//        checkUpdateScheduler();
        if( MapActivity.mapActivity==null ) return;

        SharedPreferences sp = MapActivity.mapActivity.getSharedPreferences ("com.utyf.pmetro_preferences", 0);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(KEY_ROUTE_DIFFERENCE, Integer.toString(rDif));
        ed.putString(KEY_ROUTE_MAX_TRANSFERS, Integer.toString(maxTransfer));
        ed.putString(KEY_CATALOG_STORAGE, storage);
        ed.putString(KEY_LANGUAGE, lang);
        ed.putString(KEY_CATALOG_SITE, site);
        ed.putString(KEY_SITE_MAP_PATH, mapPath);
        ed.putString(KEY_CATALOG_LIST, catalogList);
        ed.putString(KEY_CATALOG_UPDATE, cat_upd);
        ed.putString(KEY_CAT_UPD_CURRENT, cat_upd_current);
        ed.putLong(KEY_CAT_UPD_LAST, cat_date_last);
        ed.putString(KEY_MAP_FILE, mapFile);
        ed.putBoolean(KEY_HW_ACCELERATION, hw_acceleration);
        ed.putInt(KEY_BUILD_NUM, MapActivity.buildNum);
        ed.commit();
    }
}
