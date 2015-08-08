package com.utyf.pmetro.util;

import android.util.Log;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class ExtInteger {

    public static int parseInt( String str ) {
        return parseInt(str,10);
    }

    public static int parseInt( String str, int radix )  {
        int i;

        if( str==null ) return 0;
        str = str.trim();
        if( str.length()==0 ) return 0;
        if( (i=str.indexOf('.'))!=-1 )  {
            Log.e("ExtInteger /23", "Float instead integer - <" + str + "> ");
            str = str.substring(0,i);
        }
        try {
            return Integer.parseInt( str, radix );
        } catch( NumberFormatException e ) {
            Log.e("ExtInteger /29", "Wrong integer number - <" + str + "> ");
            return 0;
        }
    }
}
