package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;


public abstract class VEC_Element {
    VEC v;

    public VEC_Element(VEC vv) {
        v=vv;
    }

    public String SingleTap(float x, float y) {return null;}
    abstract public void Draw(Canvas canvas, Paint paint);
}
