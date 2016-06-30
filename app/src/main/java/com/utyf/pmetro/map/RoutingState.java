package com.utyf.pmetro.map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtPointF;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Stores current state of map, which relates to routing, such as start and end station and selected
 * routing preferences
 */

public class RoutingState {
    /** Set of currently active transports */
    private BitSet activeTRPs;
    /** Selected start station or null */
    private StationsNum routeStart;
    /** Selected end station or null */
    private StationsNum routeEnd;
    /** Set of computed routes from #routeStart to #routeEnd or null if no route exists */
    private Routes routes;

    private TRP_Collection transports;
    private RouteTimes rt;  // Must be accessed only from backgroundThread

    private ArrayList<Listener> listeners;
    private final BackgroundThread backgroundThread;

    public RoutingState(TRP_Collection transports) {
        this.transports = transports;

        routeStart = routeEnd = null;
        routes = null;

        listeners = new ArrayList<>();
        backgroundThread = new BackgroundThread();
        backgroundThread.start();
    }

    static class BackgroundThread extends HandlerThread {
        private Handler handler;

        BackgroundThread() {
            super("Routing background thread", 4);  // use lower priority
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            handler = new Handler(getLooper());
        }

        public void doWork(Runnable r) {
            handler.post(r);
        }
    }

    // Must be called after TRP_Collection.loadAll
    public void load() {
        activeTRPs = new BitSet(transports.getSize());
    }

    /** Set active transports equal to allowed transports */
    public void resetActiveTransports() {
        activeTRPs = new BitSet(transports.getSize());
        for (int trpNum = 0; trpNum < transports.getSize(); trpNum++) {
            if (transports.isAllowed(trpNum))
                activeTRPs.set(trpNum);
        }
    }

    public boolean addActive(int trpNum) {
        if (!transports.isAllowed(trpNum)) {
            Log.w("RoutingState", "Trying to set not allowed transport " + trpNum);
            return false;
        }
        activeTRPs.set(trpNum);
        return true;
    }

    public void removeActive(int trpNum) {
        if (activeTRPs.get(trpNum))
            routes = null;
        activeTRPs.clear(trpNum);
    }

    public boolean isActive(int trpNum) {
        return activeTRPs.get(trpNum);
    }

    /** Sets start station of the route and updates RoutingState */
    public void setStart(StationsNum ls) {
        // If routeStart is changed then alternative routes are possibly changed too
        if (routeStart != ls)
            routes = null;

        routeStart = ls;
        if (routeStart != null && isActive(routeStart.trp)) {
            calculateTimes(routeStart);
        }
    }

    /** Sets end station of the route and updates RoutingState */
    public void setEnd(StationsNum ls) {
        // If routeEnd is changed then alternative routes are possibly changed too
        if (routeEnd != ls)
            routes = null;

        routeEnd = ls;
        if (routeStart != null && routeEnd != null) {
            makeRoutes();
        }
    }

    private void calculateTimes(StationsNum start) {
        final StationsNum startCopy = start;
        backgroundThread.doWork(new Runnable() {
            @Override
            public void run() {
                Log.i("TRP", "start setStart");
                rt.setStart(startCopy);
                Log.i("TRP", "finish setStart");
                for (final Listener listener: listeners) {
                    listener.onComputingTimesStarted();
                }
                Log.i("TRP", "start calculateTimes");
                long tm = System.currentTimeMillis();
                rt.computeShortestPaths(new RouteTimes.Callback() {
                    @Override
                    public void onShortestPathsComputed(List<Pair<StationsNum, Float>> stationTimes) {
                        for (final Listener listener: listeners) {
                            listener.onComputingTimesProgress(stationTimes);
                        }
                    }
                });
                Log.i("TRP", String.format("calculateTimes time: %d ms", System.currentTimeMillis() - tm));
                for (final Listener listener: listeners) {
                    listener.onComputingTimesFinished();
                }
            }
        });
    }

    /** Recreates graph and calculates route */
    public void resetRoute() {
        createGraph();

        routes = null;

        if (routeStart != null && isActive(routeStart.trp)) {
            calculateTimes(routeStart);
        }
        if (routeStart != null && routeEnd != null) {
            makeRoutes();
        }
    }

