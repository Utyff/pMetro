package com.utyf.pmetro.util;

import android.graphics.PointF;

/**
 * Created by Utyf on 05.04.2015.
 *
 */


public class ExtPointF extends PointF {
    static public boolean isNull(PointF pnt) {
     return pnt==null || (pnt.x==0 && pnt.y==0);
    }
}
