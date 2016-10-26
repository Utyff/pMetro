package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

import java.util.ArrayList;

/**
 * Created by Fedor on 26.10.2016.
 */
public class TRP_line {

    public Delay delays;
    public String name, alias, LineMap, Aliases;
    public TRP_Station[] Stations;

    boolean Load(Section sec) {
        float day, night;
        String str;

        name = sec.getParamValue("Name");
        alias = sec.getParamValue("Alias");
        LineMap = sec.getParamValue("LineMap");
        Aliases = sec.getParamValue("Aliases"); // todo
        str = sec.getParamValue("Delays");
        if (!str.isEmpty()) delays = new Delay(str);
        else {
            if (sec.getParamValue("DelayDay").isEmpty() || sec.getParamValue("DelayNight").isEmpty())
                delays = new Delay();
            else {
                day = ExtFloat.parseFloat(sec.getParamValue("DelayDay"));
                night = ExtFloat.parseFloat(sec.getParamValue("DelayNight"));
                delays = new Delay(day, night);
            }
        }

        Stations = new TRP_Station[0];
        LoadStations(sec.getParamValue("Stations"));
        LoadDriving(sec.getParamValue("Driving"));

        for (TRP_Station st : Stations)
            for (TRP_Driving drv : st.drivings) {
                if (drv.bckStNum < 0 && drv.bckDR > 0)
                    Log.e("TRP /452", "Bad back driving. Line - " + name + ", Station - " + st.name);
                if (drv.frwStNum < 0 && drv.frwDR > 0)
                    Log.e("TRP /454", "Bad forw driving. Line - " + name + ", Station - " + st.name);
            }
        return true;
    }

    void LoadDriving(String drv) {
        int i, i2, stNum = 0;
        TRP_Station st;

        drv = drv.trim();

        while (!drv.isEmpty()) {
            if (stNum >= Stations.length) {
                Log.e("TRP /467", "Driving more then stations");
                return;
            }

            st = Stations[stNum];

            if (drv.charAt(0) == '(') { // is it fork?
                i = drv.indexOf(')');
                if (i == -1) {
                    Log.e("TRP /475", "Bad driving times. There is not closing ')' - " + drv);
                    return;
                }

                st.setDrivingTime(drv.substring(1, i));
                drv = drv.substring(i + 1);                    // remove till ')'
                if (!drv.isEmpty()) drv = drv.substring(1); // remove ','
            } else {
                i = drv.indexOf(',');
                if (i == -1) i2 = i = drv.length();
                else i2 = i + 1;

                st.drivings.get(0).frwDR = TRP.String2Time(drv.substring(0, i));
                if (stNum > 0) st.drivings.get(0).bckDR = getForwTime(Stations[stNum - 1], st);
                else st.drivings.get(0).bckDR = -1;

                drv = drv.substring(i2);
            }

            stNum++;
        }

        if (stNum < Stations.length) {  // if for last station was not data
            st = Stations[stNum];
            if (stNum > 0) st.drivings.get(0).bckDR = getForwTime(Stations[stNum - 1], st);
            else st.drivings.get(0).bckDR = -1;
            st.drivings.get(0).frwDR = -1;
        }

        for (TRP_Station st2 : Stations)          // set stations numbers for fork drivings
            for (TRP_Driving dr : st2.drivings) {
                if (dr.frwST.equals("-") || dr.bckST.equals("-"))
                    Log.e("TRP /461", "Station " + st2.name + " bad driving name.");
                if (dr.frwStNum == -1) dr.frwStNum = getStationNum(dr.frwST);
                if (dr.bckStNum == -1) dr.bckStNum = getStationNum(dr.bckST);
                if (dr.frwDR > 0 || dr.bckDR > 0)
                    st2.isWorking = true;  // check all stations - is it working
            }
    }

    public float getForwTime(TRP_Station st1, TRP_Station st2) {
        for (TRP_Driving td : st1.drivings)
            if (td.frwST.equals(st2.name)) return td.frwDR;
        return -1;
    }

