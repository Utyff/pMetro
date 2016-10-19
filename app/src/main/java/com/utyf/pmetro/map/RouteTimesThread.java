package com.utyf.pmetro.map;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.StationsNum;

import java.util.BitSet;

/**
 * Background thread, which handles all operations on RouteTimes
 */
class RouteTimesThread extends HandlerThread {
    private Handler handler;
    private RouteTimes rt;

    RouteTimesThread() {
        super("Routing background thread", 4);  // use lower priority
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        handler = new Handler(getLooper());
    }

    public void doWork(Runnable r) {
        // Make sure that handler is created, because doWork can potentially be called
        // before onLooperPrepared
        if (handler == null) {
            handler = new Handler(getLooper());
        }
        handler.post(r);
    }

    public void calculateTimes(StationsNum start, final CalculateTimesCallback callback) {
        final StationsNum startCopy = start;
        doWork(new Runnable() {
            @Override
            public void run() {
                Log.i("TRP", "start setStart");
                rt.setStart(startCopy);
                Log.i("TRP", "finish setStart");
                callback.onComputingTimesStarted();
                Log.i("TRP", "start calculateTimes");
                long tm = System.currentTimeMillis();
                rt.computeShortestPaths(new RouteTimes.Callback() {
                    @Override
                    public void onShortestPathsComputed(StationsNum[] stationNums, float[] stationTimes) {
                        callback.onComputingTimesProgress(stationNums, stationTimes);
                    }
                });
                Log.i("TRP", String.format("calculateTimes time: %d ms", System.currentTimeMillis() - tm));
                callback.onComputingTimesFinished();
            }
        });
    }

    public void makeRoutes(StationsNum routeEnd, final MakeRoutesCallback callback) {
        final StationsNum routeEndCopy = routeEnd;
        doWork(new Runnable() {
            @Override
            public void run() {
                callback.onMakeRoutesStarted();

                boolean ok = true;

                if (rt.getTime(routeEndCopy) == -1) {
                    ok = false; // routeEnd is not reachable
                }

                Routes routes = null;
                if (ok) {
                    rt.setEnd(routeEndCopy);

                    long tm = System.currentTimeMillis();

                    RouteInfo bestRoute = rt.getRoute();
                    RouteInfo[] alternativeRoutes = rt.getAlternativeRoutes(5, 10f);
                    routes = new Routes(bestRoute, alternativeRoutes);

                    MapActivity.makeRouteTime = System.currentTimeMillis() - tm;
                    Log.i("TRP", String.format("makeRouteTime: %d ms", MapActivity.makeRouteTime));
                }

                callback.onMakeRoutesCompleted(routes);
            }
        });
    }

    public void createGraph(TRP_Collection transports, BitSet activeTRPs) {
        final TRP_Collection transportsCopy = transports;
        final BitSet activeTRPsCopy = activeTRPs;
        doWork(new Runnable() {
            @Override
            public void run() {
                Log.i("TRP", "start createGraph");
                long tm = System.currentTimeMillis();
                rt = new RouteTimes(transportsCopy, activeTRPsCopy);
                rt.createGraph();
                Log.i("TRP", String.format("createGraph time: %d ms", System.currentTimeMillis() - tm));
            }
        });
    }

    interface CalculateTimesCallback {
        void onComputingTimesStarted();
        void onComputingTimesProgress(StationsNum[] stationNums, float[] stationTimes);
        void onComputingTimesFinished();
    }

    interface MakeRoutesCallback {
        void onMakeRoutesStarted();
        void onMakeRoutesCompleted(Routes routes);
    }
}
