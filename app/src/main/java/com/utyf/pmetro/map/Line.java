package com.utyf.pmetro.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtInteger;
import com.utyf.pmetro.util.ExtPath;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.Util;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


public class Line {

    final String    name;
    int       lineNum;
    int       trpNum;
    TRP.TRP_line   trpLine;
    String    stationLabel;
    public int Color, LabelsColor, LabelsBColor, shadowColor;
    boolean   drawLblBkgr=true;
    Float     LinesWidth, stationDiameter, stationRadius;
    PointF[]  coordinates;
    RectF[]   Rects;
    RectF     lineSelect;
//    Rect      Rects2;
//    int[]     Heights;
    ArrayList<AdditionalNodes> addNodes;
    ExtPath   path, pathIdle;
    MAP       map;
    private float   txtFontShift, txtFontSize, txtSmallFontShift, txtSmallFontSize;
    private Paint   txtPaint;
    private DashPathEffect dashPathEffect;


    public Line(Section sec, MAP m) {
        param prm;
        map = m;
        LinesWidth = map.LinesWidth;
        stationDiameter = map.StationDiameter;
        stationRadius = map.StationRadius;

        name = sec.name;
        StationsNum st = TRP.getLineNum(name);
        if( st==null ) {
            Log.e("Line /48","Can't find line - "+name);
            return;
        }
        lineNum = st.line;
        trpNum  = st.trp;
        //noinspection ConstantConditions
        trpLine = TRP.getTRP(trpNum).getLine(lineNum);

        Line lMetro=null;
        if( MapData.mapMetro!=null ) lMetro=MapData.mapMetro.getLine(name); // for get default values

        if( (prm=sec.getParam("Color"))!=null ) Color = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);
        else
            if( lMetro!=null ) Color = lMetro.Color;
            else Color=0xFF000000;

