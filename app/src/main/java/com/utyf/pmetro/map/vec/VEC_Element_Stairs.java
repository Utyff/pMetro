package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class VEC_Element_Stairs extends VEC_Element {

    public float  x1,y1, x2,y2, x3,y3;
    Path   path;

    public VEC_Element_Stairs(String param, VEC vv) {
        super(vv);
        double angle, cx,cy, dx,dy,dh, hypo;

        String[] strs=param.split(",");
        if( strs.length!=6 )  {
            Log.e("VEC_Stairs /25", "Wrong parameters number.  <" + param + ">  ");
            return;
        }

        x1 = ExtFloat.parseFloat(strs[0])*v.scale;
        y1 = ExtFloat.parseFloat(strs[1])*v.scale;
        x2 = ExtFloat.parseFloat(strs[2])*v.scale;
        y2 = ExtFloat.parseFloat(strs[3])*v.scale;
        x3 = ExtFloat.parseFloat(strs[4])*v.scale;
        y3 = ExtFloat.parseFloat(strs[5])*v.scale;

        cx = x3 - x1; cy = y3 - y1;
        hypo = Math.sqrt( cx*cx+cy*cy );
        angle = Math.acos( cx/hypo );
        if( y1>y3 ) angle = -1 * angle;

        path = new Path();
        // hypo -= 0.001; // correction
        for( dh=0; dh<=hypo; dh+=4*v.scale )  {
            dx = dh*Math.cos(angle);
            dy = dh*Math.sin(angle);
            path.moveTo( x2+(float)dx, y2+(float)dy );
            path.lineTo( x1+(float)dx, y1+(float)dy );
        }
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        if( path==null )  return;

        float wd;
        Paint.Style ps;

        wd = p.getStrokeWidth();
        ps = p.getStyle();
        p.setStrokeWidth(1*v.scale);
        p.setStyle(Paint.Style.STROKE);

        canvas.drawPath( path, p);

        p.setStyle(ps);
        p.setStrokeWidth(wd);
    }

}
