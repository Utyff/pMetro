package com.utyf.pmetro.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.settings.SET;

/**
 * Created by Utyf on 28.02.2015.
 *
 */

public abstract class TouchView extends ScrollView implements View.OnTouchListener {
    private   ScaleGestureDetector mSGD;
    private   GestureDetector mGD;
    private float scale;
    protected float minScale;
    protected PointF  shift, bufferShift; // shift by user,  shift for buffers padding
    PointF    size, margin;              // content size,  margin for nice look
    int visibleBufferIndex;
    DrawBuffer buffers[]; //, cacheDraw, cacheShow;
    DrawThread drawThread;
    Paint     p;
    viewState newState;
    final int maxTexture = 4096;
    Point     cacheSize = new Point();

    private static class DrawThread extends HandlerThread {
        private Handler handler;

        public DrawThread() {
            super("TouchView draw thread");
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            handler = new Handler(getLooper());
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            Log.w("TouchView /130", "drawThread  exit" );
        }

        public void doWork(Runnable r) {
            if (!handler.hasMessages(0)) {  // do not post Runnable if there are tasks in the queue
                handler.post(r);
            }
        }
    }

    class DrawBuffer {
        PointF    shift = new PointF();
        float     scale;
        Bitmap    bmp;
        DrawBuffer(Point sz) {
            bmp = Bitmap.createBitmap(sz.x, sz.y, Bitmap.Config.RGB_565);
            if( bmp.getWidth()!=sz.x || bmp.getHeight()!=sz.y )
                Toast.makeText(MapActivity.mapActivity, "Can`t create buffer bitmap - "+bmp.getWidth()+" x "+bmp.getHeight(), Toast.LENGTH_LONG).show();
        }
    }

    public TouchView(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);

        mSGD = new ScaleGestureDetector(context,new ScaleListener());
        mGD  = new      GestureDetector(context,new GestureListener());
        setOnTouchListener(this);

        buffers = null;
        bufferShift = new PointF(0,0);
        shift = new PointF(0,0);
        size = new PointF(0,0);
        margin = new PointF(0,0);
        setBackgroundColor(0xffffffff);

        p = new Paint();
        p.setFilterBitmap(false);
        p.setAntiAlias(false);
        p.setDither(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        DrawBuffer cc[] = buffers;
        buffers = null;

        cacheSize.x =  w * 2;  if( cacheSize.x>maxTexture ) cacheSize.x = maxTexture;
        cacheSize.y =  h * 2;  if( cacheSize.y>maxTexture ) cacheSize.y = maxTexture;
        bufferShift.x = (cacheSize.x - w) / 2;
        bufferShift.y = (cacheSize.y - h) / 2;

        if( cc!=null ) {
            if (cc[0].bmp != null) cc[0].bmp.recycle();
            if (cc[1].bmp != null) cc[1].bmp.recycle();
        }

        cc = new DrawBuffer[2];
        cc[0] = new DrawBuffer(cacheSize);
        cc[1] = new DrawBuffer(cacheSize);

        if( SET.hw_acceleration )
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        visibleBufferIndex = 0;
        buffers = cc;
        resetScale();
        startDraw();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        drawThread = new DrawThread();
        drawThread.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        exitDraw();

        if( buffers !=null )
            for( int i=0; i<2; i++ ) {
                if (buffers[i].bmp != null) buffers[i].bmp.recycle();
                buffers[i].bmp = null;
            }
        buffers = null;

        super.onDetachedFromWindow();
    }

