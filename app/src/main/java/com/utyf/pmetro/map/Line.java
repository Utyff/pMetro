package com.utyf.pmetro.map;

import java.util.Arrays;
import java.util.LinkedHashSet;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtPath;
import com.utyf.pmetro.util.StationsNum;

/**
 * Displays a metro line
 *
 * @author Utyf
 */

public class Line {
    public Line_Parameters parameters;
    MAP_Parameters map_parameters;
    MapData mapData;

    int lineNum;
    int trpNum;
    TRP_line trpLine;
    String stationLabel; //  Different transports can have their own labels on different maps
    ExtPath path, pathIdle;
    private float txtFontShift, txtFontSize, txtSmallFontShift, txtSmallFontSize;
    Float LinesWidth, stationDiameter, stationRadius;
    private Paint txtPaint;
    private DashPathEffect dashPathEffect;
    private float[] stationTimes;


    public Line(Line_Parameters parameters, MAP_Parameters map_parameters, MapData mapData) {
        this.parameters = parameters;
        this.map_parameters = map_parameters;
        this.mapData = mapData;

        LinesWidth = map_parameters.LinesWidth;
        stationDiameter = map_parameters.StationDiameter;
        stationRadius = map_parameters.StationRadius;

        StationsNum st = mapData.transports.getLineNum(parameters.name);
        if (st == null) {
            Log.e("Line /48", "Can't find line - " + parameters.name);
            return;
        }
        lineNum = st.line;
        trpNum = st.trp;
        //noinspection ConstantConditions
        trpLine = mapData.transports.getTRP(trpNum).getLine(lineNum);
        stationTimes = new float[trpLine.Stations.length];
        Arrays.fill(stationTimes, -1);

        //noinspection ConstantConditions
        stationLabel = map_parameters.stnLabels.get(mapData.transports.getTRP(trpNum).type);

        dashPathEffect = new DashPathEffect(
                new float[]{LinesWidth * 1.5f, LinesWidth * 0.5f}, 0);

        txtFontSize = stationDiameter * 0.66f;
        txtFontShift = stationDiameter * 0.26f; // shift to down
        txtSmallFontShift = txtFontShift * 0.75f;
        txtSmallFontSize = txtFontSize * 0.75f;

        txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint.setTextAlign(Paint.Align.CENTER);
        if (map_parameters.IsVector) txtPaint.setColor(0xffffffff);
        else txtPaint.setColor(0xff000000);
    }

    public void PathTo(int stFrom, int stTo, ExtPath pth) { // make path through additional nodes
        AdditionalNodes an;
        an = parameters.findAddNode(stFrom, stTo);

        if (pth == null || parameters.coordinates == null || stTo < 0 || stFrom < 0 ||
                stTo >= parameters.coordinates.length || stFrom >= parameters.coordinates.length)
            return;

        if (parameters.coordinates[stFrom].x == 0 && parameters.coordinates[stFrom].y == 0) return;
        if (parameters.coordinates[stTo].x == 0 && parameters.coordinates[stTo].y == 0) return;

        pth.moveTo(parameters.coordinates[stFrom].x, parameters.coordinates[stFrom].y);

        if (an == null)
            pth.lineTo(parameters.coordinates[stTo].x, parameters.coordinates[stTo].y);
        else {
            if (an.points[0].x == 0 && an.points[0].y == 0) return;
            if (an.spline) {
                PointF[] pnts = new PointF[an.points.length + 2];
                pnts[0] = parameters.coordinates[stFrom];
                pnts[pnts.length - 1] = parameters.coordinates[stTo];

                if (an.numSt1 == stFrom)  // forward or reverse
                    System.arraycopy(an.points, 0, pnts, 1, an.points.length);
                else
                    for (int i = 0; i < an.points.length; i++)
                        pnts[an.points.length - i] = an.points[i];

                pth.Spline(pnts);
            } else {
                if (an.numSt1 == stFrom)  // forward or reverse
                    for (PointF pnt : an.points) pth.lineTo(pnt.x, pnt.y);
                else
                    for (int i = an.points.length - 1; i >= 0; i--)
                        pth.lineTo(an.points[i].x, an.points[i].y);

                pth.lineTo(parameters.coordinates[stTo].x, parameters.coordinates[stTo].y);
            }
        }
    }

//    TRP.TRP_line getTRPLine() {
//        return trpLine;
//    }

    @Nullable
    public PointF getCoord(int stNum) {
        if (parameters.coordinates == null || parameters.coordinates.length <= stNum) return null;
        return parameters.coordinates[stNum];
    }

