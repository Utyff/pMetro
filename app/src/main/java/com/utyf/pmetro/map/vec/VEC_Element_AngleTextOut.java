package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.utyf.pmetro.util.ExtFloat;


class VEC_Element_AngleTextOut extends VEC_Element_TextOut {

    private float angle;

    VEC_Element_AngleTextOut(String param, VEC vv) {
        super( param.substring(param.indexOf(',')+1), vv );  // skip first parameter - angle

        String[] strs=param.split(",");
        angle = -1 * ExtFloat.parseFloat(strs[0]);
    }

    @Override
    public void Draw(Canvas canvas, Paint paint) {
        canvas.save();
        canvas.rotate(angle, x,y);

        super.Draw(canvas,paint);

        canvas.restore();
    }
}
