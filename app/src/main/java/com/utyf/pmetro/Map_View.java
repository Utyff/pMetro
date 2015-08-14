package com.utyf.pmetro;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.util.TouchView;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

public class Map_View extends TouchView {

    String notLoaded, loadingMap;
    float fontSize;
    int   xCentre,yCentre, ww, hh;
    private Rect  rectBar;
    private GradientDrawable bar;
    private Paint blackPaint;
    protected int actionBarHeight=230;
    float  dpi, touchRadius;
    PointF touchPoint;
    long   touchTime, showTouchTime=200;
    Paint  touchPaint;
    //public static Typeface fontArial;

    public Map_View(Context context) {
        super(context);

        loadingMap = "Loading map..";
        notLoaded  = "Map not loaded.";

        TypedValue tv = new TypedValue();   // Calculate ActionBar height
        if( getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize,tv,true) )
            actionBarHeight = TypedValue.complexToDimensionPixelSize( tv.data,getResources().getDisplayMetrics() );

        blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackPaint.setColor(Color.BLACK);
        //fontArial = Typeface.createFromAsset(MapActivity.asset, "arial.ttf");
        //view.setTypeface(fontArial);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dpi = (metrics.xdpi + metrics.ydpi) /2;
        touchRadius = dpi/6;
        touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        touchPaint.setColor(0x00f0ff);
        touchPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void  onSizeChanged (int w, int h, int oldw, int oldh) {
        xCentre = w/2;  yCentre = h/2;  ww=w; hh=h;
        fontSize = 0;
        do
            blackPaint.setTextSize(++fontSize);
        while( blackPaint.measureText(notLoaded)<w/2.5f );

        int[] colors = new int[3];
        colors[0] = 0xffffffff;
        colors[1] = 0xffdfdfff;
        colors[2] = 0xff0000ff;
        bar = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        bar.setBounds(0, yCentre+h/20, xCentre, yCentre+h/20+15);
        rectBar = new Rect(w/4, yCentre+h/20, w/4*3, yCentre+h/20+15);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void singleTap(float x, float y) {
        touchPoint = new PointF(x*Scale+shift.x,y*Scale+shift.y);
        touchTime = System.currentTimeMillis();
        //Log.e("ViewTouch","point - " + touchPoint.toString() + "; time - " + touchTime);
        MapData.singleTap(x,y);
        redraw();
    }

    private boolean aBarShow = true;
    @Override
    protected void doubleTap(float x, float y) {
        if( MapData.isReady )
            if( MapData.map.doubleTap(x,y) ) return;

        ActionBar actionBar = MapActivity.mapActivity.getActionBar();
        if( actionBar!=null) {
            if( aBarShow ) {
                aBarShow = false;
                actionBar.hide();
            } else {
                aBarShow = true;
                actionBar.show();
            }
        }
    }

    @Override
    protected PointF getContentSize()  {
        if( !MapData.isReady ) return new PointF(0,0);
        return MapData.map.getSize();
    }

    @Override
    protected void onDraw(Canvas c) {
        if( MapData.loading )  {
            blackPaint.setTextSize(fontSize);
            blackPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(loadingMap, xCentre, yCentre, blackPaint);

            new Handler().postDelayed(new Runnable() {
                public void run() { postInvalidate(); }
            }, 100);

            c.clipRect(rectBar, Region.Op.REPLACE);
            int tm = (int)(System.currentTimeMillis()%2000);
            int sh = xCentre*tm/2000-xCentre/2;
            c.translate(sh,0);
            bar.draw(c);
            c.translate(xCentre,0);
            bar.draw(c);

            return;
        }
        if( !MapData.isReady )  {
            blackPaint.setTextSize(fontSize);
            blackPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(notLoaded, xCentre, yCentre, blackPaint);
            return;
        }

        super.onDraw(c);

        if( touchTime!=0 ) {
            long ll = System.currentTimeMillis() - touchTime; // draw touch circle
            if (ll < showTouchTime) {
                int OP;
                if (ll < showTouchTime/2)
                    OP = 0x30 + ((int) ll * 0x60) / ((int) showTouchTime/2);
                else
                    OP = 0x30 + (((int) (showTouchTime - ll) * 0x60) / ((int) showTouchTime/2));
                touchPaint.setAlpha(OP);
                c.drawCircle(touchPoint.x, touchPoint.y, touchRadius, touchPaint);
                new Handler().postDelayed(new Runnable() {
                    public void run() { postInvalidate(); }
                }, 30); //showTouchTime - ll);
            } else {
                touchTime = 0;
                /* rise tap event */
            }
        }

        /*if( MapActivity.debugMode ) {
            int i=0;
            blackPaint.setColor(0xff009090);
            blackPaint.setTextSize(16);
            canvas.drawText("View accel - "+isHardwareAccelerated(), 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Canvas accel - "+canvas.isHardwareAccelerated(), 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Draw time - " +(System.currentTimeMillis()-tm), 30, (++i)*30+actionBarHeight, blackPaint);
            String str = "trp files:";
            for( int n=0; n<TRP.getSize(); n++ ) str = str+ " "+TRP.getTRP(n).Type;
            canvas.drawText(str, 30, (++i)*30+actionBarHeight, blackPaint);
            str = "allowed trp:";
            for( int n=0; n<TRP.getSize(); n++ ) str = str+ " "+TRP.isAllowed(n);
            canvas.drawText(str, 30, (++i)*30+actionBarHeight, blackPaint);
            str = "active trp:";
            for( int n=0; n<TRP.getSize(); n++ ) str = str+ " "+TRP.isActive(n);
            canvas.drawText(str, 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Calc. time: "+MapActivity.calcTime, 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Calc. back time: "+MapActivity.calcBTime, 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Make route time: "+MapActivity.makeRouteTime, 30, (++i)*30+actionBarHeight, blackPaint);
            canvas.drawText("Route time: "+TRP.bestTime, 30, (++i)*30+actionBarHeight, blackPaint);
        } //*/
    }

    @Override
    protected void myDraw(Canvas canvas) {
        if( !MapData.isReady ) return;
        MapData.draw(canvas);
    }
}
