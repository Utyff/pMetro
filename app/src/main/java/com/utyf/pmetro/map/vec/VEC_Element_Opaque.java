package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.utyf.pmetro.util.ExtInteger;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

class VEC_Element_Opaque extends VEC_Element {

    private int Opaque;

    VEC_Element_Opaque(String param, VEC vv) {
        super(vv);
        Opaque = ExtInteger.parseInt(param);
        Opaque = (int)Math.min( (255f/100f) * (float)Opaque, 255 );
        Opaque = 0x1000000 * Opaque;
    }

    @Override
    public void Draw(Canvas canvas, Paint paint) {
        v.Opaque = Opaque;
        v.currBrushColor=0xffffff;  // set Brush to white
        paint.setColor( Opaque );   // set Pen to black
    }
}
