package com.utyf.pmetro.map;

import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtPointF;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;
import java.util.LinkedList;

// Stores information about current state of the map
public class TRP_Collection extends Parameters {

    private static TRP[] trpList; // all trp files
    private static int[] allowedTRPs;
    private static int[] activeTRPs;

    private static StationsNum routeStart, routeEnd;
    final static RouteTimes rt = new RouteTimes();
    private static Route bestRoute;
    private static Route[] alternativeRoutes;
    private static int alternativeRouteIndex = -1;

    private static Paint pline;

    static boolean loadAll() {
        routeStart = routeEnd = null;
        bestRoute = null;

        String[] names = zipMap.getFileList(".trp");
        if (names == null) return false;

        ArrayList<TRP> tl = new ArrayList<>();
        for (String nm : names) {
            TRP tt = new TRP();
            if (tt.load(nm) < 0) return false;
            tl.add(tt);
        }
        trpList = tl.toArray(new TRP[tl.size()]);

        allowedTRPs = null;
        activeTRPs = new int[trpList.length];
        clearActiveTRP();
        //MapActivity.mapActivity.setTRPMenu();

        for (TRP tt : trpList)    // set numbers of line and station for all transfers
            for (TRP.Transfer tr : tt.transfers)
                tr.setNums();

        return true;
    }

    public static void clearActiveTRP() {  // disable all TRP
        for (int i = 0; i < activeTRPs.length; i++) activeTRPs[i] = -1;
    }

    public static void setActive(int[] trpNums) {
        // Check if active transports have been modified
        int activeTRPcount = 0;
        for (int trp : activeTRPs) {
            if (trp != -1) activeTRPcount++;
        }
        boolean activeTRPchanged;
        if (activeTRPcount != trpNums.length) {
            activeTRPchanged = true;
        } else {
            activeTRPchanged = false;
            for (int trp : trpNums) {
                if (trp >= activeTRPs.length || activeTRPs[trp] == -1) {
                    activeTRPchanged = true;
                    break;
                }
            }
        }
        if (activeTRPchanged)
            alternativeRouteIndex = -1;

        clearActiveTRP();

        for (int tNum : trpNums) {
            if (isAllowed(tNum)) {
                activeTRPs[tNum] = tNum;
            }
        }
        synchronized (rt) {
            rt.createGraph();

            setStart(routeStart);
            setEnd(routeEnd);
        }
        MapActivity.mapActivity.setActiveTRP();
    }

    public static void checkActive() {  // remove disallowed from active
        for (int i = 0; i < activeTRPs.length; i++)
            if (!isAllowed(i)) activeTRPs[i] = -1;
    }

    public static boolean addActive(int trpNum) {
        if (!isAllowed(trpNum)) return false;
        checkActive();
        if (activeTRPs[trpNum] != trpNum)
            alternativeRouteIndex = -1;
        activeTRPs[trpNum] = trpNum;
        return true;
    }

    public static void removeActive(int trpNum) {
        checkActive();
        if (activeTRPs[trpNum] != -1)
            alternativeRouteIndex = -1;
        activeTRPs[trpNum] = -1;
    }

    public static boolean isActive(int trpNum) {
        return activeTRPs[trpNum] == trpNum;
    }

    public static boolean isAllowed(int trpNum) {
        if (allowedTRPs == null) return false;
        for (int num : allowedTRPs)
            if (num == trpNum) return true;
        return false;
    }

    public static void setAllowed(int[] ii) {
        allowedTRPs = ii;
        MapActivity.mapActivity.setAllowedTRP();
    }

    public static TRP getTRP(int trpNum) {
        if (trpNum < 0 || trpNum > trpList.length) return null;
        return trpList[trpNum];
    }

    public static int getSize() {
        return trpList.length;
    }

    public static int getTRPnum(String name) {
        if (trpList == null) return -1;
        for (int i = 0; i < trpList.length; i++)
            if (trpList[i].name.equals(name)) return i;
        return -1;
    }

    public static TRP.TRP_line getLine(String name) {
        for (TRP tt : trpList) {
            if (tt.lines == null) return null;
            for (TRP.TRP_line tl : tt.lines)
                if (tl.name.equals(name)) return tl;
        }
        return null;
    }

    public static TRP.TRP_line getLine(int tr, int ln) {
        TRP tt = trpList[tr];
        return tt.getLine(ln);
    }

    public static StationsNum getLineNum(String name) {
        for (int i = 0; i < trpList.length; i++) {
            if (trpList[i].lines == null) return null;
            for (int j = 0; j < trpList[i].lines.length; j++)
                if (trpList[i].lines[j].name.equals(name)) return new StationsNum(i, j, -1);
        }

        return null;
    }