    private void startDraw() {
        if (drawThread == null) {
            return;
        }
        drawThread.doWork(new Runnable() {
            @Override
            public void run() {
                if (buffers == null) {
                    return;
                }

                int hiddenBufferIndex = visibleBufferIndex == 0 ? 1 : 0;

                drawBMP(buffers[hiddenBufferIndex]);
                synchronized (TouchView.this) {
                    visibleBufferIndex = hiddenBufferIndex;
                }
                postInvalidate();
                // This is a hack! It is used to avoid flickering when visible buffer has not yet
                // appeared on the screen, but drawBMP is called from drawThread to reuse it.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void exitDraw() {
        drawThread.quit();
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
            doubleTap( (ev.getX()-shift.x)/ scale, (ev.getY()-shift.y)/ scale);
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            singleTap( (ev.getX()-shift.x)/ scale, (ev.getY()-shift.y)/ scale);
            return true;
        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            shiftShift(distanceX, distanceY);
            return true;
        }
        @Override
        public void onLongPress(MotionEvent ev) {
            longPress( (ev.getX()-shift.x)/ scale, (ev.getY()-shift.y)/ scale);
        }
    }

    protected int computeHorizontalScrollOffset()  {
        return (int)(margin.x* scale -shift.x);
    }

    protected int computeHorizontalScrollRange()  {
        return (int)((size.x+margin.x*2)* scale);
    }

    protected int computeVerticalScrollOffset()  {
        return (int)(margin.y* scale -shift.y);
    }

    protected int computeVerticalScrollRange()  {
        return (int)((size.y+margin.y*2)* scale);
    }

    public void upScale(float scl, float focusX, float focusY) {
        float oldScale = scale;
        scale *= scl;
        scale = Math.max(minScale, Math.min(scale, minScale * 15f));  // max scale - 15
        scl = scale / oldScale;
        shift.x += (focusX -shift.x) - (focusX -shift.x) * scl;
        shift.y += (focusY -shift.y) - (focusY -shift.y) * scl;
        redraw();
    }

    public void shiftShift(float x, float y) {
        shift.x -= x;
        shift.y -= y;

        float wx = getWidth(), wy = getHeight();

        if ((2 * margin.x + size.x) * scale >= wx) {
            shift.x = Math.min(margin.x * scale, shift.x);
            shift.x = Math.max(wx - (margin.x + size.x) * scale, shift.x);
        } else
            shift.x = (wx - size.x * scale) / 2; // keep on centre

        if ((2 * margin.y + size.y) * scale >= wy) {
            shift.y = Math.min(margin.y * scale, shift.y);
            shift.y = Math.max(wy - (margin.y + size.y) * scale, shift.y);
        } else
            shift.y = (wy - size.y * scale) / 2; // keep on centre

        redraw();
    }

    protected abstract void singleTap(float x, float y);

    protected abstract void doubleTap(float x, float y);

    protected void longPress(float x, float y) {}

    protected abstract PointF getContentSize();

    public void contentChanged(viewState vs) {
        newState = vs;
        resetScale();
        resetCache();
        redraw();
    }

    private void resetScale() {
        size = getContentSize();
        if( ExtPointF.isNull(size) ) return; // if content not ready
        margin = new PointF( size.x/10f, size.y/10f );

        float xScale, yScale, wx=getWidth(), wy=getHeight();
        xScale =  wx / size.x;
        yScale =  wy / size.y;
        scale = Math.min( xScale, yScale ); // choose smallest scale
        minScale = scale *0.8f;

        shift.x=( wx - size.x * scale) /2;
        shift.y=( wy - size.y * scale) /2;

        if( newState!=null ) {
            if( !newState._size.equals(size.x,size.y) ) Log.e("TouchView /237","Wrong new state.");
            else {
                scale = newState._Scale;
                minScale = newState._minScale;
                shift = newState._shift;
                margin = newState._margin;
            }
            newState=null;
        }
    }

    private void resetCache() {
        if( buffers != null ) {
            buffers[0].bmp.eraseColor(Color.WHITE);
            buffers[1].bmp.eraseColor(Color.WHITE);
        }
    }

    protected float getScale() {
        return scale;
    }

    @Override
    protected void onDraw(Canvas c) {

        /*if( MapActivity.maxTexture==0 ) {
            int[] maxTextureSize = new int[1];
            GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
            Log.d("GLES10", "Texture max: " + maxTextureSize[0]);
            MapActivity.maxTexture = maxTextureSize[0];
        } */

        int cs = c.save();
        float scl;
        synchronized (this) {
            if( buffers != null ) {
                DrawBuffer visibleBuffer = buffers[visibleBufferIndex];
                if (visibleBuffer.scale != 0 && visibleBuffer.bmp != null) {
                    scl = scale / visibleBuffer.scale;

                    c.translate(shift.x - (visibleBuffer.shift.x + bufferShift.x) * scl,
                                shift.y - (visibleBuffer.shift.y + bufferShift.y) * scl);
                    c.scale(scl, scl);

                    c.drawBitmap(visibleBuffer.bmp, 0, 0, p);
                }
            }
            //else  Log.i("TouchView /261", "scale = "+buffers[visibleBufferIndex].scale + "  BMP = "+buffers[visibleBufferIndex].bmp );
        }
        c.restoreToCount(cs);
        //Log.i("TouchView /256", "scale = "+bmpScale[visibleBufferIndex] + "  BMP = "+cacheBMP[visibleBufferIndex] );
    }

    void drawBMP(DrawBuffer drawBuffer) {
        Log.i("TouchView", "drawBMP started");
        if (drawBuffer == null) {
            Log.e("drawBMP", "drawBuffer is null!");
            return;
        }

        drawBuffer.shift.x = shift.x;
        drawBuffer.shift.y = shift.y;
        drawBuffer.scale = scale;

        drawBuffer.bmp.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(drawBuffer.bmp);
        canvas.translate(drawBuffer.shift.x+ bufferShift.x, drawBuffer.shift.y+ bufferShift.y); // Math.round
        canvas.scale(drawBuffer.scale, drawBuffer.scale);

        myDraw(canvas);
        Log.i("TouchView", "drawBMP finished");
    }

    public void redraw() {
        startDraw();
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
            _Scale = scale;
            _minScale = minScale;
            _shift = new PointF(shift.x, shift.y);
            _size = new PointF(size.x, size.y);
            _margin = new PointF(margin.x, margin.y);
        }
    }
}
