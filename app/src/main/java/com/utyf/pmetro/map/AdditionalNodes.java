package com.utyf.pmetro.map;

import android.graphics.PointF;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Loads and parses information about additional nodes from .map files
 *
 * Additional nodes are used to define detailed geometry of metro lines by using polygons or splines
 *
 * @author Utyf
 */

class AdditionalNodes {
    PointF[] pnts;
    int      numSt1, numSt2;
    boolean  Spline;

    public AdditionalNodes(String[] strs) {
        int  i,j;
        String   St1, St2;

        St1 = strs[1].trim(); St2 = strs[2].trim();
        TRP.TRP_line tl = TRP_Collection.getLine(strs[0]);
        if( tl!=null ) {
            numSt1 = tl.getStationNum(St1);
            numSt2 = tl.getStationNum(St2);
        } else  {
            numSt1 = numSt2 = -1;
            Log.e("AddNodes /31","Wrong line name");
            return;
        }

        pnts = new PointF[(strs.length-3)/2];

        j=0;
        for( i=3; i<strs.length-1; i++ )
            pnts[j++] = new PointF( ExtFloat.parseFloat(strs[i]), ExtFloat.parseFloat(strs[++i]) );

        Spline = (strs.length-3)%2!=0 && strs[i].trim().toLowerCase().equals("spline");
    }
}
