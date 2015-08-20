package com.utyf.pmetro.util;

import android.graphics.Path;
import android.graphics.PointF;

/*
 * Created by adm on 21.02.2015.
 *  By Utyf
 */

public class ExtPath extends Path {

    public void Spline(PointF[] pnts)  {

        int n=pnts.length;

        moveTo( pnts[0].x, pnts[0].y );

        if( n == 2 )  {
            lineTo( pnts[1].x, pnts[1].y );
            return;
        }

        if( n == 3 )  {
            PointF pntF = QuadControlPnt( pnts[0], pnts[1], pnts[2] );
            quadTo( pntF.x, pntF.y, pnts[2].x, pnts[2].y );
            return;
        }

        if( n == 4 )  {
            PointF pnt1 = pnts[1];
            PointF pnt2 = pnts[2];
            PointF pnt3 = pnts[3];
            ControlPNTs ctl = CubeControlPnts( pnts[0], pnt1, pnt2, pnt3, 1f );  // 1
            cubicTo( ctl.x0, ctl.y0, ctl.x1, ctl.y1, pnt3.x, pnt3.y );
            return;
        }

        for( int i=1; i+2<n; i++ )  {
            PointF pnt0 = pnts[i-1];
            PointF pnt1 = pnts[i];
            PointF pnt2 = pnts[i+1];
            PointF pnt3 = pnts[i+2];
            ControlPNTs ctl1 = CubeControlPnts( pnt0, pnt1, pnt2, pnt3, 1f ); // 1
            if( i==1 )  {
                ControlPNTs ctl3 = CubeControlPnts( pnt0, pnt1, pnt2, pnt3, 0.5f );
                quadTo( pnt1.x - (ctl3.x0 - pnt1.x), pnt1.y - (ctl3.y0 - pnt1.y), pnt1.x, pnt1.y );
            }
            cubicTo( ctl1.x0, ctl1.y0, ctl1.x1, ctl1.y1, pnt2.x, pnt2.y );
            if( i+3 == n )  {
                ControlPNTs ctl2 = CubeControlPnts( pnt0, pnt1, pnt2, pnt3, 0.5f );
                quadTo( pnt2.x - (ctl2.x1 - pnt2.x), pnt2.y - (ctl2.y1 - pnt2.y), pnt3.x, pnt3.y );
            }
        }
    }

    public static class ControlPNTs {
        float x0, y0, x1, y1;

        public ControlPNTs(float xx0, float yy0, float xx1, float yy1) {
            this.x0 = xx0;   this.y0 = yy0;
            this.x1 = xx1;   this.y1 = yy1;
        }
    }

    private static ControlPNTs CubeControlPnts
            (PointF p1, PointF p2, PointF p3, PointF p4, float tension) {
        float f1 = p1.x;
        float f2 = p1.y;
        float f3 = p2.x;
        float f4 = p2.y;
        float f5 = p3.x;
        float f6 = p3.y;
        float f7 = p4.x;
        float f8 = p4.y;
        float f9  = (f1 + f3) / 2f;
        float f10 = (f2 + f4) / 2f;
        float f11 = (f3 + f5) / 2f;
        float f12 = (f4 + f6) / 2f;
        float f13 = (f5 + f7) / 2f;
        float f14 = (f6 + f8) / 2f;
        float f15 = (float)Math.sqrt((f3-f1) * (f3-f1) + (f4-f2) * (f4-f2));
        float f16 = (float)Math.sqrt((f5-f3) * (f5-f3) + (f6-f4) * (f6-f4));
        float f17 = (float)Math.sqrt((f7-f5) * (f7-f5) + (f8-f6) * (f8-f6));
        float f18 = f15 / (f15 + f16);
        float f19 = f16 / (f17 + f16);
        float f20 = f9  + f18 * (f11 - f9);
        float f21 = f10 + f18 * (f12 - f10);
        float f22 = f11 + f19 * (f13 - f11);
        float f23 = f12 + f19 * (f14 - f12);
        float f24 = f3 + (f20 + tension * (f11 - f20)) - f20;
        float f25 = f4 + (f21 + tension * (f12 - f21)) - f21;
        float f26 = f5 + (f22 + tension * (f11 - f22)) - f22;
        float f27 = f6 + (f23 + tension * (f12 - f23)) - f23;
        return new ControlPNTs( f24, f25, f26, f27 );
    }

    private static PointF QuadControlPnt(PointF p1, PointF p2, PointF p3)  {
        float f1 = p2.x - p1.x;
        float f2 = p2.y - p1.y;
        float f3 = (float)Math.sqrt( f1*f1 + f2*f2 );
        float f4 = p3.x - p2.x;
        float f5 = p3.y - p2.y;
        float f6 = f3 / (f3 + (float)Math.sqrt( f4*f4 + f5*f5 ));
        float f7 = 1f - f6;
        float f8 = f6 * f6;
        float f9 = f7 * (2f * f6);
        PointF pntF = new PointF();
        pntF.x = ( (p2.x - f7*f7*p1.x - f8*p3.x) / f9);
        pntF.y = ( (p2.y - f7*f7*p1.y - f8*p3.y) / f9);
        return pntF;
    }
}
