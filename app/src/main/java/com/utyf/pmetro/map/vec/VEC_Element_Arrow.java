package com.utyf.pmetro.map.vec;

import android.graphics.PointF;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

class VEC_Element_Arrow  extends VEC_Element_Line {

    VEC_Element_Arrow(String param, VEC vv) {
        super(param, vv);
        double   angle, cx,cy,x1,y1,x2,y2, hypo, hypo2;
        PointF   pnt1 = pnts[pnts.length-1], pnt2 = pnts[pnts.length-2];

        cx = pnt2.x - pnt1.x;
        cy = pnt2.y - pnt1.y;
        hypo  = Math.sqrt( cx*cx+cy*cy );
        hypo2 = hypo * 0.25;
        hypo2 = Math.min( hypo2, Width*10/v.scale );

        angle = Math.acos( cx/hypo );
        if( pnt1.y>pnt2.y ) angle = -1 * angle;

        x1 = pnt1.x + hypo2 * Math.cos(angle-(15*Math.PI/180));
        y1 = pnt1.y + hypo2 * Math.sin(angle-(15*Math.PI/180));
        x2 = pnt1.x + hypo2 * Math.cos(angle+(15*Math.PI/180));
        y2 = pnt1.y + hypo2 * Math.sin(angle+(15*Math.PI/180));

        path.moveTo((float)x1,(float)y1);
        path.lineTo(  pnt1.x,  pnt1.y );
        path.lineTo((float)x2,(float)y2);
    }
}
