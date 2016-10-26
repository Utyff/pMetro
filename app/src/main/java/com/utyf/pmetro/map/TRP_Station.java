package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.Util;

import java.util.LinkedList;

/**
 * Created by Fedor on 26.10.2016.
 */
public class TRP_Station {
    public String name;  //, alias;  // todo
    boolean isWorking;
    public LinkedList<TRP_Driving> drivings;

    public void addDriving(String names) {

        String[] strs = Util.split(names, ',');
        if (drivings == null) drivings = new LinkedList<>();

        for (int i = 0; i < strs.length; i += 2)
            if (i + 1 >= strs.length)   // is there last name?
                drivings.add(new TRP_Driving(strs[i].trim(), ""));
            else
                drivings.add(new TRP_Driving(strs[i].trim(), strs[i + 1].trim()));
    }

    public void addDrivingEmpty() {
        if (drivings != null) {
            Log.e("TRP /354", "Driving not empty.");
            return;
        }
        drivings = new LinkedList<>();
        drivings.add(new TRP_Driving("-", "-"));
    }

    public void setDrivingTime(String times) {

        String[] strs = times.split(",");
        int j = 0;
        for (int i = 0; i < strs.length; i += 2) {
            if (drivings.size() <= j) {
                Log.e("TRP /405", "Driving fork not the same as station fork");
                return;
            }
            if (i + 1 >= strs.length)  // is there last time?
                drivings.get(j).setTimes(strs[i], "");
            else
                drivings.get(j).setTimes(strs[i], strs[i + 1]);
            j++;
        }
    }
}  // TRP_Station
