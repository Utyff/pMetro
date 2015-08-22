package com.utyf.pmetro.map.vec;

import java.util.ArrayList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import com.utyf.pmetro.map.Parameters;
import com.utyf.pmetro.map.param;
import com.utyf.pmetro.map.Section;
import com.utyf.pmetro.util.ExtInteger;
import com.utyf.pmetro.util.zipMap;

/**
 * Created by Utyf on 27.02.2015.
 *
 */

public class VEC extends Parameters {

    public int      currBrushColor, Opaque;
    public PointF   Size;
    float           scale=1;  // todo  remove it
    ArrayList<VEC_Element> elements;
    private Bitmap  bmp;
    Paint           mPaint;

    public VEC() {
        super();
        NameSeparator = ' '; // set " " as name separator
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /*public void onClose() {
        if( bmp!=null ) bmp.recycle();
        bmp=null;
        System.gc();
    } //*/

    @Override
    public int load(String name) {
        elements = null;
        Size = null;

        if( name.toLowerCase().endsWith(".vec") ) {
            return loadVEC(name);
        } else if( name.toLowerCase().endsWith(".bmp") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".png") ) {
            return loadImage(name);
        } else {
            Log.e("VEC /50", "Unsupported VEC file type - " + name);
            return -1;
        }
    }

    public int loadVEC(String name){
        int i;

        if( super.load(name)<0 ) return -1;

        Section sec = getSec("");
        Size = new PointF();
        String ss = sec.getParamValue("Size");
        i = ss.indexOf(120);    // looking separator for symbol 'x'
        Size.x = ExtInteger.parseInt(ss.substring(0, i));
        Size.y = ExtInteger.parseInt(ss.substring(i + 1));

        elements = new ArrayList<>();

        for( param prm : sec.params ) // convert parameters without parameters
            if( prm.name.isEmpty() )
                { prm.name = prm.value; prm.value=""; }

        for( param prm : sec.params ) {
            switch( prm.name ) {
                case "BrushColor":
                    elements.add( new VEC_Element_BrushColor(prm.value, this) );
                    break;
                case "PenColor":
                    elements.add( new VEC_Element_PenColor(prm.value, this) );
                    break;
                case "Spline":
                    elements.add( new VEC_Element_Spline(prm.value, this) );
                    break;
                case "Line":
                    elements.add( new VEC_Element_Line(prm.value, this) );
                    break;
                case "AngleTextOut":
                    elements.add( new VEC_Element_AngleTextOut(prm.value, this) );
                    break;
                case "TextOut":
                    elements.add( new VEC_Element_TextOut(prm.value, this) );
                    break;
                case "Image":
                    elements.add( new VEC_Element_Image(prm.value, this) );
                    break;
                case "Polygon":
                    elements.add( new VEC_Element_Polygon(prm.value, this) );
                    break;
                case "Ellipse":
                    elements.add( new VEC_Element_Ellipse(prm.value, this) );
                    break;
                case "Arrow":
                    elements.add( new VEC_Element_Arrow(prm.value, this) );
                    break;
                case "Angle":
                    elements.add( new VEC_Element_Angle(prm.value, this) );
                    break;
                case "SpotCircle":
                    elements.add( new VEC_Element_SpotCircle(prm.value, this) );
                    break;
                case "SpotRect":
                    elements.add( new VEC_Element_SpotRect(prm.value, this) );
                    break;
                case "Stairs":
                    elements.add( new VEC_Element_Stairs(prm.value, this) );
                    break;
                case "Railway":
                    elements.add( new VEC_Element_Railway(prm.value, this) );
                    break;
                case "Opaque":
                    elements.add( new VEC_Element_Opaque(prm.value, this) );
                    break;
                case "Dashed":
                    elements.add( new VEC_Element_Dashed(prm.value, this) );
                    break;
                case "Size":
                case "size":
                    break;
                default:
                    Log.e("VEC /131", name +" unhandled VEC command - " + prm.name +" "+prm.value );
                    break;
            }
        } // for sec.params

        secs = null; // free memory
        return 0;
    }

    public int loadImage(String name) {
        byte bb[];
        bb = zipMap.getFile(name);
        //bb = loadFile(name);
        if( bb==null ) bb=new byte[0];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        bmp  =  BitmapFactory.decodeByteArray(bb, 0, bb.length, options);
        Size = new PointF( bmp.getWidth(), bmp.getHeight());

        return 0;
    }

    public void DrawVEC(Canvas canvas) {
        if( elements==null ) return;

        int sv = canvas.save();
        currBrushColor=0xffffff;   Opaque=0xff000000;  // default Brush - white

        for( VEC_Element el: elements )    el.Draw(canvas, mPaint);

        canvas.restoreToCount(sv);
    }

    public void Draw(Canvas c)  {
        mPaint.setFilterBitmap(true);
        mPaint.setColor(0xff000000);  // default Pen color - black

        int sv = c.save();
      //  c.scale(1/scale,1/scale);
        if( bmp!=null )   c.drawBitmap(bmp,0,0,mPaint);
        else              DrawVEC(c);

        c.restoreToCount(sv);
    }

    public String SingleTap(float x, float y) {
        if( elements==null ) return null;

        String action;
        for( VEC_Element el: elements )
            if( (action=el.SingleTap(x,y))!=null )  return action;

        return null;
    }
}