    /*private float getBackTime(TRP_Station st1, TRP_Station st2 ) {
        for( TRP_Driving td : st1.drivings )
            if( td.bckST.equals(st2.name) ) return td.bckDR;
        return -1;
    } //*/

    private String stnStr;

    void LoadStations(final String stn) {
        TRP_Driving dr, dr2;
        TRP_Station st = null, st2 = null;
        stnStr = stn;
        ArrayList<TRP_Station> sa = new ArrayList<>();

        while (!stnStr.isEmpty()) {  // loading stations names

            st = getStationEntry();
            if (st == null || st.name == null || st.name.isEmpty()) {
                Log.e("TRP /490", "Bad station string - " + stn);
                return;
            }

            if (st2 == null) {  // set stations for default directions
                if (st.drivings.get(0).bckST.equals("-")) st.drivings.get(0).bckST = "";
            } else {
                dr = st.drivings.get(0);
                dr2 = st2.drivings.get(0);
                if (dr2.frwST.equals("-")) {
                    dr2.frwST = st.name;
                    dr2.frwStNum = sa.size();
                }
                if (dr.bckST.equals("-")) {
                    dr.bckST = st2.name;
                    dr.bckStNum = sa.size() - 1;
                }
            }

            st2 = st;
            sa.add(st);
        }
        Stations = sa.toArray(new TRP_Station[sa.size()]);
        stnStr = null; // free memory

        if (st != null && st.drivings.get(0).frwST.equals("-"))
            st.drivings.get(0).frwST = "";  // remove forward for last station
    }

    private TRP_Station getStationEntry() {
        TRP_Station st = new TRP_Station();
        int n;

        st.name = getName();
        if (st.name == null || st.name.isEmpty()) return null;

        if (stnStr.isEmpty() || stnStr.charAt(0) == ',')  // is there fork ?
            st.addDrivingEmpty();                        // will set next on the stage
        else {
            if (stnStr.charAt(0) != '(') return null;
            n = stnStr.indexOf(')');
            if (n < 0) return null;

            st.addDriving(stnStr.substring(1, n));  // load fork
            stnStr = stnStr.substring(n + 1);          // remove till ")"
        }
        stnStr = stnStr.trim();
        if (!stnStr.isEmpty())
            if (stnStr.charAt(0) == ',') stnStr = stnStr.substring(1);
            else Log.e("TRP /532", "Wrong station format");

        return st;
    }

    private String getName() {
        int i;
        String name = "";

        if (stnStr == null || stnStr.isEmpty()) return null;
        stnStr = stnStr.trim();
        if (stnStr.charAt(0) == '"') {
            i = stnStr.indexOf('"', 1);
            if (i < 0) return null;

            name = stnStr.substring(1, i);
            stnStr = stnStr.substring(i + 1);  // remove name and next symbol "
        } else
            while (!stnStr.isEmpty()) {
                if (stnStr.charAt(0) == '(' || stnStr.charAt(0) == ')' || stnStr.charAt(0) == ',')
                    return name;
                name = name + stnStr.charAt(0);
                stnStr = stnStr.substring(1);
            }

        stnStr = stnStr.trim();
        return name.trim();
    }

    /*private String getBackWay(TRP_Station st1, TRP_Station st2) {  // return back way if there is forward way
        for( TRP_Driving td : st2.drivings )
            if( td.frwST.equals(st1.name) )    return st2.name;
        return "";
    } //*/

    public TRP_Station getStation(int st) {
        if (Stations == null || st < 0 || Stations.length <= st) return null;
        return Stations[st];
    }

    public String getStationName(int num) {
        if (Stations == null || Stations.length <= num) return null;
        return Stations[num].name;
    }

    int getStationNum(String name) {
        if (Stations == null) return -1;
        if (name.isEmpty()) return -1;
        int sz = Stations.length;
        for (int i = 0; i < sz; i++)
            if (Stations[i].name.equals(name)) return i;
        return -1;
    }
}  // class TRP_line
