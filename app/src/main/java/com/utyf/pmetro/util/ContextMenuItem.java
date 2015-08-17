package com.utyf.pmetro.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Created by Utyf on 17.08.2015.
 *
 */

public class ContextMenuItem {

    Drawable drawable;
    String text;

    public ContextMenuItem(int color, String text) {
        super();
        this.drawable = new CircleDrawable(color);
        this.text = text;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    /*public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    } //*/

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    class CircleDrawable extends Drawable {
        private Paint mPaint;

        public CircleDrawable(int clr) {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xff000000 | clr);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float x = b.width() /2;
            float y = b.height() /2;
            canvas.drawCircle(x, y, (x+y)*0.3f, mPaint);
        }

        /*@Override
        protected boolean onLevelChange(int level) {
            invalidateSelf();
            return true;
        } //*/

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter cf) {}

        @Override
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}