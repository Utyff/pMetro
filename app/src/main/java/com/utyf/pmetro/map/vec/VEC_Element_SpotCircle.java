package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Created by Utyf on 27.02.2015.
 *
 */


public class VEC_Element_SpotCircle extends VEC_Element {
    float    radius, x,y;
    float    radius2, x2,y2; // not scaled for hit check
    String   action;

    public VEC_Element_SpotCircle(String param, VEC vv) {
        super(vv);

        String[] strs=param.split(",");

        radius2 = ExtFloat.parseFloat(strs[0]);
        x2 = ExtFloat.parseFloat(strs[1]);
        y2 = ExtFloat.parseFloat(strs[2]);
        radius = radius2*v.scale;
        x = x2*v.scale;
        y = y2*v.scale;
        if( strs.length>3 ) action = strs[3];
        else Log.e("VEC_SpotCircle /32", v.name+" has no action - "+param);
    }

    @Override
    public String SingleTap(float xT, float yT) {
        float xx = x2 - xT;
        float yy = y2 - yT;
        if( Math.sqrt(xx*xx+yy*yy) > radius2 ) return null;
        return action;
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        int   clr;
        Paint.Style  ps;

        if( v.currBrushColor==-1 )  return;

        ps = p.getStyle();
        clr = p.getColor();
        p.setStyle( Paint.Style.FILL );
        p.setColor( v.currBrushColor + v.Opaque );

        canvas.drawCircle( x,y,radius, p );

        p.setColor( clr );
        p.setStyle( ps );
    }
}