    public void drawEndStation(Canvas canvas, Paint p, MAP map) {
        PointF pnt;
        Line   ll;
        ll = map.getLine(routeEnd.trp,routeEnd.line);
        if( ll!=null && !ExtPointF.isNull(pnt=ll.getCoord(routeEnd.stn)) ) {
            p.setARGB(255, 11, 5, 203);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pnt.x, pnt.y, map.parameters.StationRadius, p);
            p.setARGB(255, 240, 40, 200);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(map.parameters.StationRadius/2.5f);
            canvas.drawCircle(pnt.x, pnt.y, map.parameters.StationRadius*0.875f, p);
            ll.drawText(canvas,routeEnd.stn);
        }
    }

    public void drawStartStation(Canvas canvas, Paint p, MAP map) {
        PointF pnt;
        Line   ll;
        ll = map.getLine(routeStart.trp,routeStart.line);
        if( ll!=null && !ExtPointF.isNull(pnt=ll.getCoord(routeStart.stn)) ) {
            p.setARGB(255, 10, 133, 26);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pnt.x, pnt.y, map.parameters.StationRadius, p);
            p.setARGB(255, 240, 40, 200);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(map.parameters.StationRadius/2.5f);
            canvas.drawCircle(pnt.x, pnt.y, map.parameters.StationRadius*0.875f, p);
            ll.drawText(canvas,routeStart.stn);
        }
    }

    private void makeRoutes() {
        backgroundThread.doWork(new Runnable() {
            @Override
            public void run() {
                for (final Listener listener: listeners) {
                    listener.onComputingRoutesStarted();
                }

                boolean ok = true;
                if (!isActive(routeStart.trp) || !isActive(routeEnd.trp)) {
                    ok = false; // stop if transport not active
                }

                if (rt.getTime(routeEnd) == -1) {
                    ok = false; // routeEnd is not reachable
                }

                if (ok) {
                    rt.setEnd(routeEnd);

                    long tm = System.currentTimeMillis();

                    RouteInfo bestRoute = rt.getRoute();
                    RouteInfo[] alternativeRoutes = rt.getAlternativeRoutes(5, 10f);
                    routes = new Routes(bestRoute, alternativeRoutes);

                    MapActivity.makeRouteTime = System.currentTimeMillis() - tm;
                    Log.i("TRP", String.format("makeRouteTime: %d ms", MapActivity.makeRouteTime));
                }

                for (final Listener listener: listeners) {
                    listener.onComputingRoutesFinished(getBestRoutes());
                }
            }
        });
    }

    public boolean isRouteStartSelected() {
        return routeStart != null;
    }

    public boolean isRouteEndSelected() {
        return routeEnd != null;
    }

    public boolean isRouteStartActive() {
        return isActive(routeStart.trp);
    }

    /** Returns true if the route was found between selected points */
    public boolean routeExists() {
        return routes != null;
    }

    /** Cancels selection of the route */
    public void clearRoute() {
        routeStart = null;
        routeEnd = null;
        routes = null;
    }

    private RouteInfo[] getBestRoutes() {
        if (!routeExists())
            return new RouteInfo[0];

        return routes.getAllRoutes();
    }

    public void updateRoute() {
        if (!routeExists())
            return;

        for (Listener listener : listeners) {
            listener.onRouteSelected(routes.getCurrentRoute());
        }
        MapActivity.mapActivity.mapView.redraw();
    }

    public void showBestRoute() {
        routes.selectBestRoute();
        for (Listener listener : listeners) {
            listener.onRouteSelected(routes.getCurrentRoute());
        }
        MapActivity.mapActivity.mapView.redraw();
    }

    public void showAlternativeRoute(int index) {
        routes.selectAlternativeRoute(index);
        for (Listener listener : listeners) {
            listener.onRouteSelected(routes.getCurrentRoute());
        }
        MapActivity.mapActivity.mapView.redraw();
    }

    private void createGraph() {
        backgroundThread.doWork(new Runnable() {
            @Override
            public void run() {
                Log.i("TRP", "start createGraph");
                long tm = System.currentTimeMillis();
                rt = new RouteTimes(transports, activeTRPs);
                rt.createGraph();
                Log.i("TRP", String.format("createGraph time: %d ms", System.currentTimeMillis() - tm));
            }
        });
    }

    public float getTime(int trpNum, int lineNum, int stNum) {
        return rt.getTime(trpNum, lineNum, stNum);
    }

    public interface Listener {
        void onComputingTimesStarted();
        void onComputingTimesProgress(final List<Pair<StationsNum, Float>> stationTimes);
        void onComputingTimesFinished();
        void onComputingRoutesStarted();
        void onComputingRoutesFinished(final RouteInfo[] bestRoutes);
        void onRouteSelected(final RouteInfo route);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void close() {
        backgroundThread.quit();
    }
}
