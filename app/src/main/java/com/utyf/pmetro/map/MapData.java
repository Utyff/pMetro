package com.utyf.pmetro.map;

import android.graphics.Canvas;
import android.util.Log;
import android.widget.Toast;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.map.routing.RoutingState;
import com.utyf.pmetro.util.TouchView;
import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


public class MapData {

    public final CTY cty;
    public final Info info;
    public final TRP_Collection transports;
    public final RoutingState routingState;

    private ArrayList<TouchView.viewState> mapStack;
    public MAP map, mapMetro;

    private MapData(CTY cty, Info info, TRP_Collection transports, RoutingState routingState) {
        this.cty = cty;
        this.info = info;
        this.transports = transports;
        this.routingState = routingState;
        mapStack = new ArrayList<>();
    }

    public static void loadAsync(final LoadCallback handler) {
        new Thread("Load map") {
            @Override
            public void run() {
                try {
                    //long loadTime = System.currentTimeMillis();
                    if (!zipMap.load())
                        throw new Exception("Cannot load zip archive");
                    //Log.e("Load zip map","time - " + (System.currentTimeMillis()-loadTime));

                    CTY cty = new CTY();
                    if (cty.Load() < 0)
                        throw new Exception("Cannot load .cty file");

                    TRP_Collection transports = TRP_Collection.loadAll();
                    if (transports == null)
                        throw new Exception("Cannot load .trp files");
                    RoutingState routingState = new RoutingState(transports);

                    Info info = new Info();
                    info.load();

                    MapData mapData = new MapData(cty, info, transports, routingState);

                    MAP map = new MAP(mapData);
                    if (map.load("Metro.map") < 0)
                        throw new Exception("Cannot load .map file");

                    mapData.map = map;
                    mapData.mapMetro = map;  // set Metro.map as main

                    handler.onMapDataLoaded(mapData);
                } catch (Exception e) {
                    Log.e("MapData", String.format("Exception caught!\n%s", e.toString()));
                    e.printStackTrace();
                    handler.onMapDataFailed();
                }
            }
        }.start();
    }

    public interface LoadCallback {
        void onMapDataLoaded(MapData mapData);
        void onMapDataFailed();
    }

    public boolean mapBack() {
        TouchView.viewState vs;
        if (mapStack.size() != 0)
            synchronized( MapData.class ) {
                vs = mapStack.get(mapStack.size()-1);
                if( mapStack.size()==1 )  map = mapMetro;
                else   map.load( vs.name );
                mapStack.remove(mapStack.size() - 1);

                if (!routingState.routeExists()) {
                    map.setActiveTransports();
                }
                map.redrawRoute();

                MapActivity.mapActivity.mapView.contentChanged(vs);
                routingState.updateRoute();

                return true;
            }
        return false;
    }

    public void singleTap(float x, float y, int hitCircle, boolean isLongTap) {
        String action;
        TouchView.viewState vs;
        if ((action = map.singleTap(x, y, hitCircle, isLongTap)) != null) {
            String[] strs = action.split(" ");
            if( strs[0].toLowerCase().equals("loadmap") )
                synchronized( MapData.class ) {
                    vs = MapActivity.mapActivity.mapView.getState();
                    vs.name = map.parameters.name;
                    mapStack.add(vs);
                    if( map.parameters.name.equals("Metro.map") ) {
                        map = new MAP(this);
                    }
                    if( map.load(strs[1])<0 ) {
                        map = null;
                        Toast.makeText(MapActivity.mapActivity, "Can`t load map.", Toast.LENGTH_LONG).show();
                    } else {
                        map.redrawRoute();
                    }
                    MapActivity.mapActivity.mapView.contentChanged(null);
                    routingState.updateRoute();
                }
        }
    }

    public synchronized void draw(Canvas c) {
        map.Draw(c);
    }
}
