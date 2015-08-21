package com.utyf.pmetro.map;

import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;

/**
 * Created by Utyf on 12.05.2015.
 *
 */

public class Info {

    public boolean ready, fail;
    public ArrayList<TXT> txts;

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

    class TXT extends Parameters {
        boolean AddToInfo;
        String  Caption, StringToAdd, Type;

        public int load(String nm) {
            Section sec;

            int i = super.load(nm);
            if( i!=0 ) return i;

            sec = getSec("Options");
            if( sec==null ) return -10;
            AddToInfo = sec.getParamValue("AddToInfo").equals("1");
            Caption = sec.getParamValue("Caption");
            StringToAdd = sec.getParamValue("StringToAdd");
            Type = sec.getParamValue("Type");

            return 0;
        }
    }
}