        if( (prm=sec.getParam("LabelsColor"))==null ) {
            if (lMetro != null) LabelsColor = lMetro.LabelsColor;
            else LabelsColor = Color;
        }
        else  LabelsColor = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);
        shadowColor = Util.getDarkColor(LabelsColor);

        if( (prm=sec.getParam("LabelsBColor"))==null ) LabelsBColor = 0xffffffff;
        else
            if( prm.value.equals("-1") ) drawLblBkgr=false;
            else LabelsBColor = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);

        LoadCoordinates( sec.getParamValue("Coordinates") );
        LoadRects( sec.getParamValue("Rects") );
        LoadRect ( sec.getParam("Rect") );

        //noinspection ConstantConditions
        stationLabel = map.stnLabels.get(TRP.getTRP(trpNum).Type);

        dashPathEffect = new DashPathEffect(
                new float[]{LinesWidth*1.5f, LinesWidth*0.5f}, 0);

        txtFontSize = stationDiameter * 0.66f;
        txtFontShift = stationDiameter * 0.26f; // shift to down
        txtSmallFontShift = txtFontShift * 0.75f;
        txtSmallFontSize = txtFontSize * 0.75f;

        txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint.setTextAlign(Paint.Align.CENTER);
        if( MapData.map.IsVector )   txtPaint.setColor(0xffffffff);
        else                         txtPaint.setColor(0xff000000);
    }

    void addAddNode(String[] strs) {

        AdditionalNodes ad = new AdditionalNodes( strs, map);
        if( addNodes == null ) addNodes = new ArrayList<>();
        addNodes.add(ad);
    }

    private AdditionalNodes findAddNode(int st1, int st2) {
        if( addNodes==null ) return null;

        for( AdditionalNodes an : addNodes )  {
            if( an.numSt1==st1 && an.numSt2==st2 ) return an;
            if( an.numSt1==st2 && an.numSt2==st1 ) return an;
        }
        return null;
    }

    private void LoadCoordinates(String cc) {
        int    i, j;
        float  x, y;

        if( cc.isEmpty() )  // if no data
            { coordinates = null; return; }

        String[] strs=cc.split(",");

        if( strs.length%2 != 0 ) {  // if wrong data
            Log.e("Line /80","Line " + name + ". Number of coordinates not even - " + strs.length + " -" + cc +"- ");
            coordinates = null;
            return;
        }

        coordinates = new PointF[strs.length/2];

        j=0;
        for( i=0; i<strs.length; i+=2 ) {
            x = ExtFloat.parseFloat(strs[i]);
            y = ExtFloat.parseFloat(strs[i+1]);
            coordinates[j++] = new PointF(x,y);
        }
    } // LoadCoordinates()

    private void LoadRects(String cc) {
        int     i, j;
        float   x1, y1, x2, y2;

        if( cc.length()<1 )  // if no data
            { Rects=null; return; }

        String[] strs=cc.split(",");

        if( strs.length%4 != 0 ) {  // if wrong data
            Log.e("Line /127","Line " + name + ". Number of rectangle coordinates not even - " + strs.length + " -" + cc +"- ");
            Rects=null;
            return;
        }

        Rects = new RectF[strs.length/4];

        j=0;
        for( i=0; i<strs.length; i+=4 ) {
            x1 = ExtFloat.parseFloat(strs[i]);
            y1 = ExtFloat.parseFloat(strs[i + 1]);
            x2 = ExtFloat.parseFloat(strs[i + 2]) + x1;
            y2 = ExtFloat.parseFloat(strs[i + 3]) + y1;

            Rects[j] = new RectF(x1,y1,x2,y2);
            j++;
        }
    }

    private void LoadRect(param prm) {
        float  x1, y1, x2, y2;

        if( prm==null || prm.value.isEmpty() )  // if no data
            { lineSelect=null; return; }
        String[] strs=prm.value.split(",");

        if( strs.length != 4 ) {  // if wrong data
            Log.e("Line /154","Line " + name + ". Number of rectangle coordinates not 4 - " + strs.length + " -" + prm.value +"- ");
            Rects=null;
            return;
        }

        x1 = ExtFloat.parseFloat(strs[0]);
        y1 = ExtFloat.parseFloat(strs[1]);
        x2 = ExtFloat.parseFloat(strs[2]) + x1;
        y2 = ExtFloat.parseFloat(strs[3]) + y1;
        lineSelect = new RectF(x1,y1,x2,y2);
    }

    public void PathTo(int stFrom, int stTo, ExtPath pth)  { // make path through additional nodes
        AdditionalNodes an;
        an = findAddNode(stFrom, stTo);

        if( pth==null || coordinates==null || stTo<0 || stFrom<0 || stTo>=coordinates.length || stFrom>=coordinates.length )
            return;

        if( coordinates[stFrom].x==0 && coordinates[stFrom].y==0 ) return;
        if( coordinates[stTo].x==0   && coordinates[stTo].y==0   ) return;

        pth.moveTo(coordinates[stFrom].x, coordinates[stFrom].y);

        if( an == null )
            pth.lineTo( coordinates[stTo].x, coordinates[stTo].y );
        else {
            if( an.pnts[0].x==0 && an.pnts[0].y==0 ) return;
            if (an.Spline) {
                PointF[] pnts = new PointF[an.pnts.length+2];
                pnts[0] = coordinates[stFrom];
                pnts[pnts.length-1] = coordinates[stTo];

                if( an.numSt1==stFrom )  // forward or reverse
                   System.arraycopy(an.pnts,0,pnts,1,an.pnts.length);
                else
                    for( int i=0; i<an.pnts.length; i++ )
                        pnts[an.pnts.length-i] = an.pnts[i];

                pth.Spline(pnts);
            } else {
                if( an.numSt1==stFrom )  // forward or reverse
                    for( PointF pnt : an.pnts ) pth.lineTo(pnt.x, pnt.y);
                else
                    for( int i=an.pnts.length-1; i>=0; i-- )
                        pth.lineTo(an.pnts[i].x, an.pnts[i].y);

                pth.lineTo(coordinates[stTo].x, coordinates[stTo].y);
            }
        }
    }

