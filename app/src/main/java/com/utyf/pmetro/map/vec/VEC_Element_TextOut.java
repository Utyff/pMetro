package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtInteger;


public class VEC_Element_TextOut extends VEC_Element {

    int      style=0, s1=0;
    float    size, x,y;
    boolean  underline;
    String   text;
    // String face;  // todo

    public VEC_Element_TextOut(String param, VEC vv) {
        super(vv);

        String[] strs=param.split(",");

        //face = strs[0].trim();

        size = ExtFloat.parseFloat(strs[1])*v.scale*0.9f; // 0.9 - font width correction. Diff PC version
        x    = ExtFloat.parseFloat(strs[2])*v.scale;
        y    = ExtFloat.parseFloat(strs[3])*v.scale;
        if( strs.length > 5 ) {
            s1 = ExtInteger.parseInt(strs[5]);
            if( (s1 & 1) == 1 )  style |= Typeface.BOLD;
            if( (s1 & 2) == 2 )  style |= Typeface.ITALIC;
            if( (s1 & 4) == 4 )  underline = true;
            //if( (s1 & 8) == 8 )  style |= Typeface.; //  todo zacherk
        }

        text = strs[4].trim();  // todo - make multiline, work with "" ''
    }

    @Override
    public void Draw(Canvas canvas, Paint paint) {

        Typeface tf = paint.getTypeface();
        paint.setUnderlineText(underline);
        paint.setTypeface( Typeface.create(tf, style) );
        paint.setTextSize(size);
        paint.setStyle(Paint.Style.FILL);

        if( v.currBrushColor!=-1 ) {  // draw background rect
            int clr = paint.getColor();
            paint.setColor(v.currBrushColor + v.Opaque);
            canvas.drawRect(x-1, y, x+paint.measureText(text)+1, y-paint.ascent()+paint.descent()/2, paint);
            paint.setColor( clr );
        }

        canvas.drawText( text, x,y-paint.ascent(), paint );

        paint.setStyle(Paint.Style.STROKE);
        paint.setUnderlineText(false);
        paint.setTypeface(tf);
    }
}
