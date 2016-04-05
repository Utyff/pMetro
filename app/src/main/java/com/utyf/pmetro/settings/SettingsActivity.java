package com.utyf.pmetro.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.utyf.pmetro.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Utyf on 12.04.2015.
 *
 */

public class SettingsActivity  extends PreferenceActivity {

    static boolean exit;
    static ArrayList<SettingsActivity> listAct = new ArrayList<>();

    static void addAct(SettingsActivity act) {
        if( listAct.indexOf(act)==-1 ) listAct.add(act);
    }

    static boolean delAct(SettingsActivity act) {
        listAct.remove(act);
        return listAct.isEmpty();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addAct(this);
    }

    @Override
    public void onDestroy() {
        if( delAct(this) ) exit = false;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if( exit ) finish();
        super.onResume();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        //return StockPreferenceFragment.class.getName().equals(fragmentName);
        return true; // MyPreferenceFragmentA.class.getName().equals(fragmentName)
                     // || MyPreferenceFragmentB.class.getName().equals(fragmentName)
    }

    public static class GeneralFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences_catalog2, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_general);

            Preference connectionPref;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            //connectionPref = findPreference(KEY_LANGUAGE);
            //connectionPref.setSummary(sp.getString(KEY_LANGUAGE, ""));
            connectionPref = findPreference(SET.KEY_ROUTE_DIFFERENCE);
            connectionPref.setSummary(sp.getString(SET.KEY_ROUTE_DIFFERENCE, ""));
            connectionPref = findPreference(SET.KEY_ROUTE_MAX_TRANSFERS);
            connectionPref.setSummary(sp.getString(SET.KEY_ROUTE_MAX_TRANSFERS, ""));
            //connectionPref = findPreference(SET.KEY_HW_ACCELERATION);
            //connectionPref.setSummary(sp.getBoolean(KEY_HW_ACCELERATION, true) ? "true" : "false");
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Preference connectionPref;
            switch (key) {
                /*case KEY_LANGUAGE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break; */
                case SET.KEY_ROUTE_DIFFERENCE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.rDif = sharedPreferences.getInt(key, 3);
                    break;
                case SET.KEY_ROUTE_MAX_TRANSFERS:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.maxTransfer = sharedPreferences.getInt(key, 5);
                    break;
                case SET.KEY_HW_ACCELERATION:
                    SET.hw_acceleration = sharedPreferences.getBoolean(key, true);
                    // TODO change mode for current view
                    break;
            }
        }
    }

    public static class CatalogFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_catalog);

            Preference connectionPref;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            connectionPref = findPreference(SET.KEY_CATALOG_STORAGE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_STORAGE, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_SITE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_SITE, ""));
            connectionPref = findPreference(SET.KEY_SITE_MAP_PATH);
            connectionPref.setSummary(sp.getString(SET.KEY_SITE_MAP_PATH, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_LIST);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_LIST, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_UPDATE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_UPDATE, ""));
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Preference connectionPref;
            switch (key) {
                case SET.KEY_CATALOG_STORAGE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.storage = sharedPreferences.getString(key, "Local");
                    break;
                case SET.KEY_CATALOG_SITE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, "http://pmetro.su");
                    break;
                case SET.KEY_SITE_MAP_PATH:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, "/download");
                    break;
                case SET.KEY_CATALOG_LIST:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, "/Files.xml");
                    break;
                case SET.KEY_CATALOG_UPDATE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.cat_upd = sharedPreferences.getString(key, "Weekly");
                    SET.checkUpdateScheduler();
                    break;
            }
        }
    }
}
