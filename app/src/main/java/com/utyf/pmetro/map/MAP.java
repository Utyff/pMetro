package com.utyf.pmetro.map;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;

/**
 * Displays metro map
 *
 * @author Utyf
 */

public class MAP {
    MAP_Parameters parameters;

    private Line[] lines;
    Paint p;

    public MAP() {
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);  // todo switch by settings
    }

    public int load(String name) {
        parameters = new MAP_Parameters();
        if (parameters.load(name) < 0) {
            return -1;
        }

        lines = new Line[parameters.la.size()];
        for (int i = 0; i < parameters.la.size(); i++)
            lines[i] = new Line(parameters.la.get(i), parameters);

        for (Line l : lines) l.CreatePath(); // create line path for drawing

        MapData.isReady = true;
        return 0;
    }

    public PointF getSize() {  // todo   adjust to all vecs
        if (parameters.vecs == null || parameters.vecs[0] == null) return new PointF();
        return parameters.vecs[0].Size;
    }

    public void setActiveTransports() {
        //if( TRP.routeStart==null )  // do not change active if route marked
        TRP_Collection.setAllowed(parameters.allowedTRPs);
        TRP_Collection.setActive(parameters.activeTRPs);
    }

    public Line getLine(int tNum, int lNum) {
        if (lines == null) return null;
        for (Line ll : lines)
            if (ll.trpNum == tNum && ll.lineNum == lNum) return ll;
        return null;
    }

    private StationsNum stationByPoint(float x, float y) {
        int st;

        for (Line ll : lines)
            if ((st = ll.stationByPoint(x, y)) != -1)
                return new StationsNum(ll.trpNum, ll.lineNum, st);

        return null;
    }

    private StationsNum[] stationsByPoint(float x, float y, int hitCircle) {  // todo  use it
        ArrayList<StationsNum> stns = new ArrayList<>();
        Integer[] st;

        for (Line ll : lines)
            if ((st = ll.stationsByPoint(x, y, hitCircle)) != null)
                for (Integer stn : st)
                    stns.add(new StationsNum(ll.trpNum, ll.lineNum, stn));

        if (stns.size() > 0) return stns.toArray(new StationsNum[stns.size()]);
        return null;
    } //*/

    public String singleTap(float x, float y, int hitCircle) {
        StationsNum ls;
        StationsNum[] stns = stationsByPoint(x, y, hitCircle);

        if (stns != null) {
            if (stns.length < 2) ls = stns[0];
            else {
                //MapActivity.mapActivity.mapView.menuStns = stns;
                //MapActivity.mapActivity.mapView.showContextMenu();
                MapActivity.mapActivity.showStationsMenu(stns);
                return null;
                /*ls=stns[1];
                String str="hits:";
                for( StationsNum stn : stns )
                    str = str + " " + stn.trp+","+ stn.line+","+ stn.stn;
                Log.e("MAP /225",str); */
            }
            MapActivity.mapActivity.mapView.selectStation(ls);
        } else {
            if (parameters.vecs == null || parameters.vecs.length == 0 || parameters.vecs[0] == null)
                return null;
            String action = parameters.vecs[0].SingleTap(x, y);  // todo   proceed all vecs
            if (action == null) {
                TRP_Collection.clearRoute();
            }

            return action;
        }
        return null;
    }

    public boolean doubleTap(float x, float y) {
        if (!MapData.isReady) return false;
        StationsNum ls = stationByPoint(x, y);
        if (ls == null) return false;

        Intent intent = new Intent(MapActivity.mapActivity, StationInfoActivity.class);
        intent.putExtra("trp", ls.trp);
        intent.putExtra("line", ls.line);
        intent.putExtra("station", ls.stn);
        MapActivity.mapActivity.startActivity(intent);
        return true;
    }

    public synchronized void Draw(Canvas canvas) {
        int s = canvas.save();

        DrawMAP(canvas);

        if (TRP_Collection.isRouteStartSelected() && TRP_Collection.isRouteEndSelected()) {   // greying map
            canvas.drawColor(0xb4ffffff);
        }

        if (TRP_Collection.routeExists()) {   // drawing route
            TRP_Collection.drawRoute(canvas, p);
        }

        if (TRP_Collection.isRouteEndSelected()) {   // mark end station
            TRP_Collection.drawEndStation(canvas, p, this);
        }
        if (TRP_Collection.isRouteStartSelected()) {  // mark start station and draw times
            TRP_Collection.drawStartStation(canvas, p, this);
        }
        canvas.restoreToCount(s);
    }

    private void DrawMAP(Canvas canvas) {
        //int s = canvas.save();
        //canvas.scale(scale, scale);           // VEC is not scaled
        for (VEC vv : parameters.vecs) vv.Draw(canvas);   // draw background
        //canvas.restoreToCount(s);

        if (!parameters.IsVector) {  // for pixel maps - draw times end exit
            if (TRP_Collection.isRouteStartSelected())
                for (Line ll : lines) ll.drawAllTexts(canvas);
            return;
        }

        p.setStyle(Paint.Style.STROKE);
        for (Line ll : lines)
            ll.DrawLines(canvas, p);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines)
            ll.DrawYellowStations(canvas, p);

        TRP_Collection.DrawTransfers(canvas, p, this);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines) ll.DrawStations(canvas, p);
        for (Line ll : lines) ll.DrawStationNames(canvas, p);
    }
}
