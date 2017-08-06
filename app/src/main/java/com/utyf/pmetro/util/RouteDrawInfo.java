package com.utyf.pmetro.util;

import android.os.Parcel;
import android.os.Parcelable;

class RouteDrawInfo implements Parcelable {
    int[] stationColors;
    ConnectionType[] connectionTypes;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeIntArray(stationColors);
        out.writeInt(connectionTypes.length);
        for (ConnectionType type : connectionTypes)
            out.writeInt(type.ordinal());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel in) {
            RouteDrawInfo info = new RouteDrawInfo();
            info.stationColors = in.createIntArray();
            int connectionTypesSize = in.readInt();
            info.connectionTypes = new ConnectionType[connectionTypesSize];
            for (int i = 0; i < connectionTypesSize; ++i) {
                int ordinal = in.readInt();
                info.connectionTypes[i] = ConnectionType.values()[ordinal];
            }
            return info;
        }

        @Override
        public Object[] newArray(int size) {
            return new Object[size];
        }
    };
}
