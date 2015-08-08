package com.utyf.pmetro.map.vec;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;


public class VEC_Element_Image extends VEC_Element {
    float  x=0,  y=0;
    String name;
    VEC    vec;

    public VEC_Element_Image(String param, VEC vv) {
        super(vv);

        String[] strs=param.split(",");

        name = strs[0].trim();
        if( strs.length > 1 ) {
            x = ExtFloat.parseFloat(strs[1])*v.scale;
            y = ExtFloat.parseFloat(strs[2])*v.scale;
        }

        if( name.endsWith(".vec")) {  // todo file type not here ?
            vec = new VEC();
            if( vec.load(name)<0 ) {
                vec=null;
                Log.i("VEC image /35", "Can`t load file - "+name);
            }
        }
        else {
            vec=null;
            Log.e("VEC_image /40", "Image file is not VEC - "+name);
        }
    }

    @Override
    public void Draw(Canvas canvas, Paint p) {

        if( vec==null ) return;
        int s=canvas.save();
        canvas.translate(x,y);
        canvas.scale(v.scale,v.scale);
        vec.Draw(canvas);
        canvas.restoreToCount(s);
    }
}
