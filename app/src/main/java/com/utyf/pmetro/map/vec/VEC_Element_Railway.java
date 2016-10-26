package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtInteger;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

class VEC_Element_Railway extends VEC_Element {

    private float  RailWidth, SleeperWidth, SleeperStep, Width;
    private Path     path;

    VEC_Element_Railway(String param, VEC vv) {
        super(vv);
        int       i,j;
        PointF[]  pnts;
        PointF    pnt1, pnt2;
        double    angle, cx,cy, dx,dy,dh, hypo;

        String[] strs=param.split(",");
        if( strs.length<7 )  {
            Log.e("VEC_Stairs /30", "Not enough parameters.  <" + param + ">  ");
            return;
        }

        RailWidth = ExtFloat.parseFloat(strs[0])*v.scale;
        SleeperWidth = ExtFloat.parseFloat(strs[1])*v.scale;
        SleeperStep = ExtFloat.parseFloat(strs[2])*v.scale;
        pnts = new PointF[(strs.length-3)/2];
        path = new Path();

        j=0;
        for( i=3; i<strs.length-1; i++ ) {
            pnts[j] = new PointF(ExtFloat.parseFloat(strs[i]) * v.scale, ExtInteger.parseInt(strs[++i]) * v.scale);
            j++;
        }

        if( (strs.length)%2 != 0 )  Width = 1;
        else Width = ExtFloat.parseFloat(strs[i]);

        for( i=1; i<pnts.length; i++ ) {  // todo draw railway
            pnt1 = pnts[i-1];  pnt2 = pnts[i];
            cx = pnt2.x - pnt1.x; cy = pnt2.y - pnt1.y;
            hypo = Math.sqrt( cx*cx+cy*cy );
            angle = Math.acos( cx/hypo );
            if( pnt1.y>pnt2.y ) angle = -1 * angle;

            for( dh=0; dh<=hypo; dh+=4 )  {
                dx = dh*Math.cos(angle);
                dy = dh*Math.sin(angle);
                path.moveTo( pnt1.x+(float)dx, pnt1.y+10+(float)dy );
                path.lineTo( pnt1.x+(float)dx, pnt1.y+(float)dy );
            }
        }
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        if( path==null )  return;

        float wd;
        Paint.Style ps;

        wd = p.getStrokeWidth();
        ps = p.getStyle();
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);

        canvas.drawPath( path, p);

        p.setStyle(ps);
        p.setStrokeWidth(wd);
    }
}
