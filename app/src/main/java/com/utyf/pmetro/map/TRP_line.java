package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

import java.util.ArrayList;

/**
 * Created by Fedor on 26.10.2016.
 */
public class TRP_line {
    public final Delay delays;
    public final String name, alias, LineMap, Aliases;
    public final TRP_Station[] Stations;

    private TRP_line(Delay delays, String name, String alias, String LineMap, String Aliases, TRP_Station[] Stations) {
        this.delays = delays;
        this.name = name;
        this.alias = alias;
        this.LineMap = LineMap;
        this.Aliases = Aliases;
        this.Stations = Stations;
    }

    static TRP_line Load(Section sec) {
        float day, night;
        String str;

        String name = sec.getParamValue("Name");
        String alias = sec.getParamValue("Alias");
        String LineMap = sec.getParamValue("LineMap");
        String Aliases = sec.getParamValue("Aliases"); // todo
        str = sec.getParamValue("Delays");
        Delay delays;
        if (!str.isEmpty())
            delays = new Delay(str);
        else {
            if (sec.getParamValue("DelayDay").isEmpty() || sec.getParamValue("DelayNight").isEmpty())
                delays = new Delay();
            else {
                day = ExtFloat.parseFloat(sec.getParamValue("DelayDay"));
                night = ExtFloat.parseFloat(sec.getParamValue("DelayNight"));
                delays = new Delay(day, night);
            }
        }

        TRP_Station[] Stations = LoadStations(sec.getParamValue("Stations"));
        if (Stations == null) {
            Log.e("TRP_line", "Failed to load stations for line" + sec.name);
            return null;
        }
        LoadDriving(sec.getParamValue("Driving"), Stations);

        for (TRP_Station st : Stations)
            for (TRP_Driving drv : st.drivings) {
                if (drv.bckStNum < 0 && drv.bckDR > 0)
                    Log.e("TRP /452", "Bad back driving. Line - " + name + ", Station - " + st.name);
                if (drv.frwStNum < 0 && drv.frwDR > 0)
                    Log.e("TRP /454", "Bad forw driving. Line - " + name + ", Station - " + st.name);
            }
        return new TRP_line(delays, name, alias, LineMap, Aliases, Stations);
    }

    private static void LoadDriving(String drv, TRP_Station[] Stations) {
        int stNum = 0;
        TRP_Station st;

        final String str = drv.trim();
        int strLength = str.length();
        int pos = 0;

        while (pos < strLength) {
            if (stNum >= Stations.length) {
                Log.e("TRP /467", "Driving more then stations");
                return;
            }

            st = Stations[stNum];

            if (str.charAt(pos) == '(') { // is it fork?
                int closePos = str.indexOf(')', pos);
                if (closePos == -1) {
                    Log.e("TRP /475", "Bad driving times. There is not closing ')' - " + str);
                    return;
                }

                st.setDrivingTime(str.substring(pos + 1, closePos));
                pos = closePos + 1;  // remove till ')'
                if (pos < strLength)
                    pos += 1; // remove ','
            } else {
                int endPos = str.indexOf(',', pos);
                if (endPos == -1)
                    endPos = strLength;

                st.drivings.get(0).frwDR = TRP.String2Time(str.substring(pos, endPos));
                if (stNum > 0) st.drivings.get(0).bckDR = getForwTime(Stations[stNum - 1], st);
                else st.drivings.get(0).bckDR = -1;

                pos = endPos + 1;  // can become greater than strLength!
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
                if (dr.frwStNum == -1) dr.frwStNum = getStationNum(dr.frwST, Stations);
                if (dr.bckStNum == -1) dr.bckStNum = getStationNum(dr.bckST, Stations);
                if (dr.frwDR > 0 || dr.bckDR > 0)
                    st2.isWorking = true;  // check all stations - is it working
            }
    }

    public static float getForwTime(TRP_Station st1, TRP_Station st2) {
        for (TRP_Driving td : st1.drivings)
            if (td.frwST.equals(st2.name)) return td.frwDR;
        return -1;
    }

    /*private float getBackTime(TRP_Station st1, TRP_Station st2 ) {
        for( TRP_Driving td : st1.drivings )
            if( td.bckST.equals(st2.name) ) return td.bckDR;
        return -1;
    } //*/

    private static TRP_Station[] LoadStations(String stn) {
        TRP_Driving dr, dr2;
        TRP_Station st = null, st2 = null;
        ArrayList<TRP_Station> sa = new ArrayList<>();

        StationsParser parser = new StationsParser(stn);

        while (parser.hasStations()) {  // loading stations names

            st = parser.getStationEntry();
            if (st == null || st.name == null || st.name.isEmpty()) {
                Log.e("TRP /490", "Bad station string - " + stn);
                return null;
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
        TRP_Station[] Stations = sa.toArray(new TRP_Station[sa.size()]);

        if (st != null && st.drivings.get(0).frwST.equals("-"))
            st.drivings.get(0).frwST = "";  // remove forward for last station
        return Stations;
    }

    private static class StationsParser {
        private final String str;
        private final int strLength;
        private int pos;

        public StationsParser(String str) {
            this.str = str;
            this.strLength = str.length();
            pos = 0;
        }

        private TRP_Station getStationEntry() {
            TRP_Station st = new TRP_Station();

            st.name = getName();
            if (st.name == null || st.name.isEmpty()) return null;

            if (pos >= strLength || str.charAt(pos) == ',')  // is there fork ?
                st.addDrivingEmpty();                        // will set next on the stage
            else {
                if (str.charAt(pos) != '(') return null;
                int closingPos = str.indexOf(')', pos);
                if (closingPos < 0) return null;

                st.addDriving(str.substring(pos + 1, closingPos));  // load fork
                pos = closingPos + 1;  // remove till ")"
            }
            if (pos < strLength) {
                if (str.charAt(pos) == ',')
                    ++pos;
                else Log.e("TRP /532", "Wrong station format");
            }

            return st;
        }

        private String getName() {
            String name;

            if (pos >= strLength) return null;
            if (str.charAt(pos) == '"') {
                int closingPos = str.indexOf('"', pos + 1);
                if (closingPos < 0) return null;

                name = str.substring(pos + 1, closingPos);
                pos = closingPos + 1;  // remove name and next symbol "
            } else {
                int startPos = pos;
                while (pos < strLength) {
                    char nextChar = str.charAt(pos);
                    if (nextChar == '(' || nextChar == ')' || nextChar == ',')
                        break;
                    pos += 1;
                }
                name = str.substring(startPos, pos);
            }

            return name.trim();
        }

        public boolean hasStations() {
            return pos < strLength;
        }
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

    private static int getStationNum(String name, TRP_Station[] Stations) {
        if (Stations == null) return -1;
        if (name.isEmpty()) return -1;
        int sz = Stations.length;
        for (int i = 0; i < sz; i++)
            if (Stations[i].name.equals(name)) return i;
        return -1;
    }

    public int getStationNum(String name) {
        return getStationNum(name, Stations);
    }
}  // class TRP_line