    public static TRP.Transfer[] getTransfers(int trp, int line, int stn) {
        LinkedList<TRP.Transfer> listT = new LinkedList<>();
        int i = 0;

        for (TRP tt : trpList)
            for (TRP.Transfer trn : tt.transfers)
                if ((trn.trp1num == trp && trn.line1num == line && trn.st1num == stn)
                        || (trn.trp2num == trp && trn.line2num == line && trn.st2num == stn)) {
                    listT.add(trn);
                    i++;
                }

        if (i > 0)
            return listT.toArray(new TRP.Transfer[i]);

        return null;
    }

/*    public static TRP getTRP(String name)  {
        if( trpList==null ) return null;
        for( TRP tt : trpList )
            if( tt.name.equals(name) ) return tt;
        return null;
    } //*/

    public synchronized static void setStart(StationsNum ls) {
        // If routeStart is changed then alternative routes are possibly changed too
        if (routeStart != ls)
            alternativeRouteIndex = -1;

        routeStart = ls;
        if (routeStart != null && isActive(routeStart.trp)) {
            long tm = System.currentTimeMillis();
            calculateTimes(routeStart);
            Log.i("TRP", String.format("calculateTimes time: %d ms", System.currentTimeMillis() - tm));
        }
    }

    public synchronized static void setEnd(StationsNum ls) {
        // If routeEnd is changed then alternative routes are possibly changed too
        if (routeEnd != ls)
            alternativeRouteIndex = -1;

        routeEnd = ls;
        if (routeStart != null && routeEnd != null) {
            makeRoutes();
        }
    }

    // Recreates graph and calculates route
    public synchronized static void resetRoute() {
        final ProgressDialog progDialog = ProgressDialog.show(MapActivity.mapActivity, null, "Computing routes..", true);

        new Thread("Route computing") {
            public void run() {
                setPriority(MAX_PRIORITY);

                synchronized (rt) {
                    rt.createGraph();

                    alternativeRouteIndex = -1;

                    setStart(routeStart);
                    setEnd(routeEnd);
                }

                progDialog.dismiss();
                MapActivity.mapActivity.mapView.redraw();
            }
        }.start();
    }

    public static void redrawRoute() {
        makeRoutePaths();
        MapActivity.mapActivity.mapView.redraw();
    }

    private synchronized static void makeRoutes() {
        long tm = System.currentTimeMillis();

        bestRoute = null;
        synchronized (rt) {
            rt.setEnd(routeEnd);

            if (!isActive(routeStart.trp) || !isActive(routeEnd.trp))
                return; // stop if transport not active

            if (rt.getTime(routeEnd) == -1)
                return; // routeEnd is not reachable

            bestRoute = rt.getRoute();
        }

        alternativeRoutes = rt.getAlternativeRoutes(5, 10f);
        makeRoutePaths();

        MapActivity.makeRouteTime = System.currentTimeMillis() - tm;
        Log.i("TRP", String.format("makeRouteTime: %d ms", MapActivity.makeRouteTime));
    }

    private static void makeRoutePaths() {
        bestRoute.makePath();
        for (Route route : alternativeRoutes) {
            route.makePath();
        }
    }

    public static void calculateTimes(StationsNum start) {
        synchronized (rt) {
            rt.setStart(start);
        }
    }

    public static boolean isRouteStartSelected() {
        return routeStart != null;
    }

    public static boolean isRouteEndSelected() {
        return routeEnd != null;
    }

    public static boolean isRouteStartActive() {
        return isActive(TRP_Collection.routeStart.trp);
    }

    public static boolean routeExists() {
        return bestRoute != null;
    }

    public static void clearRoute() {
        routeStart = null;
        routeEnd = null;
        bestRoute = null;
    }

    public static Route[] getBestRoutes() {
        if (!routeExists())
            return new Route[0];

        // Append alternativeRoutes to bestRoute
        Route[] bestRoutes = new Route[1 + alternativeRoutes.length];
        bestRoutes[0] = bestRoute;
        System.arraycopy(alternativeRoutes, 0, bestRoutes, 1, alternativeRoutes.length);
        return bestRoutes;
    }

    public static void showBestRoute() {
        alternativeRouteIndex = -1;
        MapActivity.mapActivity.mapView.redraw();
    }

    public static void showAlternativeRoute(int index) {
        alternativeRouteIndex = index;
        MapActivity.mapActivity.mapView.redraw();
    }

    public static TRP.Transfer getTransfer(StationsNum ls1, StationsNum ls2) {

        for (TRP tt : trpList)
            for (TRP.Transfer trn : tt.transfers) {
                if ((trn.trp1num == ls1.trp && trn.line1num == ls1.line && trn.st1num == ls1.stn)
                        && (trn.trp2num == ls2.trp && trn.line2num == ls2.line && trn.st2num == ls2.stn))
                    return trn;
                if ((trn.trp1num == ls2.trp && trn.line1num == ls2.line && trn.st1num == ls2.stn)
                        && (trn.trp2num == ls1.trp && trn.line2num == ls1.line && trn.st2num == ls1.stn))
                    return trn;
            }

        return null;
    }

