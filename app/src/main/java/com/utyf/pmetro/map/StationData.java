package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class StationData {
    StationsNum     Station;
    public String   lineName, stationName;
    public ArrayList<VEC>      vecs;
    public ArrayList<String>   vecsCap;
    public ArrayList<InfoItem> items;

    public boolean load(StationsNum ls) {
        String   value;
        String[] strs;
        Section  sec;
        VEC      vv;
        InfoItem ii;

        Station = ls;
        lineName = TRP_Collection.getLine(ls.trp,ls.line).name;
        stationName = TRP_Collection.getStationName(ls);

        vecs    = new ArrayList<>();
        vecsCap = new ArrayList<>();
        items   = new ArrayList<>();

        if( MapData.info.fail ) return false;
        while( !MapData.info.ready )
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        for( Info.TXT tt : MapData.info.txts ) {
            sec = tt.getSec(lineName);
            if( sec==null )  continue;
            value = sec.getParamValue(stationName);
            if( value.isEmpty() ) continue;

            if( tt.Type.equals("Image") ) {
                strs = value.split("\\\\n");
                for( String nm : strs ) {
                    vv = new VEC();
                    if( vv.load(nm)<0 ) continue;
                    vecs.add(vv);
                    vecsCap.add(tt.Caption);
                }
                continue;
            }

            if( !tt.Type.isEmpty() ) Log.e("StationData /54",tt.name+" wrong type - "+tt.Type);

            int i;
            for( i=0; i<items.size(); i++ )
                if( items.get(i).caption.equals(tt.Caption) ) break;
            if( i<items.size() ) {
                items.get(i).text = items.get(i).text +"\n\n"+ tt.StringToAdd + value.replaceAll("\\\\n","\n");
            } else {
                ii = new InfoItem();
                ii.caption = tt.Caption;
                ii.text = tt.StringToAdd + value.replaceAll("\\\\n","\n");
                items.add(ii);
            }
        }
        //Log.w("StationData /61","VECs - " + vecs.size()+" Items - "+items.size());
        return true;
    }

    class InfoItem {
        String caption;
        String text;
    }
}
