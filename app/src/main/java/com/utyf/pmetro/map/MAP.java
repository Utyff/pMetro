package com.utyf.pmetro.map;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.map.routing.RouteInfo;
import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;

/**
 * Displays metro map
 *
 * @author Utyf
 */

public class MAP {
    public MAP_Parameters parameters;
    private MapData mapData;

    private Line[] lines;
    private Route route;
    Paint p;

    public MAP(MapData mapData) {
        this.mapData = mapData;
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);  // todo switch by settings
    }

    public int load(String name) {
        parameters = new MAP_Parameters();
        MAP_Parameters defaultParameters = mapData.mapMetro != null ? mapData.mapMetro.parameters : null;
        if (parameters.load(name, defaultParameters, mapData.transports) < 0) {
            return -1;
        }
        setActiveTransports();

        lines = new Line[parameters.la.size()];
        for (int i = 0; i < parameters.la.size(); i++)
            lines[i] = new Line(parameters.la.get(i), parameters, mapData);

        for (Line l : lines) l.CreatePath(); // create line path for drawing

        return 0;
    }

    public PointF getSize() {  // todo   adjust to all vecs
        if (parameters.vecs == null || parameters.vecs[0] == null) return new PointF();
        return parameters.vecs[0].Size;
    }

    public void setActiveTransports() {
        //if( TRP.routeStart==null )  // do not change active if route marked
        mapData.routingState.setActive(parameters.allowedTRPs);
        MapActivity.mapActivity.setActiveTRP();

        mapData.routingState.resetRoute();
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
                mapData.routingState.clearRoute();
                clearStationTimes();
            }

            return action;
        }
        return null;
    }

    public boolean doubleTap(float x, float y) {
        StationsNum ls = stationByPoint(x, y);
        if (ls == null) return false;

        StationData stationData = new StationData();
        stationData.load(ls, mapData);

        Intent intent = new Intent(MapActivity.mapActivity, StationInfoActivity.class);
        intent.putExtra("stationData", stationData);
        MapActivity.mapActivity.startActivity(intent);
        return true;
    }

    public synchronized void Draw(Canvas canvas) {
        int s = canvas.save();

        DrawMAP(canvas);

        if (mapData.routingState.isRouteStartSelected() && mapData.routingState.isRouteEndSelected()) {   // greying map
            canvas.drawColor(0xb4ffffff);
        }

        drawRoute(canvas, p);   // drawing route

        if (mapData.routingState.isRouteEndSelected()) {   // mark end station
            mapData.routingState.drawEndStation(canvas, p, this);
        }
        if (mapData.routingState.isRouteStartSelected()) {  // mark start station and draw times
            mapData.routingState.drawStartStation(canvas, p, this);
        }
        canvas.restoreToCount(s);
    }

    public void createRoute(RouteInfo routeInfo) {
        route = new Route(mapData);
        StationsNum[] stations = routeInfo.getStations();
        for (StationsNum station : stations) {
            route.addNode(station);
        }
        route.makePath();
    }

    public void clearRoute() {
        route = null;
    }

    private void drawRoute(Canvas canvas, Paint paint) {
        if (route != null) {
            route.Draw(canvas, paint);
        }
    }

    private void DrawMAP(Canvas canvas) {
        //int s = canvas.save();
        //canvas.scale(scale, scale);           // VEC is not scaled
        for (VEC vv : parameters.vecs) vv.Draw(canvas);   // draw background
        //canvas.restoreToCount(s);

        if (!parameters.IsVector) {  // for pixel maps - draw times end exit
            if (mapData.routingState.isRouteStartSelected())
                for (Line ll : lines) ll.drawAllTexts(canvas);
            return;
        }

        p.setStyle(Paint.Style.STROKE);
        for (Line ll : lines)
            ll.DrawLines(canvas, p);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines)
            ll.DrawYellowStations(canvas, p);

        mapData.transports.DrawTransfers(canvas, p, this);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines) ll.DrawStations(canvas, p);
        for (Line ll : lines) ll.DrawStationNames(canvas, p);
    }

    public void redrawRoute() {
        if (route != null) {
            route.makePath();
        }
    }

    public void setStationTimes(StationsNum[] stationNums, float[] stationTimes) {
//        Map<StationsNum, Float> stationTimesMap = new TreeMap<>(new Comparator<StationsNum>() {
//            @Override
//            public int compare(StationsNum lhs, StationsNum rhs) {
//                if (lhs.trp != rhs.trp) {
//                    return lhs.trp - rhs.trp;
//                }
//                else if (lhs.line != rhs.line) {
//                    return lhs.line - rhs.line;
//                }
//                else {
//                    return lhs.stn - rhs.stn;
//                }
//            }
//        });
//        for (Pair<StationsNum, Float> entry: stationTimes) {
//            stationTimesMap.put(entry.first, entry.second);
//        }
//        for (Line line: lines) {
//            TRP.TRP_line trp_line = mapData.transports.getLine(line.trpNum, line.lineNum);
//            int stationCount = trp_line.Stations.length;
//            for (int stn = 0; stn < stationCount; stn++) {
//                StationsNum stationsNum = new StationsNum(line.trpNum, line.lineNum, stn);
//                Float stationTime = stationTimesMap.get(stationsNum);
//                if (stationTime != null) {
//                    line.setStationTime(stn, stationTime);
//                } else {
//                    line.setStationTime(stn, -1);
//                }
//            }
//        }
        for (int i = 0; i < stationNums.length; i++) {
            StationsNum num = stationNums[i];
            Float time = stationTimes[i];
            Line line = getLine(num.trp, num.line);
            if (line != null) {
                line.setStationTime(num.stn, time);
            }
        }
    }

    public void clearStationTimes() {
        for (Line line: lines) {
            line.clearStationTimes();
        }
    }
}
