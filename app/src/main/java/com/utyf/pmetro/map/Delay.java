package com.utyf.pmetro.map;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Utyf on 28.03.2015.
 *
 */

public class Delay {
    private static ArrayList<String> Names;
    private static int  type=0;

    static void setNames(String param) {
        Names = new ArrayList<>( Arrays.asList(param.split(",")) );
        //MapActivity.mapActivity.setDelays();
    }

    public static String getName(int i) {
        return Names.get(i);
    }

    public static int getSize() {
        if( Names==null ) return 0;
        return Names.size();
    }

    public static void setType(int t) {
        type = t;
    }

    private final float  delays[];

    public Delay() {
        delays = new float[Names.size()];
        for( int i=0; i<delays.length; i++ )   delays[i] = 0;
    }

    public Delay(String param) {
        String[] strs = param.split(",");
        if( Names.size()!=strs.length )
            Log.e("Delay /43","Delays not the same as DelayNames.");

        delays = new float[strs.length];

        for( int i=0; i<delays.length; i++ )
            delays[i] = TRP.String2Time(strs[i]);
    }

    public Delay(float day, float night) {
        if( Names.size()!=2 )
            Log.e("Delay /53","Delays mixed style.");

        delays = new float[2];
        delays[0] = day;
        delays[1] = night;
    }

    public float get() {
        if( type==-1 ) return 0;
        return delays[type];
    }
}
