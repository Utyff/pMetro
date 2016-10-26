package com.utyf.pmetro.map.vec;


import android.graphics.Canvas;
import android.graphics.Paint;

import com.utyf.pmetro.util.ExtInteger;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

class VEC_Element_BrushColor extends VEC_Element {

    private int Color;

    VEC_Element_BrushColor(String param, VEC vv) {
        super(vv);
        Color = ExtInteger.parseInt(param, 16);
    }

    @Override
    public void Draw(Canvas canvas, Paint paint) {
        v.currBrushColor = Color;
    }
}
