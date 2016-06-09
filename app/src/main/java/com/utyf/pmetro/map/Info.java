package com.utyf.pmetro.map;

import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loads and parses additional text information from .txt files
 *
 * Files of 3dImage type are not supported
 *
 * @author Utyf
 */

public class Info {

    public boolean ready, fail;
    public ArrayList<TXT> txts;

    /**
     * Loads and parses additional text information from .txt files
     *
     * Loading is asynchronous. Result is stored in txts. It is available after ready becomes true.
     * If load fails, fail becomes true and result is not available.
     *
     * @return true
     */
    public boolean load() {
        Thread loadThread;
        ready = fail = false;

        loadThread = new Thread("Map info loading") {
            @Override
            public void run() {
                String[] names = zipMap.getFileList(".txt");
                if( names==null )  fail=true;
                else {
                    txts = new ArrayList<>();

                    TXT  tt;
                    for( String nm : names ) {
                        tt = new TXT();
                        if( tt.load(nm)<0 || !tt.AddToInfo ) continue;
                        if( tt.Type.equals("3dImage") )      continue;  // unsupported type
                        txts.add(tt);
                    }
                    ready = true;
                }
            }
        };
        loadThread.start();

        return true;
    }

    /**
     * Loads and parses additional text information from a single .txt file
     */
    class TXT {
        String name;
        boolean AddToInfo;
        String  Caption, StringToAdd, Type;
        private HashMap<String, LineInfo> linesInfo;

        /**
         * Loads and parses additional text information from a single .txt file
         *
         * @param nm Name of the file
         * @return 0 if loaded successfully, negative value if failed to load
         */
        public int load(String nm) {
            name = nm;

            Parameters parser = new Parameters();
            int i = parser.load(nm);
            if( i!=0 ) return i;

            linesInfo = new HashMap<>();
            boolean optionsLoaded = false;
            for (Section sec: parser.secs) {
                if (sec.name.equals("Options")) {
                    AddToInfo = sec.getParamValue("AddToInfo").equals("1");
                    Caption = sec.getParamValue("Caption");
                    StringToAdd = sec.getParamValue("StringToAdd");
                    Type = sec.getParamValue("Type");
                    optionsLoaded = true;
                }
                else {
                    LineInfo lineInfo = new LineInfo();
                    lineInfo.load(sec);
                    linesInfo.put(sec.name, lineInfo);
                }
            }
            if (!optionsLoaded) return -10;

            return 0;
        }

        public LineInfo getLineInfo(String lineName) {
            return linesInfo.get(lineName);
        }
    }

    class LineInfo {
        private HashMap<String, String> stationsInfo;

        public void load(Section section) {
            stationsInfo = new HashMap<>();
            for (param station: section.params) {
                String stationInfo = station.value;
                if (stationsInfo.containsKey(station.name)) {
                    // Section contains duplicate station names. Concatenate info in this case
                    String oldInfo = stationsInfo.get(station.name);
                    stationInfo = oldInfo + '\n' + stationInfo;
                }
                stationsInfo.put(station.name, stationInfo);
            }
        }

        public String getStationInfo(String stationName) {
            return stationsInfo.get(stationName);
        }
    }
}
