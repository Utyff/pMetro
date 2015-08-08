package com.utyf.pmetro.util;

import android.util.Log;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class ExtFloat {

    public static float parseFloat(String str) {
        if( str==null ) return 0;
        str = str.trim();
        if( str.isEmpty() ) return 0;
        try {
            return Float.parseFloat( str );
        } catch (NumberFormatException e) {
            Log.e("ExtFloat /19", "Wrong float number - <" + str + "> ");
            return 0;
        }
    }

}
