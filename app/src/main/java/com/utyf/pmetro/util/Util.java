package com.utyf.pmetro.util;

import android.graphics.Color;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;

/**
 * Created by adm on 06.03.2015.
 *
 */

public class Util {
    public static String[] split(String str, char separator) {  // todo - work with "" and ''

        if( str==null || str.length()==0 ) return null;

        ArrayList<String> res = new ArrayList<>();
        int i,n;
        int start=0;

        for( i=0; i<str.length(); i++ ) {
            if( str.charAt(i)==separator ) {
                res.add(str.substring(start, i));
                start = i+1;
            }
            if( i==start && str.charAt(i)=='"' ) {
                n = str.indexOf('"', i + 1);
                if (n < 0) {
                    Log.e("Util", "There is not closing symbol - " + str);
                    return null;
                }
                res.add(str.substring(i + 1, n));
                i = n + 1;
                start = i + 1;
                // } else {  i = n + 1; }
            }
            if( start>=str.length() || i>=str.length() ) break;
        }
        if( start<str.length() )
            res.add(str.substring(start, str.length()));

        return res.toArray( new String[res.size()] );
    }

    public static int getDarkColor(int clr) {
        return Color.argb(0xff, Color.red(clr)/2, Color.green(clr)/2, Color.blue(clr)/2 );
    }

    public static String milli2string(long time) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new java.util.Date(time));
    }
}
