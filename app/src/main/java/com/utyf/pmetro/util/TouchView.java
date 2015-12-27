package com.utyf.pmetro.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.utyf.pmetro.MapActivity;

/**
 * Created by Utyf on 28.02.2015.
 *
 */

public abstract class TouchView extends ScrollView implements View.OnTouchListener {
    private   ScaleGestureDetector mSGD;
    private   GestureDetector mGD;
    protected float   Scale, minScale;
    protected PointF  shift, shiftCache;
    PointF    size, margin;
    int       drawBMP;
    DrawCache cache[]; //, cacheDraw, cacheShow;
    boolean   startDraw, exitDraw;
    Thread    dt;
    Paint     p;
    viewState newState;

    class DrawCache {
        PointF    shift = new PointF();
        float     scale;
        Bitmap    bmp;
    }

    public TouchView(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);

        mSGD = new ScaleGestureDetector(context,new ScaleListener());
        mGD  = new      GestureDetector(context,new GestureListener());
        setOnTouchListener(this);

        cache = null;
        shiftCache = new PointF(0,0);
        shift = new PointF(0,0);
        size = new PointF(0,0);
        margin = new PointF(0,0);
        //setBackgroundColor(0xffffffff);

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(false);
        p.setAntiAlias(false);
        p.setDither(false);
    }

    @Override
    protected void  onSizeChanged (int w, int h, int oldw, int oldh) {
        DrawCache cc[] = cache;
        cache = null;

        if( cc!=null ) {
            if (cc[0].bmp != null) cc[0].bmp.recycle();
            if (cc[1].bmp != null) cc[1].bmp.recycle();
        }

        cc = new DrawCache[2];
        cc[0] = new DrawCache();
        cc[1] = new DrawCache();

        for( int i=0; i<2; i++ ) {
            cc[i].bmp = Bitmap.createBitmap(w*2, h*2, Bitmap.Config.RGB_565);
            if( cc[i].bmp.getWidth()!=w*2 || cc[i].bmp.getHeight()!=h*2 )
                Toast.makeText(MapActivity.mapActivity, "Can`t create cache bitmap - "+cc[i].bmp.getWidth()+" x "+cc[i].bmp.getHeight(), Toast.LENGTH_LONG).show();
        }

        drawBMP = 0;

        if( Scale == 0 ) {  // check for cross maximum shift & scale
            shiftCache.x = w / 2;
            shiftCache.y = h / 2;
        }
        cache = cc;

        startDraw = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        exitDraw = false;
        dt = new Thread(new Runnable() {
            public void run() {  drawThread();  }
        });
        dt.setName("TouchView Draw");
        dt.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        exitDraw = true;

        if( cache!=null )
            for( int i=0; i<2; i++ ) {
                if (cache[i].bmp != null) cache[i].bmp.recycle();
                cache[i].bmp = null;
            }
        cache = null;

        super.onDetachedFromWindow();
    }

    void drawThread() {
        int  n;
        while( !exitDraw ) {
            if( startDraw && cache!=null ) {
                startDraw = false;

                if( drawBMP ==0 )  n=1;
                else               n=0;

                drawBMP(n);

                synchronized (this) { drawBMP = n; }
                postInvalidate();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.w("TouchView /130", "drawThread  exit" );
    }

    public boolean onTouch(View v, MotionEvent ev) {
        mSGD.onTouchEvent(ev);
        mGD.onTouchEvent(ev);
        awakenScrollBars();
        //redraw();
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            upScale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            doubleTap( (ev.getX()-shift.x)/Scale, (ev.getY()-shift.y)/Scale );
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            singleTap( (ev.getX()-shift.x)/Scale, (ev.getY()-shift.y)/Scale );
            return true;
        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            shiftShift(distanceX, distanceY);
            return true;
        }
    }

    protected int computeHorizontalScrollOffset()  {
        return (int)(margin.x*Scale-shift.x);
    }

    protected int computeHorizontalScrollRange()  {
        return (int)((size.x+margin.x*2)*Scale);
    }

    protected int computeVerticalScrollOffset()  {
        return (int)(margin.y*Scale-shift.y);
    }

    protected int computeVerticalScrollRange()  {
        return (int)((size.y+margin.y*2)*Scale);
    }

    public void upScale(float scl, float focusX, float focusY) {
        float oldScale = Scale;
        Scale *= scl;
        Scale = Math.max(minScale, Math.min(Scale, minScale * 15f));  // max scale - 15
        scl = Scale / oldScale;
        shift.x += (focusX -shift.x) - (focusX -shift.x) * scl;
        shift.y += (focusY -shift.y) - (focusY -shift.y) * scl;
        redraw();
    }

    public void shiftShift(float x, float y) {
        shift.x -= x;
        shift.y -= y;

        float wx=getWidth(), wy=getHeight();

        if( (2*margin.x+size.x)*Scale>=wx ) {
            shift.x = Math.min( margin.x*Scale, shift.x );
            shift.x = Math.max( wx - (margin.x+size.x)*Scale, shift.x );
        } else
            shift.x = (wx - size.x*Scale) /2; // keep on centre

        if( (2*margin.y+size.y)*Scale>=wy ) {
            shift.y = Math.min( margin.y*Scale, shift.y );
            shift.y = Math.max( wy - (margin.y+size.y)*Scale, shift.y );
        } else
            shift.y = (wy - size.y*Scale) /2; // keep on centre

        redraw();
    }

    protected abstract void singleTap(float x, float y);

    protected abstract void doubleTap(float x, float y);

    protected abstract PointF getContentSize();

    public void contentChanged(viewState vs) {
        Scale = 0;
        newState = vs;
        redraw();
    }

    @Override
    protected void onDraw(Canvas c) {
        if( Scale==0 ) {              // initial values, fit and centre map to screen
            size = getContentSize();
            if( ExtPointF.isNull(size) ) return;
            margin = new PointF( size.x/10f, size.y/10f );

            float xScale, yScale, wx=getWidth(), wy=getHeight();
            xScale =  wx / size.x;
            yScale =  wy / size.y;
            Scale = Math.min( xScale, yScale ); // choose smallest scale
            minScale = Scale*0.8f;

            shift.x=( wx - size.x * Scale ) /2;
            shift.y=( wy - size.y * Scale ) /2;

            if( newState!=null ) {
                if( !newState._size.equals(size.x,size.y) ) Log.e("TouchView /237","Wrong new state.");
                else {
                    Scale = newState._Scale;
                    minScale = newState._minScale;
                    shift = newState._shift;
                    margin = newState._margin;
                }
                newState=null;
            }
            cache[0].bmp.eraseColor(Color.WHITE);
            cache[1].bmp.eraseColor(Color.WHITE);
            startDraw = true;
        }

        int cs = c.save();
        float scl;
        synchronized (this) {
            if( cache != null ) {
                DrawCache cacheDraw = cache[drawBMP];
                if( cacheDraw.scale!=0 && cacheDraw.bmp!=null ) {
                    scl = Scale / cacheDraw.scale;

                    c.translate(shift.x - (cacheDraw.shift.x + shiftCache.x) * scl,
                            shift.y - (cacheDraw.shift.y + shiftCache.y) * scl);
                    c.scale(scl, scl);

                    c.drawBitmap(cacheDraw.bmp, 0, 0, p);
                }
            }
            //else  Log.i("TouchView /261", "Scale = "+cache[drawBMP].scale + "  BMP = "+cache[drawBMP].bmp );
        }
        c.restoreToCount(cs);
        //Log.i("TouchView /256", "Scale = "+bmpScale[drawBMP] + "  BMP = "+cacheBMP[drawBMP] );
    }

    void drawBMP(int n) {

        cache[n].shift.x = shift.x;
        cache[n].shift.y = shift.y;
        cache[n].scale = Scale;

        cache[n].bmp.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(cache[n].bmp);
        canvas.translate(shift.x+shiftCache.x, shift.y+shiftCache.y); // Math.round
        canvas.scale(Scale, Scale);

        myDraw(canvas);
    }

    public void redraw() {
        startDraw = true;
    }

    protected abstract void myDraw(Canvas c);

    public viewState getState() {
        return new viewState();
    }

    public class viewState {
        public String     name;
        protected float   _Scale, _minScale;
        protected PointF  _shift;
        protected PointF  _size, _margin;
        viewState() {
            _Scale = Scale;
            _minScale = minScale;
            _shift = new PointF(shift.x, shift.y);
            _size = new PointF(size.x, size.y);
            _margin = new PointF(margin.x, margin.y);
        }
    }
}
