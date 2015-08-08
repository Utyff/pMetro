package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.utyf.pmetro.util.ExtInteger;


public class VEC_Element_SpotRect extends VEC_Element {
    float   x,y,w,h;
    float   x2,y2,w2,h2; // not scaled for hit check
    String  action;

    public VEC_Element_SpotRect(String param, VEC vv) {
        super(vv);

        String[] strs=param.split(",");
        if( strs.length < 5 )  Log.e("VEC_SpotRect /16", "Wrong parameters.  <" + param + ">  ");

        x2 = ExtInteger.parseInt(strs[0]);
        y2 = ExtInteger.parseInt(strs[1]);
        w2 = ExtInteger.parseInt(strs[2]);
        h2 = ExtInteger.parseInt(strs[3]);
        x = x2*v.scale;
        y = y2*v.scale;
        w = w2*v.scale;
        h = h2*v.scale;
        action = strs[4].trim();
    }

    @Override
    public String SingleTap(float xT, float yT) {
        if( xT>x2 && xT<x2+w2 && yT>y2 && yT<y2+h2 ) return action;
        return null;
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

        canvas.drawRect( x,y,x+w,y+h, p );

        p.setColor( clr );
        p.setStyle( ps );
    }
}
