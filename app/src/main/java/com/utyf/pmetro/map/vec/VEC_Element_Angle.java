package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.utyf.pmetro.util.ExtFloat;

/**
 * Created by Utyf on 27.02.2015.
 *
 */


public class VEC_Element_Angle extends VEC_Element {

    float Angle;

    public VEC_Element_Angle(String param, VEC vv) {
        super(vv);
        Angle = -1 * ExtFloat.parseFloat(param);
    }

    @Override
    public void Draw(Canvas canvas, Paint paint) {
        canvas.rotate( Angle, v.Size.x/2, v.Size.y/2 );
    }
}
