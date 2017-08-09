package com.utyf.pmetro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewConfiguration;

import com.utyf.pmetro.map.MapData;
import com.utyf.pmetro.map.routing.RouteInfo;
import com.utyf.pmetro.map.routing.RoutingState;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.TouchView;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

public class Map_View extends TouchView {
    private final MapData mapData;
    //protected int actionBarHeight=230;
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private float  touchRadius;
    private PointF touchPointScr;
    private long   touchTime, showTouchTime=DOUBLE_TAP_TIMEOUT;
    private Paint  touchPaint;
    private Runnable postInvalidateRunnable; // do not create on every onDraw call
    private RoutingState.Listener listener;
    //public static Typeface fontArial;
    //public StationsNum[] menuStns;

    public Map_View(Context context, MapData _mapData) {
        super(context);
        this.mapData = _mapData;

        //TypedValue tv = new TypedValue();   // Calculate ActionBar height
        //if( getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize,tv,true) )
        //    actionBarHeight = TypedValue.complexToDimensionPixelSize( tv.data,getResources().getDisplayMetrics() );

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpi = (metrics.xdpi + metrics.ydpi) /2;
        touchRadius = dpi/6;
        touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        touchPaint.setColor(0x00f0ff);
        touchPaint.setStyle(Paint.Style.FILL);
        postInvalidateRunnable = new Runnable() {
            public void run() {
                postInvalidate(); }
        };

        // TODO: 08.07.2016 Move this call into activity
        mapData.routingState.addListener(listener = new RoutingState.Listener() {
            @Override
            public void onComputingTimesStarted() {
            }

            @Override
            public void onComputingTimesFinished() {
            }

            @Override
            public void onComputingTimesProgress(final StationsNum[] stationNums, final float[] stationTimes) {
                // need to run callback on UI thread
                post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Map_View", "onComputingTimesProgress started");
                        mapData.map.setStationTimes(stationNums, stationTimes);
                        redraw();
                        Log.d("Map_View", "onComputingTimesProgress finished");
                    }
                });
            }

            @Override
            public void onComputingRoutesStarted() {
            }

            @Override
            public void onComputingRoutesFinished(final RouteInfo[] bestRoutes) {
                // need to run callback on UI thread
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (bestRoutes.length == 1) {
                            mapData.map.createRoute(bestRoutes[0]);
                        }
                        else if (bestRoutes.length > 1) {
                            mapData.map.clearRoute();
                            MapActivity.mapActivity.showRouteSelectionMenu(bestRoutes);
                        }
                        else {
                            mapData.map.clearRoute();
                        }
                        redraw();
                    }
                });
            }

            @Override
            public void onRouteSelected(final RouteInfo route) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        mapData.map.createRoute(route);
                        redraw();
                    }
                });
            }

            @Override
            public void onBlockedStationsChanged() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        redraw();
                    }
                });
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // TODO: 08.07.2016 Move this call into activity
        mapData.routingState.removeListener(listener);
    }

    private void processTap(float x, float y, boolean isLongTap) {
        PointF touchPointMap = new PointF(x, y);
        touchPointScr = new PointF(x*getScale()+shift.x,y*getScale()+shift.y);
        touchTime = System.currentTimeMillis();
        mapData.singleTap(touchPointMap.x, touchPointMap.y, (int)(touchRadius/getScale()), isLongTap);
        redraw();
    }

    @Override
    protected void singleTap(float x, float y) {
        processTap(x, y, false);
    }

    private boolean aBarShow = true;
    @Override
    protected void doubleTap(float x, float y) {
        ActionBar actionBar = MapActivity.mapActivity.getSupportActionBar();
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
    protected void longPress(float x, float y) {
        processTap(x, y, true);
    }

    public void selectStation(StationsNum stn, boolean isLongTap) {
        if (isLongTap) {
            MapActivity.mapActivity.stationContextMenu(stn);
        }
        else {
            if (!mapData.routingState.isRouteStartSelected())
                mapData.routingState.setStart(stn);
            else
                mapData.routingState.setEnd(stn);
        }
    }

    @Override
    protected PointF getContentSize()  {
        return mapData.map.getSize();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        if( touchTime!=0 ) {
            long ll = System.currentTimeMillis() - touchTime; // draw touch circle
            if (ll < showTouchTime) {
//            if (false) {
                int alpha;
                if (ll < showTouchTime/2)
                    alpha = 0x10 + ((int) ll * 0x80) / ((int) showTouchTime/2);
                else
                    alpha = 0x10 + (((int) (showTouchTime - ll) * 0x80) / ((int) showTouchTime/2));
                touchPaint.setAlpha(alpha);
                c.drawCircle(touchPointScr.x, touchPointScr.y, touchRadius, touchPaint);
                postDelayed(postInvalidateRunnable, 10); //showTouchTime - ll);
            } else {
                touchTime = 0;
                // TODO: 23.06.2016 Remove redraw call, use callback instead
                redraw();
                //Log.e("Map_View", "touch point1 - " + touchPointMap.toString());
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
            for( int n=0; n<TRP.getSize(); n++ ) str = str+ " "+TRP.getTRP(n).type;
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
        mapData.draw(canvas);
    }
}
