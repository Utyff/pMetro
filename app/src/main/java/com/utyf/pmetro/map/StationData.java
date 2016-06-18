package com.utyf.pmetro.map;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class StationData implements Parcelable {
    StationsNum station;
    public String lineName, stationName;
    public ArrayList<String> vecsData;
    public ArrayList<String> vecsCap;
    public ArrayList<InfoItem> items;

    public StationData() {
    }

    public boolean load(StationsNum ls, MapData mapData) {
        String   value;
        String[] strs;
        InfoItem ii;

        station = ls;
        lineName = mapData.transports.getLine(ls.trp,ls.line).name;
        stationName = mapData.transports.getStationName(ls);

        vecsData = new ArrayList<>();
        vecsCap = new ArrayList<>();
        items = new ArrayList<>();

        if( mapData.info.fail ) return false;
        while( !mapData.info.ready )
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        for( Info.TXT tt : mapData.info.txts ) {
            Info.LineInfo lineInfo = tt.getLineInfo(lineName);
            if (lineInfo == null) {
                continue;
            }
            value = lineInfo.getStationInfo(stationName);
            if (value == null || value.isEmpty()) {
                continue;
            }

            if( tt.Type.equals("Image") ) {
                strs = value.split("\\\\n");
                for( String nm : strs ) {
                    vecsData.add(nm);
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

    protected StationData(Parcel in) {
        station = in.readParcelable(StationsNum.class.getClassLoader());
        lineName = in.readString();
        stationName = in.readString();
        vecsData = in.createStringArrayList();
        vecsCap = in.createStringArrayList();
        items = in.createTypedArrayList(InfoItem.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(station, flags);
        dest.writeString(lineName);
        dest.writeString(stationName);
        dest.writeStringArray(vecsData.toArray(new String[vecsData.size()]));
        dest.writeStringArray(vecsCap.toArray(new String[vecsData.size()]));
        dest.writeTypedList(items);
    }

    public static final Creator<StationData> CREATOR = new Creator<StationData>() {
        @Override
        public StationData createFromParcel(Parcel in) {
            return new StationData(in);
        }

        @Override
        public StationData[] newArray(int size) {
            return new StationData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    static class InfoItem implements Parcelable {
        String caption;
        String text;

        public InfoItem() {}

        protected InfoItem(Parcel in) {
            caption = in.readString();
            text = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(caption);
            dest.writeString(text);
        }

        public static final Creator<InfoItem> CREATOR = new Creator<InfoItem>() {
            @Override
            public InfoItem createFromParcel(Parcel in) {
                return new InfoItem(in);
            }

            @Override
            public InfoItem[] newArray(int size) {
                return new InfoItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
