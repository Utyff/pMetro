package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.Util;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Loads and parses information about transports from *.trp files
 *
 * @author Utyf
 */
public class TRP {
    // TODO: 25.04.2016  Make private
    Transfer[] transfers;
    TRP_line[] lines;
    String Type;

    private String name;

    public String getType() {
        return Type;
    }

    static float String2Time(String t) {
        t = t.trim();
        if (t.isEmpty()) return -1;

        int i = t.indexOf('.');

        try {
            if (i == -1) return Integer.parseInt(t);
            else
                return (float) Integer.parseInt(t.substring(0, i)) + (float) Integer.parseInt(t.substring(i + 1)) / 60;
        } catch (NumberFormatException e) {
            Log.e("TRP /354", "TRP Driving fork wrong time - <" + t + "> ");
            return -1;
        }
    }

    public String getName() {
        return name;
    }

    public class TRP_Driving {
        public int frwStNum = -1, bckStNum = -1;
        String frwST, bckST;
        public float frwDR = -1, bckDR = -1;

        TRP_Driving(String n1, String n2) {
            frwST = n1;
            bckST = n2;
            if (frwST.length() > 1 && frwST.charAt(0) == '-') frwST = frwST.substring(1);
            if (bckST.length() > 1 && bckST.charAt(0) == '-') bckST = bckST.substring(1);
        }

        boolean setTimes(String t1, String t2) {
            frwDR = String2Time(t1);
            bckDR = String2Time(t2);
            return frwDR > 0 || bckDR > 0;  // true if station is working
        }
    }

    public class TRP_Station {
        public String name;  //, alias;  // todo
        boolean isWorking;
        LinkedList<TRP_Driving> drivings;

        private void addDriving(String names) {

            String[] strs = Util.split(names, ',');
            if (drivings == null) drivings = new LinkedList<>();

            for (int i = 0; i < strs.length; i += 2)
                if (i + 1 >= strs.length)   // is there last name?
                    drivings.add(new TRP_Driving(strs[i].trim(), ""));
                else
                    drivings.add(new TRP_Driving(strs[i].trim(), strs[i + 1].trim()));
        }

        private void addDrivingEmpty() {
            if (drivings != null) {
                Log.e("TRP /354", "Driving not empty.");
                return;
            }
            drivings = new LinkedList<>();
            drivings.add(new TRP_Driving("-", "-"));
        }

        private void setDrivingTime(String times) {

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

    public class TRP_line {

        Delay delays;
        public String name, alias, LineMap, Aliases;
        TRP_Station[] Stations;

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

                    st.drivings.get(0).frwDR = String2Time(drv.substring(0, i));
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

    public class Transfer {
        public int trp1num = -1, line1num = -1, st1num = -1, trp2num = -1, line2num = -1, st2num = -1;
        String line1, st1, line2, st2;
        public float time;
        public boolean invisible = false;
        boolean isWrong;

        public Transfer(String str) {
            String[] strs = str.split(",");
            if (strs.length < 4) {
                Log.e("TRP /595", "TRP - " + name + "  Bad transfer parameters - " + str);
                return;
            }

            line1 = strs[0].trim();
            st1 = strs[1].trim();
            line2 = strs[2].trim();
            st2 = strs[3].trim();
            if (strs.length > 4) {
                time = String2Time(strs[4]);
                if (strs.length > 5 && strs[5].trim().toLowerCase().equals("invisible"))
                    invisible = true;
            }
        }

        public void setNums(TRP_Collection transports) {  // call this method after all TRPs are loaded for set stations numbers
            StationsNum sn1 = transports.getLineNum(line1);
            StationsNum sn2 = transports.getLineNum(line2);
            if (sn1 == null || sn2 == null) {
                Log.e("TRP /609", "Wrong transfer Line name - " + line1 + " " + line2);
                isWrong = true;
                return;
            }

            trp1num = sn1.trp;
            trp2num = sn2.trp;
            line1num = sn1.line;
            line2num = sn2.line;
            //noinspection ConstantConditions
            st1num = transports.getTRP(trp1num).getLine(line1num).getStationNum(st1);
            //noinspection ConstantConditions
            st2num = transports.getTRP(trp2num).getLine(line2num).getStationNum(st2);
            if (trp1num != trp2num) invisible = true;
            if (trp1num == -1 || line1num == -1 || st1num == -1 || trp2num == -1 || line2num == -1 || st2num == -1)
                isWrong = true;
        }

        public boolean isCorrect() {
            return !isWrong;
        }
    }

    public int load(String name) {
        this.name = name;
        Parameters parser = new Parameters();

        if (parser.load(name) < 0) return -1;
        // parsing TRP file
        int i;
        TRP.TRP_line ll;

        if (parser.getSec("Options") != null)
            Type = parser.getSec("Options").getParamValue("Type");
        else
            Type = name.substring(0, name.lastIndexOf("."));

        ArrayList<TRP.TRP_line> la = new ArrayList<>();
        for (i = 0; i < parser.secsNum(); i++) {
            if (parser.getSec(i).name.equals("Options")) continue;
            if (!parser.getSec(i).name.startsWith("Line")) break;
            ll = new TRP.TRP_line();
            ll.Load(parser.getSec(i));
            la.add(ll);
        }
        lines = la.toArray(new TRP.TRP_line[la.size()]);

        Section sec = parser.getSec("Transfers"); // load transfers
        ArrayList<TRP.Transfer> ta = new ArrayList<>();
        if (sec != null)
            for (i = 0; i < sec.ParamsNum(); i++)
                ta.add(new TRP.Transfer(sec.getParam(i).value));  // sec.getParam(i).name,
        transfers = ta.toArray(new TRP.Transfer[ta.size()]);

        //  = getSec("AdditionalInfo");  todo
        return 0;
    }

    public TRP_line getLine(int ln) {
        if (lines == null || lines.length < ln || ln < 0) return null;
        return lines[ln];
    }
}


