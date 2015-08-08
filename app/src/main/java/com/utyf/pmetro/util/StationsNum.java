package com.utyf.pmetro.util;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

public class StationsNum {
    public int trp, line, stn;

    public StationsNum(int t, int ln, int st) {
        trp = t;
        line = ln;
        stn = st;
    }

    public boolean isEqual( StationsNum st ) {
        return  st.trp==trp && st.line==line && st.stn==stn ;
    }

    public boolean isEqual( int t, int l, int s ) {
        return  t==trp && l==line && s==stn ;
    }
}
