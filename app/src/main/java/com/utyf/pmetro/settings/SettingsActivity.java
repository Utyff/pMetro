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
        public static final String KEY_LANGUAGE = "Language";
        public static final String KEY_ROUTE_DIFFERENCE = "Route_difference";
        public static final String KEY_ROUTE_MAX_TRANSFERS = "Route_max_transfers";

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
            connectionPref = findPreference(KEY_LANGUAGE);
            connectionPref.setSummary(sp.getString(KEY_LANGUAGE, ""));
            connectionPref = findPreference(KEY_ROUTE_DIFFERENCE);
            connectionPref.setSummary(sp.getString(KEY_ROUTE_DIFFERENCE, ""));
            connectionPref = findPreference(KEY_ROUTE_MAX_TRANSFERS);
            connectionPref.setSummary(sp.getString(KEY_ROUTE_MAX_TRANSFERS, ""));
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
                case KEY_LANGUAGE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
                case KEY_ROUTE_DIFFERENCE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
                case KEY_ROUTE_MAX_TRANSFERS:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
            }
        }
    }

    public static class CatalogFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String CATALOG_STORAGE = "Catalog_storage";
        public static final String KEY_CATALOG_SITE = "Catalog_site";
        public static final String KEY_CATALOG_UPDATE = "Catalog_update";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_catalog);

            Preference connectionPref;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            connectionPref = findPreference(CATALOG_STORAGE);
            connectionPref.setSummary(sp.getString(CATALOG_STORAGE, ""));
            connectionPref = findPreference(KEY_CATALOG_SITE);
            connectionPref.setSummary(sp.getString(KEY_CATALOG_SITE, ""));
            connectionPref = findPreference(KEY_CATALOG_UPDATE);
            connectionPref.setSummary(sp.getString(KEY_CATALOG_UPDATE, ""));
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
                case CATALOG_STORAGE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
                case KEY_CATALOG_SITE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
                case KEY_CATALOG_UPDATE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    break;
            }
        }
    }
}