//    TRP.TRP_line getTRPLine() {
//        return trpLine;
//    }

    public PointF getCoord(int stNum) {
        if( coordinates==null || coordinates.length<=stNum ) return null;
        return coordinates[stNum];
    }

    void CreatePath() {
        TRP.TRP_Station st, st2;
        float tm;

        path = new ExtPath();
        pathIdle = new ExtPath();
        if( trpLine==null ) return;

        for( int i=0; i<trpLine.Stations.length; i++ )  {
            st = trpLine.Stations[i];

            for( TRP.TRP_Driving drv : st.drivings )  {
                if( drv.frwDR>0 )     PathTo(i, drv.frwStNum, path);
                else
                   if( drv.frwStNum>=0 ) PathTo(i, drv.frwStNum, pathIdle);

                if( drv.bckStNum<0 )  continue;
                st2 = trpLine.Stations[drv.bckStNum];
                tm  = trpLine.getForwTime( st2,st );
                if( tm<0 )       // draw back way only if there is not forward way
                    if( drv.bckDR>0 )  PathTo(i, drv.bckStNum, path);
                    else               PathTo(i, drv.bckStNum, pathIdle);
            }
        }
    }

    private boolean Hit( PointF pnt, float x, float y, int hitCircle ) { // hit into station
        float xx = pnt.x - x;
        float yy = pnt.y - y;
        return Math.sqrt(xx*xx+yy*yy) <= stationRadius+hitCircle;
    }

    public int stationByPoint(float x, float y) {  // find hit station

        if( coordinates==null ) return -1;

        for( int i=0; i< coordinates.length; i++ )  {
            if( Hit(coordinates[i],x,y,0) ) return i;   // check station circle
            if( Rects!=null && i<Rects.length && Rects[i].contains((int)x,(int)y) ) return i; // check station name rect
        }
        return -1;
    }

    public Integer[] stationsByPoint(float x, float y, int hitCircle) {  // find hit station
        if( coordinates==null ) return null;

        LinkedHashSet<Integer> stns = new LinkedHashSet<>();

        for( int i=0; i< coordinates.length; i++ )  {
            if( Hit(coordinates[i],x,y,hitCircle) ) stns.add(i);   // check station circle
            if( Rects!=null && i<Rects.length && Rects[i].contains((int)x,(int)y) ) stns.add(i); // check station name rect
        }

        if( stns.size()>0 ) return stns.toArray( new Integer[stns.size()] );
        return null;
    } //*/

    void DrawLines(Canvas canvas, Paint p) {
        if( coordinates == null )  return;
        p.setColor( Color );
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(LinesWidth);
        canvas.drawPath(path, p);

        p.setPathEffect( dashPathEffect );
        canvas.drawPath( pathIdle, p );
        p.setPathEffect( null );

        // draw line symbol
        final float length=stationDiameter*2f;
        if( lineSelect!=null && lineSelect.left!=0 && lineSelect.top!=0 ) { //&& lineSelect.right!=0 && lineSelect.bottom!=0 ) {
            float y = lineSelect.top + (lineSelect.bottom-lineSelect.top)/2;
            float x = lineSelect.left + 10 + LinesWidth/2;

            canvas.drawLine( x,y, x+length,y, p );
            p.setStyle( Paint.Style.FILL );
            canvas.drawCircle( x,y, LinesWidth/2f, p );
            canvas.drawCircle( x+length,y, LinesWidth/2f, p );

            p.setColor( 0xffffff00 ); // draw yellow circle
            canvas.drawCircle( x+length/2, y, stationRadius+1, p );
            p.setColor( Color );      // draw station circle
            canvas.drawCircle( x+length/2, y, stationRadius, p);
            p.setStyle( Paint.Style.STROKE );
        }
    }

    void DrawYellowStations(Canvas canvas, Paint p) {
        if( coordinates == null )  return;

        p.setColor( 0xffffff00 );
        for( PointF crd : coordinates )
            if( crd.x!=0 || crd.y!=0 )
                canvas.drawCircle(crd.x, crd.y, stationRadius+1, p);
    }

    void DrawStations(Canvas canvas, Paint p) {

        if( coordinates == null ) return;
        p.setColor( Color );

        for( int i=0; i< coordinates.length; i++ )
            if( coordinates[i].x != 0 || coordinates[i].y != 0 )  {
                canvas.drawCircle( coordinates[i].x, coordinates[i].y, stationRadius, p );
                if( !trpLine.Stations[i].isWorking ) {  // mark non working stations
                    p.setColor( 0xffffffff );
                    canvas.drawCircle( coordinates[i].x, coordinates[i].y, stationRadius*0.6f, p );
                    p.setColor( Color );
                } else drawText(canvas,i);
            }
    }

    void drawAllTexts(Canvas canvas) {
        if( coordinates == null ) return;

        for( int i=0; i< coordinates.length; i++ )
            if( coordinates[i].x != 0 || coordinates[i].y != 0 )
                if( trpLine.Stations[i].isWorking ) drawText(canvas,i);
    }

    void drawText(Canvas canvas, int stNum) {
        String tm;
        if( txtPaint == null ) return;

        if( stationLabel!=null ) {
            txtPaint.setTextSize(txtFontSize);
            canvas.drawText(stationLabel, coordinates[stNum].x, coordinates[stNum].y + txtFontShift, txtPaint);
        }
        else
            if( TRP.routeStart!=null && TRP.isActive(TRP.routeStart.trp) && !(tm=getTime(stNum)).isEmpty() )
                if( tm.length()<3 ) {
                    txtPaint.setTextSize(txtFontSize);
                    canvas.drawText(tm, coordinates[stNum].x, coordinates[stNum].y + txtFontShift, txtPaint);
                }
                else {
                    txtPaint.setTextSize(txtSmallFontSize);
                    canvas.drawText(tm, coordinates[stNum].x, coordinates[stNum].y + txtSmallFontShift, txtPaint);
                }
    }

    void DrawStationNames(Canvas canvas, Paint p) {
        if( Rects == null )  return;

        p.setColor(LabelsColor);
        Typeface tf = p.getTypeface();
        p.setTypeface( Typeface.create(tf,Typeface.BOLD) );
        p.setTextSize(12);

        for( int i=0;  i<Rects.length; i++ )
            drawStnName( TRP.getLine(trpNum,lineNum).getStationName(i), coordinates[i], Rects[i], p, canvas);
    }

    private Paint   pBCKG, pWhite;  // for draw background
    private float   ascent, height;
    private static RectF   rr = new RectF();
    private void drawStnName( String nm, PointF pntSt, RectF r, Paint p, Canvas c )  {
        int i;
        String nm2;

        if( r.left==0 && r.top==0 ) return;
        if( map.UpperCase ) nm=nm.toUpperCase();

        if( pBCKG==null ) {
            pBCKG = new Paint(p);
            pWhite = new Paint(p);
            if( drawLblBkgr ) pBCKG.setColor(LabelsBColor);
            else              pBCKG.setColor(shadowColor);
            pWhite.setColor(0xffffffff);
            Paint.FontMetrics fm = p.getFontMetrics();
            ascent = fm.ascent;
            height = fm.descent/2 - fm.top;
        }

        if( r.right-r.left < r.bottom-r.top ) { // turn on -90°
            c.save();
            c.rotate( -90, r.left, r.bottom );

            if( r.top>pntSt.y ) {  // align to down
                rr.left = r.left - ( p.measureText(nm)-(r.bottom-r.top) );
                rr.right = r.left + (r.bottom-r.top);
                rr.top = r.bottom;
                rr.bottom = rr.top + height;
                drawName(c,nm,rr,p);
            } else {    // align to top
                rr.left = r.left;
                rr.right = r.left + p.measureText(nm);
                rr.top = r.bottom;
                rr.bottom = rr.top + height;
                drawName(c,nm,rr,p);
            }
            c.restore();
        } else {
            i = nm.indexOf(' ');
            if( i!=-1 && map.WordWrap ) {
                nm2 = nm.substring(i+1);
                nm  = nm.substring(0,i);
            } else nm2=null;

            if( r.left>pntSt.x ) {  // align left or right
                rr.left   = r.left;
                rr.right  = r.left + p.measureText(nm);
                rr.top    = r.top;
                rr.bottom = rr.top + height;
                drawName(c,nm,rr,p);
                if( nm2!=null ) {
                    rr.right   = r.left + p.measureText(nm2);
                    rr.top    += height;
                    rr.bottom += height;
                    drawName(c,nm2,rr,p);
                }
            } else {
                rr.left   = r.right - p.measureText(nm);
                rr.right  = r.right;
                rr.top    = r.top;
                rr.bottom = rr.top + height;
                drawName(c,nm,rr,p);
                if( nm2!=null ) {
                    rr.left    = r.right - p.measureText(nm2);
                    rr.top    += height;
                    rr.bottom += height;
                    drawName(c,nm2,rr,p);
                }
            }
        }
    }

    private void drawName(Canvas c, String nm, RectF rr, Paint p) {
        if( drawLblBkgr ) c.drawRect(rr, pBCKG);
        else {
            c.drawText(nm, rr.left - 0.5f, rr.top - ascent - 0.5f, pWhite);
            if( LabelsColor!=0xff000000 )
                c.drawText(nm, rr.left + 0.5f, rr.top - ascent + 0.5f, pBCKG);
        }
        c.drawText(nm, rr.left, rr.top - ascent, p);
    }

    //private Paint pr;
    //private RouteTimes.Line   tll;//,tle;

    //void DrawTimes(Canvas canvas) {
        /*float txtFontShift, txtFontSize;
        float txtSmallFontShift, txtSmallFontSize;
        txtFontSize = stationDiameter * 0.66f;
        txtFontShift = stationDiameter * 0.26f; // shift to down
        txtSmallFontShift = txtFontShift * 0.8f;
        txtSmallFontSize = txtFontSize * 0.7f;

        if( coordinates == null )  return;
        if( pr == null )
            pr = new Paint(Paint.ANTI_ALIAS_FLAG);

        pr.setTextAlign(Paint.Align.CENTER);
        if( Map_View.map.IsVector )   pr.setColor(0xffffffff);
        else                          pr.setColor(0xff000000);
        pr.setTextSize(txtFontSize);

        if( TRP.rt.fromStart==null ) return;
        tll = TRP.rt.fromStart.trps[trpNum].lines[lineNum];
        //if( TRP.rt.tooEnd!=null && MapActivity.debugMode )
        //   tle = TRP.rt.tooEnd.trps[trpNum].lines[lineNum];
        String tm;
        for( int i=0; i< coordinates.length; i++ )
            if( coordinates[i].x!=0 || coordinates[i].y!=0 )
                if( stationLabel!=null )
                    canvas.drawText(stationLabel, coordinates[i].x, coordinates[i].y+txtFontShift, pr);
                else  if( !(tm=getTime(i)).isEmpty() )
                    if( tm.length()<3 )
                        canvas.drawText(tm, coordinates[i].x, coordinates[i].y+txtFontShift, pr);
                    else {
                        pr.setTextSize(txtSmallFontSize);
                        canvas.drawText(tm, coordinates[i].x, coordinates[i].y+txtSmallFontShift, pr);
                        pr.setTextSize(txtFontSize);
                    } //*/
    //}

    String getTime(int stNum) {
        int time,t1;

        synchronized( TRP.rt ) {
            time = Math.round(TRP.rt.getTime(trpNum, lineNum, stNum));
            //if( MapActivity.debugMode && TRP.rt.tooEnd!=null )
            //    time -= Math.round(TRP.rt.tooEnd.getTime(trpNum, lineNum, stNum));
        }
        //time=Math.round(tll.stns[stNum]);
        //if( tle!=null )
        //    time=Math.round(tll.stns[stNum]-tle.stns[stNum]);

        if( time<0 && !MapActivity.debugMode )   return "";
        if( time<=60 ) return Integer.toString(time);

        StringBuilder result = new StringBuilder(16);
        result.append(time/60);
        t1 = time%60;
        if( t1<10 ) result.append(".0").append(t1);
        else        result.append(".").append(t1);
        return  result.toString();
    }
}