    void CreatePath() {
        TRP_Station st, st2;
        float tm;

        path = new ExtPath();
        pathIdle = new ExtPath();
        if (trpLine == null) return;

        for (int i = 0; i < trpLine.Stations.length; i++) {
            st = trpLine.Stations[i];

            for (TRP_Driving drv : st.drivings) {
                if (drv.frwDR > 0) PathTo(i, drv.frwStNum, path);
                else if (drv.frwStNum >= 0) PathTo(i, drv.frwStNum, pathIdle);

                if (drv.bckStNum < 0) continue;
                st2 = trpLine.Stations[drv.bckStNum];
                tm = TRP_line.getForwTime(st2, st);
                if (tm < 0)       // draw back way only if there is not forward way
                    if (drv.bckDR > 0) PathTo(i, drv.bckStNum, path);
                    else PathTo(i, drv.bckStNum, pathIdle);
            }
        }
    }

    private boolean Hit(PointF pnt, float x, float y, int hitCircle) { // hit into station
        float xx = pnt.x - x;
        float yy = pnt.y - y;
        return Math.sqrt(xx * xx + yy * yy) <= stationRadius + hitCircle;
    }

    public int stationByPoint(float x, float y) {  // find hit station

        if (parameters.coordinates == null) return -1;

        for (int i = 0; i < parameters.coordinates.length; i++) {
            if (Hit(parameters.coordinates[i], x, y, 0)) return i;   // check station circle
            if (parameters.Rects != null && i < parameters.Rects.length && parameters.Rects[i].contains((int) x, (int) y))
                return i; // check station name rect
        }
        return -1;
    }

    public Integer[] stationsByPoint(float x, float y, int hitCircle) {  // find hit station
        if (parameters.coordinates == null) return null;

        LinkedHashSet<Integer> stns = new LinkedHashSet<>();

        for (int i = 0; i < parameters.coordinates.length; i++) {
            if (Hit(parameters.coordinates[i], x, y, hitCircle))
                stns.add(i);   // check station circle
            if (parameters.Rects != null && i < parameters.Rects.length && parameters.Rects[i].contains((int) x, (int) y))
                stns.add(i); // check station name rect
        }

        if (stns.size() > 0) return stns.toArray(new Integer[stns.size()]);
        return null;
    } //*/

