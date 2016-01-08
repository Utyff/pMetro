package com.utyf.pmetro.settings;

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

    static final String key_rDif = "Route_difference";
    static final String key_maxTransfer = "Route_max_transfers";
    static final String key_storage = "Catalog_storage";
    //static final String key_lang = "Language";
    static final String key_site = "Catalog_site";
    static final String key_cat_upd = "Catalog_update";
    static final String key_cat_upd_current = "Catalog_update_current";
    static final String key_mapFile = "Map_file";

    public static int    rDif = 3;
    public static int    maxTransfer = 5;
    public static String storage = "Local";
    //public static String lang = "English";
    public static String site = "http://pmetro.su";
    public static String cat_upd = "Weekly";
    public static String cat_upd_current = "";
    public static String mapFile = "";
    public static String newMapFile;

    public static void load() {
        SharedPreferences sp = MapActivity.mapActivity.getSharedPreferences("com.utyf.pmetro_preferences", 0);
        rDif = ExtInteger.parseInt( sp.getString(key_rDif, "3") );
        maxTransfer = ExtInteger.parseInt(sp.getString(key_maxTransfer, "5"));
        storage = sp.getString(key_storage, "Local");
        //lang = sp.getString(key_lang, "English");
        site = sp.getString(key_site, "http://pmetro.su");
        cat_upd = sp.getString(key_cat_upd, "Weekly");
        cat_upd_current = sp.getString(key_cat_upd_current, "");
        mapFile = sp.getString(key_mapFile, "");

        if( checkUpdateScheduler() ) save();
    }

    private static boolean checkUpdateScheduler() {
        if( cat_upd.equals(cat_upd_current) )  return false;

        cat_upd_current = cat_upd;
        MapActivity.mapActivity.setUpdateScheduler();
        return true;
    }

    public static void save() {
        checkUpdateScheduler();

        SharedPreferences sp = MapActivity.mapActivity.getSharedPreferences ("com.utyf.pmetro_preferences", 0);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(key_rDif, Integer.toString(rDif));
        ed.putString(key_maxTransfer, Integer.toString(maxTransfer));
        ed.putString(key_storage, storage);
        //ed.putString(key_lang, lang);
        ed.putString(key_site, site);
        ed.putString(key_cat_upd, cat_upd);
        ed.putString(key_cat_upd_current, cat_upd_current);
        ed.putString(key_mapFile, mapFile);
        ed.commit();
    }
}
