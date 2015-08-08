package com.utyf.pmetro.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.TouchView;

/**
 * Created by Utyf on 26.02.2015.
 *
 */

@SuppressLint("ViewConstructor")
public class StationSchemaView extends TouchView {

    //StationsNum  numLineStation;
    VEC  vec;
    //StationData  stationData;
    Paint p;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public StationSchemaView(Context context, VEC v) {
        super(context);
        shift = new PointF(0,0);
        Scale = 0;
        setBackgroundColor(0xffffffff);
        vec = v;
        p=new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void singleTap(float x, float y) {
    }

    @Override
    protected void doubleTap(float x, float y) {
    }

    @Override
    protected PointF getContentSize() {
        if( vec==null || vec.Size==null ) return new PointF();
        return vec.Size;
    }

    @Override
    protected void myDraw(Canvas canvas) {
        if( vec==null || vec.Size==null )  {
            p.setColor(0xff500000);
            p.setTextSize(60);
            canvas.drawText("There is not information.", 50,300, p);
            return;
        }

        vec.Draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // View is now detached, and about to be destroyed
        //if( stationData.vecSchema!=null )
        //    stationData.vecSchema.onClose();
    }

}