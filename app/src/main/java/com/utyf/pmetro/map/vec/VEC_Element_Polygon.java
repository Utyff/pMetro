package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Created by Utyf on 27.02.2015.
 *
 */


public class VEC_Element_Polygon extends VEC_Element {
    float    Width;
    PointF[] pnts;
    Path     path;

    public VEC_Element_Polygon(String param, VEC vv) {
        super(vv);
        int i,j;

        String[] strs=param.split(",");
        pnts = new PointF[(strs.length)/2];

        j=0;
        path = new Path();
        //path.setFillType(Path.FillType.WINDING);
        for( i=0; i<strs.length-1; i++ ) {
            pnts[j] = new PointF( ExtFloat.parseFloat(strs[i])*v.scale, ExtFloat.parseFloat(strs[++i])*v.scale ) ;
            if( j==0 ) path.moveTo(pnts[0].x, pnts[0].y);
            else       path.lineTo(pnts[j].x, pnts[j].y);
            j++;
        }
        path.lineTo(pnts[0].x, pnts[0].y); // to close polygon

        if( strs.length%2 == 0 )  Width = 1*v.scale;
        else Width = ExtFloat.parseFloat(strs[i])*v.scale;
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {
        float  wd;
        Paint.Style ps;

        wd = p.getStrokeWidth();
        ps = p.getStyle();

        if( v.currBrushColor!=-1 ) {
            int  clr = p.getColor();
            p.setStyle(Paint.Style.FILL);
            p.setColor(v.currBrushColor + v.Opaque);
            canvas.drawPath(path, p);
            p.setColor( clr );
        }

        p.setStyle( Paint.Style.STROKE );
        p.setStrokeWidth( Width );
        canvas.drawPath( path, p );

        p.setStrokeWidth( wd );
        p.setStyle( ps );
    }
}