    void DrawLines(Canvas canvas, Paint p) {
        if (parameters.coordinates == null) return;
        p.setColor(parameters.Color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(LinesWidth);
        canvas.drawPath(path, p);

        p.setPathEffect(dashPathEffect);
        canvas.drawPath(pathIdle, p);
        p.setPathEffect(null);

        // draw line symbol
        final float length = stationDiameter * 2f;
        if (parameters.lineSelect != null && parameters.lineSelect.left != 0 && parameters.lineSelect.top != 0) { //&& lineSelect.right!=0 && lineSelect.bottom!=0 ) {
            float y = parameters.lineSelect.top + (parameters.lineSelect.bottom - parameters.lineSelect.top) / 2;
            float x = parameters.lineSelect.left + 10 + LinesWidth / 2;

            canvas.drawLine(x, y, x + length, y, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, LinesWidth / 2f, p);
            canvas.drawCircle(x + length, y, LinesWidth / 2f, p);

            p.setColor(0xffffff00); // draw yellow circle
            canvas.drawCircle(x + length / 2, y, stationRadius + 1, p);
            p.setColor(parameters.Color);      // draw station circle
            canvas.drawCircle(x + length / 2, y, stationRadius, p);
            p.setStyle(Paint.Style.STROKE);
        }
    }

    void DrawYellowStations(Canvas canvas, Paint p) {
        if (parameters.coordinates == null) return;

        p.setColor(0xffffff00);
        for (PointF crd : parameters.coordinates)
            if (crd.x != 0 || crd.y != 0)
                canvas.drawCircle(crd.x, crd.y, stationRadius + 1, p);
    }

    void DrawStations(Canvas canvas, Paint p) {

        if (parameters.coordinates == null) return;
        p.setColor(parameters.Color);

        for (int i = 0; i < parameters.coordinates.length; i++)
            if (parameters.coordinates[i].x != 0 || parameters.coordinates[i].y != 0) {
                canvas.drawCircle(parameters.coordinates[i].x, parameters.coordinates[i].y, stationRadius, p);
                if (!trpLine.Stations[i].isWorking) {  // mark non working stations
                    p.setColor(0xffffffff);
                    canvas.drawCircle(parameters.coordinates[i].x, parameters.coordinates[i].y, stationRadius * 0.6f, p);
                    p.setColor(parameters.Color);
                } else drawText(canvas, i);
            }
    }

    void drawAllTexts(Canvas canvas) {
        if (parameters.coordinates == null) return;

        for (int i = 0; i < parameters.coordinates.length; i++)
            if (parameters.coordinates[i].x != 0 || parameters.coordinates[i].y != 0)
                if (trpLine.Stations[i].isWorking) drawText(canvas, i);
    }

    public void drawText(Canvas canvas, int stNum) {
        String tm;
        if (txtPaint == null) return;

        if (stationLabel != null) {
            txtPaint.setTextSize(txtFontSize);
            canvas.drawText(stationLabel, parameters.coordinates[stNum].x, parameters.coordinates[stNum].y + txtFontShift, txtPaint);
        } else if (stationTimes[stNum] != -1) {
            tm = formatTime(stationTimes[stNum]);
            if (tm.length() < 3) {
                txtPaint.setTextSize(txtFontSize);
                canvas.drawText(tm, parameters.coordinates[stNum].x, parameters.coordinates[stNum].y + txtFontShift, txtPaint);
            } else {
                txtPaint.setTextSize(txtSmallFontSize);
                canvas.drawText(tm, parameters.coordinates[stNum].x, parameters.coordinates[stNum].y + txtSmallFontShift, txtPaint);
            }
        }
    }

    void DrawStationNames(Canvas canvas, Paint p) {
        if (parameters.Rects == null) return;

        p.setColor(parameters.LabelsColor);
        Typeface tf = p.getTypeface();
        p.setTypeface(Typeface.create(tf, Typeface.BOLD));
        p.setTextSize(12);

        for (int i = 0; i < parameters.Rects.length; i++)
            drawStnName(mapData.transports.getLine(trpNum, lineNum).getStationName(i), parameters.coordinates[i], parameters.Rects[i], p, canvas);
    }

    private Paint pBCKG, pWhite;  // for draw background
    private float ascent, height;
    private static RectF rr = new RectF();

    private void drawStnName(String nm, PointF pntSt, RectF r, Paint p, Canvas c) {
        int i;
        String nm2;

        if (r.left == 0 && r.top == 0) return;
        if (map_parameters.UpperCase) nm = nm.toUpperCase();

        if (pBCKG == null) {
            pBCKG = new Paint(p);
            pWhite = new Paint(p);
            if (parameters.drawLblBkgr) pBCKG.setColor(parameters.LabelsBColor);
            else pBCKG.setColor(parameters.shadowColor);
            pWhite.setColor(0xffffffff);
            Paint.FontMetrics fm = p.getFontMetrics();
            ascent = fm.ascent;
            height = fm.descent / 2 - fm.top;
        }

        if (r.right - r.left < r.bottom - r.top) { // turn on -90 degrees
            c.save();
            c.rotate(-90, r.left, r.bottom);

            if (r.top > pntSt.y) {  // align to down
                rr.left = r.left - (p.measureText(nm) - (r.bottom - r.top));
                rr.right = r.left + (r.bottom - r.top);
                rr.top = r.bottom;
                rr.bottom = rr.top + height;
                drawName(c, nm, rr, p);
            } else {    // align to top
                rr.left = r.left;
                rr.right = r.left + p.measureText(nm);
                rr.top = r.bottom;
                rr.bottom = rr.top + height;
                drawName(c, nm, rr, p);
            }
            c.restore();
        } else {
            i = nm.indexOf(' ');
            if (i != -1 && map_parameters.WordWrap) {
                nm2 = nm.substring(i + 1);
                nm = nm.substring(0, i);
            } else nm2 = null;

            if (r.left > pntSt.x) {  // align left or right
                rr.left = r.left;
                rr.right = r.left + p.measureText(nm);
                rr.top = r.top;
                rr.bottom = rr.top + height;
                drawName(c, nm, rr, p);
                if (nm2 != null) {
                    rr.right = r.left + p.measureText(nm2);
                    rr.top += height;
                    rr.bottom += height;
                    drawName(c, nm2, rr, p);
                }
            } else {
                rr.left = r.right - p.measureText(nm);
                rr.right = r.right;
                rr.top = r.top;
                rr.bottom = rr.top + height;
                drawName(c, nm, rr, p);
                if (nm2 != null) {
                    rr.left = r.right - p.measureText(nm2);
                    rr.top += height;
                    rr.bottom += height;
                    drawName(c, nm2, rr, p);
                }
            }
        }
    }

    private void drawName(Canvas c, String nm, RectF rr, Paint p) {
        if (parameters.drawLblBkgr) c.drawRect(rr, pBCKG);
        else {
            c.drawText(nm, rr.left - 0.5f, rr.top - ascent - 0.5f, pWhite);
            if (parameters.LabelsColor != 0xff000000)
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

    String formatTime(float _time) {
        int time, t1;

        time = Math.round(_time);
        //if( MapActivity.debugMode && TRP.rt.tooEnd!=null )
        //    time -= Math.round(TRP.rt.tooEnd.getTime(trpNum, lineNum, stNum));

        //time=Math.round(tll.stns[stNum]);
        //if( tle!=null )
        //    time=Math.round(tll.stns[stNum]-tle.stns[stNum]);

        if (time < 0 && !MapActivity.debugMode) return "";
        if (time <= 60) return Integer.toString(time);

        StringBuilder result = new StringBuilder(16);
        result.append(time / 60);
        t1 = time % 60;
        if (t1 < 10) result.append(".0").append(t1);
        else result.append(".").append(t1);
        return result.toString();
    }

    public int getColor() {
        return parameters.Color;
    }

    public void setStationTime(int stn, float time) {
        stationTimes[stn] = time;
    }

    public void clearStationTimes() {
        Arrays.fill(stationTimes, -1);
    }
}
