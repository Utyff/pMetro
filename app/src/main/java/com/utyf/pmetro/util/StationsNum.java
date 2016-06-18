package com.utyf.pmetro.util;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

public class StationsNum implements Parcelable {
    public int trp, line, stn;

    public StationsNum(int t, int ln, int st) {
        trp = t;
        line = ln;
        stn = st;
    }

    public boolean isEqual(StationsNum st ) {
        return  st.trp==trp && st.line==line && st.stn==stn ;
    }

    protected StationsNum(Parcel in) {
        trp = in.readInt();
        line = in.readInt();
        stn = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(trp);
        dest.writeInt(line);
        dest.writeInt(stn);
    }

    public static final Creator<StationsNum> CREATOR = new Creator<StationsNum>() {
        @Override
        public StationsNum createFromParcel(Parcel in) {
            return new StationsNum(in);
        }

        @Override
        public StationsNum[] newArray(int size) {
            return new StationsNum[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
