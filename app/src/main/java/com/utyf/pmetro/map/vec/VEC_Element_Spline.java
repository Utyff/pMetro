package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtPath;


public class VEC_Element_Spline extends VEC_Element {
    float     Width;
    PointF[]  pnts;
    ExtPath   path;

    public VEC_Element_Spline(String param, VEC vv) {
        super(vv);
        int i,j;

        String[] strs=param.split(",");
        pnts = new PointF[(strs.length)/2];

        j=0;
        path = new ExtPath();
        for( i=0; i<strs.length-1; i++ ) {
            pnts[j] = new PointF(ExtFloat.parseFloat(strs[i]) * v.scale, ExtFloat.parseFloat(strs[++i]) * v.scale);
            j++;
        }

        if( (strs.length)%2 == 0 )   Width = 1;
        else  Width = ExtFloat.parseFloat(strs[i])*v.scale;

        path.Spline(pnts);
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        float wd;
        Paint.Style ps;
        Paint.Cap   pc;

        wd = p.getStrokeWidth();
        ps = p.getStyle();
        pc = p.getStrokeCap();
        p.setStrokeWidth(Width);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);

        canvas.drawPath(path, p);

        p.setStrokeCap(pc);
        p.setStrokeWidth(wd);
        p.setStyle(ps);
    }
}
