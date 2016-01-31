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
    //static final String key_lang = "Language";
    static final String KEY_CATALOG_SITE = "Catalog_site";
    static final String KEY_CATALOG_UPDATE = "Catalog_update";
    static final String KEY_CAT_UPD_CURRENT = "Catalog_update_current";
    static final String KEY_CAT_UPD_LAST = "Catalog_update_last";
    static final String KEY_MAP_FILE = "Map_file";
    static final String KEY_HW_ACCELERATION = "HW_Acceleration";

    public static int    rDif = 3;
    public static int    maxTransfer = 5;
    public static String storage = "Local";
    //public static String lang = "English";
    public static String site = "http://pmetro.su";
    public static String cat_upd = "Weekly";
    public static String cat_upd_current = "";
    public static long cat_upd_last = 0;   // time of last catalog update
    public static String mapFile = "";
    public static String newMapFile;
    public static boolean hw_acceleration=true;

    public static void load(Context cntx) {
        SharedPreferences sp = cntx.getSharedPreferences("com.utyf.pmetro_preferences", 0);
        rDif = ExtInteger.parseInt(sp.getString(KEY_ROUTE_DIFFERENCE, "3"));
        maxTransfer = ExtInteger.parseInt(sp.getString(KEY_ROUTE_MAX_TRANSFERS, "5"));
        storage = sp.getString(KEY_CATALOG_STORAGE, "Local");
        //lang = sp.getString(key_lang, "English");
        site = sp.getString(KEY_CATALOG_SITE, "http://pmetro.su");
        cat_upd = sp.getString(KEY_CATALOG_UPDATE, "Weekly");
        cat_upd_current = sp.getString(KEY_CAT_UPD_CURRENT, "");
        cat_upd_last = sp.getLong(KEY_CAT_UPD_LAST, 0);
        mapFile = sp.getString(KEY_MAP_FILE, "");
        hw_acceleration = sp.getBoolean(KEY_HW_ACCELERATION, true);

        checkUpdateScheduler();
    }

    static boolean checkUpdateScheduler() {
        if( MapActivity.mapActivity==null ) return false;

        if( cat_upd.equals(cat_upd_current) )  return false;

        cat_upd_current = cat_upd;
        MapActivity.mapActivity.setUpdateScheduler();
        return true;
    }

    public static void save() {
//        checkUpdateScheduler();

        SharedPreferences sp = MapActivity.mapActivity.getSharedPreferences ("com.utyf.pmetro_preferences", 0);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(KEY_ROUTE_DIFFERENCE, Integer.toString(rDif));
        ed.putString(KEY_ROUTE_MAX_TRANSFERS, Integer.toString(maxTransfer));
        ed.putString(KEY_CATALOG_STORAGE, storage);
        //ed.putString(key_lang, lang);
        ed.putString(KEY_CATALOG_SITE, site);
        ed.putString(KEY_CATALOG_UPDATE, cat_upd);
        ed.putString(KEY_CAT_UPD_CURRENT, cat_upd_current);
        ed.putLong(KEY_CAT_UPD_LAST, cat_upd_last);
        ed.putString(KEY_MAP_FILE, mapFile);
        ed.putBoolean(KEY_HW_ACCELERATION, hw_acceleration);
        ed.commit();
    }
}
