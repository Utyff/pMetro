package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

class VEC_Element_Line extends VEC_Element {
    float    Width;
    PointF[] pnts;
    Path     path;
    DashPathEffect dashPathEffect;

    VEC_Element_Line(String param, VEC vv) {
        super(vv);
        int i,j;

        String[] strs=param.split(",");
        pnts = new PointF[(strs.length)/2];

        j=0;
        path = new Path();
        for( i=0; i<strs.length-1; i++ ) {
            pnts[j] = new PointF( ExtFloat.parseFloat(strs[i])*v.scale, ExtFloat.parseFloat(strs[++i])*v.scale );
            if( j==0 ) path.moveTo( pnts[j].x, pnts[j].y );
            else       path.lineTo( pnts[j].x, pnts[j].y );
            j++;
        }
        if( (strs.length)%2 == 0 )  Width = 1*v.scale;
        else Width = ExtFloat.parseFloat(strs[i])*v.scale;
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        float        wd;
        Paint.Style  ps;
        Paint.Cap    pc;
        Paint.Join   pj;

        wd = p.getStrokeWidth();
        ps = p.getStyle();
        pc = p.getStrokeCap();
        pj = p.getStrokeJoin();

        p.setStrokeWidth(Width);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        if( dashPathEffect!=null ) {
            p.setPathEffect( dashPathEffect );
            p.setStrokeCap(Paint.Cap.BUTT);
        }
        else p.setStrokeCap(Paint.Cap.ROUND);

        canvas.drawPath( path, p );

        p.setPathEffect( null );
        p.setStrokeWidth( wd );
        p.setStyle( ps );
        p.setStrokeCap( pc );
        p.setStrokeJoin( pj );
    }
}