    //public static TRP_Station getStation(StationsNum ls)  {
    //    return trpList.get(ls.trp).getLine(ls.line).getStation(ls.stn);
    //}

    public static TRP.TRP_Station getStation(int t, int l, int s) {
        return trpList[t].getLine(l).getStation(s);
    }

    public static String getStationName(StationsNum ls) {
        return trpList[ls.trp].getLine(ls.line).getStationName(ls.stn);
    }

    public static synchronized void drawRoute(Canvas c, Paint p) {
        if (alternativeRouteIndex != -1) {
            if (alternativeRoutes != null) {
                alternativeRoutes[alternativeRouteIndex].Draw(c, p);
            }
        } else {
            if (bestRoute != null)
                bestRoute.Draw(c, p);
        }
    }

    public static void DrawTransfers(Canvas c, Paint p, MAP map) {
        PointF p1, p2;
        Line ll;
        if (pline == null) {
            pline = new Paint(p);
            pline.setStyle(Paint.Style.STROKE);
        }

        p.setColor(0xff000000);
        pline.setColor(0xff000000);
        pline.setStrokeWidth(map.LinesWidth + 6);
        for (int trpNum : allowedTRPs) {   // draw black edging
            if (trpNum == -1) continue;
            TRP ttt = getTRP(trpNum);
            if (ttt == null) continue;
            for (TRP.Transfer t : ttt.transfers) {
                if (t.invisible || !t.isCorrect()) continue;

                if ((ll = map.getLine(t.trp1num, t.line1num)) == null) continue;
                if (ExtPointF.isNull(p1 = ll.getCoord(t.st1num))) continue;

                if ((ll = map.getLine(t.trp2num, t.line2num)) == null) continue;
                if (ExtPointF.isNull(p2 = ll.getCoord(t.st2num))) continue;

                c.drawCircle(p1.x, p1.y, map.StationRadius + 3, p);
                c.drawCircle(p2.x, p2.y, map.StationRadius + 3, p);
                c.drawLine(p1.x, p1.y, p2.x, p2.y, pline);
            }
        }

        p.setColor(0xffffffff);
        pline.setColor(0xffffffff);
        pline.setStrokeWidth(map.LinesWidth + 4);
        for (int trpNum : allowedTRPs) {   // draw white transfer
            if (trpNum == -1) continue;
            TRP ttt = getTRP(trpNum);
            if (ttt == null) continue;
            for (TRP.Transfer t : ttt.transfers) {
                if (t.invisible || !t.isCorrect()) continue;

                if ((ll = map.getLine(t.trp1num, t.line1num)) == null) continue;
                if (ExtPointF.isNull(p1 = ll.getCoord(t.st1num))) continue;

                if ((ll = map.getLine(t.trp2num, t.line2num)) == null) continue;
                if (ExtPointF.isNull(p2 = ll.getCoord(t.st2num))) continue;

                c.drawCircle(p1.x, p1.y, map.StationRadius + 2, p);
                c.drawCircle(p2.x, p2.y, map.StationRadius + 2, p);
                c.drawLine(p1.x, p1.y, p2.x, p2.y, pline);
            }
        }
    }

    public static void drawEndStation(Canvas canvas, Paint p, MAP map) {
        PointF pnt;
        Line   ll;
        ll = map.getLine(routeEnd.trp,routeEnd.line);
        if( ll!=null && !ExtPointF.isNull(pnt=ll.getCoord(routeEnd.stn)) ) {
            p.setARGB(255, 11, 5, 203);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pnt.x, pnt.y, map.StationRadius, p);
            p.setARGB(255, 240, 40, 200);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(map.StationRadius/2.5f);
            canvas.drawCircle(pnt.x, pnt.y, map.StationRadius*0.875f, p);
            ll.drawText(canvas,routeEnd.stn);
        }
    }

    public static void drawStartStation(Canvas canvas, Paint p, MAP map) {
        PointF pnt;
        Line   ll;
        ll = map.getLine(routeStart.trp,routeStart.line);
        if( ll!=null && !ExtPointF.isNull(pnt=ll.getCoord(routeStart.stn)) ) {
            p.setARGB(255, 10, 133, 26);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pnt.x, pnt.y, map.StationRadius, p);
            p.setARGB(255, 240, 40, 200);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(map.StationRadius/2.5f);
            canvas.drawCircle(pnt.x, pnt.y, map.StationRadius*0.875f, p);
            ll.drawText(canvas,routeStart.stn);
        }
    }
}
