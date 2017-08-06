package com.utyf.pmetro.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.utyf.pmetro.map.MAP_Parameters;

class RouteImage extends Drawable implements Parcelable {
    private final Paint mFillPaint;
    private final Paint mLinePaint;
    private final RouteDrawInfo mDrawInfo;
    private final float mLineWidth;
    private final float mStationRadius;

    private float[] mCoords;
    private float mHeight;

    private RouteImage(RouteDrawInfo drawInfo, float lineWidth, float stationRadius) {
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
        mLinePaint = new Paint(mFillPaint);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mDrawInfo = drawInfo;
        mLineWidth = lineWidth;
        mStationRadius = stationRadius;
        mHeight = mStationRadius * 4;

        mCoords = new float[drawInfo.stationColors.length];
        mCoords[0] = 0;
        float transferLength = mStationRadius * 3f;
        float trainLength = mStationRadius * 6f;
        for (int i = 1; i < mCoords.length; i++) {
            if (drawInfo.connectionTypes[i - 1] == ConnectionType.TRANSFER)
                mCoords[i] = mCoords[i - 1] + transferLength;
            else
                mCoords[i] = mCoords[i - 1] + trainLength;
        }
    }

    public static RouteImage createRouteImage(RouteDrawInfo drawInfo, MAP_Parameters mapParameters) {
        return new RouteImage(drawInfo, mapParameters.LinesWidth, mapParameters.StationRadius);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        float scale = getBounds().height() / mHeight;
        canvas.scale(scale, scale);

        drawLines(canvas);
        drawYellowStations(canvas);
        drawTransfers(canvas);
        drawStations(canvas);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private void drawLines(Canvas canvas) {
        mLinePaint.setStrokeWidth(mLineWidth);
        float y = mHeight / 2;
        float x = mHeight / 2;
        for (int i = 0; i < mDrawInfo.connectionTypes.length; i++) {
            if (mDrawInfo.connectionTypes[i] == ConnectionType.TRAIN) {
                mLinePaint.setColor(mDrawInfo.stationColors[i]);  // line has the same color as station
                canvas.drawLine(x + mCoords[i], y, x + mCoords[i + 1], y, mLinePaint);
            }
        }
    }

    private void drawTransfers(Canvas canvas) {
        float y = mHeight / 2;
        float x = mHeight / 2;

        mFillPaint.setColor(0xff000000);
        mLinePaint.setColor(0xff000000);
        mLinePaint.setStrokeWidth(mLineWidth + 6);
        for (int i = 0; i < mDrawInfo.connectionTypes.length; i++) {   // draw black edging
            if (mDrawInfo.connectionTypes[i] == ConnectionType.TRANSFER) {
                canvas.drawCircle(x + mCoords[i], y, mStationRadius + 3, mFillPaint);
                canvas.drawCircle(x + mCoords[i + 1], y, mStationRadius + 3, mFillPaint);
                canvas.drawLine(x + mCoords[i], y, x + mCoords[i + 1], y, mLinePaint);
            }
        }

        mFillPaint.setColor(0xffffffff);
        mLinePaint.setColor(0xffffffff);
        mLinePaint.setStrokeWidth(mLineWidth + 4);
        for (int i = 0; i < mDrawInfo.connectionTypes.length; i++) {   // draw white transfer
            if (mDrawInfo.connectionTypes[i] == ConnectionType.TRANSFER) {
                canvas.drawCircle(x + mCoords[i], y, mStationRadius + 2, mFillPaint);
                canvas.drawCircle(x + mCoords[i + 1], y, mStationRadius + 2, mFillPaint);
                canvas.drawLine(x + mCoords[i], y, x + mCoords[i + 1], y, mLinePaint);
            }
        }
    }

    private void drawStations(Canvas canvas) {
        float y = mHeight / 2;
        float x = mHeight / 2;
        for (int i = 0; i < mDrawInfo.stationColors.length; i++) {
            mFillPaint.setColor(mDrawInfo.stationColors[i]);
            canvas.drawCircle(x + mCoords[i], y, mStationRadius, mFillPaint);
        }
    }

    private void drawYellowStations(Canvas canvas) {
        mFillPaint.setColor(0xffffff00);
        float y = mHeight / 2;
        float x = mHeight / 2;
        for (int i = 0; i < mDrawInfo.stationColors.length; i++) {
            canvas.drawCircle(x + mCoords[i], y, mStationRadius + 1, mFillPaint);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mDrawInfo, flags);
        out.writeFloat(mLineWidth);
        out.writeFloat(mStationRadius);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            RouteDrawInfo drawInfo = in.readParcelable(RouteDrawInfo.class.getClassLoader());
            float lineWidth = in.readFloat();
            float stationRadius = in.readFloat();
            return new RouteImage(drawInfo, lineWidth, stationRadius);
        }

        @Override
        public Object[] newArray(int size) {
            return new Object[size];
        }
    };
}
