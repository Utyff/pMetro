package com.utyf.pmetro.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by Utyf on 17.08.2015.
 *
 */

public class StationSelectionMenuItem implements Parcelable {
    private final Drawable drawable;
    private int color;
    private String text;

    public StationSelectionMenuItem(int color, String text) {
        this.color = color;
        this.text = text;

        drawable = new CircleDrawable(color);
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(color);
        out.writeString(text);
    }

    public static final Creator<StationSelectionMenuItem> CREATOR = new Creator<StationSelectionMenuItem>() {
        @Override
        public StationSelectionMenuItem createFromParcel(Parcel in) {
            int color = in.readInt();
            String text = in.readString();
            return new StationSelectionMenuItem(color, text);
        }

        @Override
        public StationSelectionMenuItem[] newArray(int size) {
            return new StationSelectionMenuItem[size];
        }
    };

    private class CircleDrawable extends Drawable {
        private final Paint mPaint;

        public CircleDrawable(int color) {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xff000000 | color);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            float x = b.width() /2;
            float y = b.height() /2;
            canvas.drawCircle(x, y, (x+y)*0.3f, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter cf) {}

        @Override
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}