package com.utyf.pmetro.map;

import android.graphics.Canvas;
import android.util.Log;
import android.widget.Toast;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.TouchView;
import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


public class MapData {

    public CTY cty;
    public Info info;
    public TRP_Collection transports;
    public RouteTimes rt;
    // TODO: 11.06.2016 Make isReady and loading private. Make sure that they are properly set, especially when loading MAP_Parameters
    public boolean isReady, isLoading;

    private ArrayList<TouchView.viewState> mapStack;
    public MAP map, mapMetro;

    public MapData() {
        cty = new CTY();
        info = new Info();
        transports = new TRP_Collection();
        rt = new RouteTimes(transports);
    }

    public synchronized int Load() {
        isReady = false;
        isLoading = true;

        map = new MAP(this);

        new Thread("Load map") {
            @Override
            public void run() {

                try {
                    //long loadTime = System.currentTimeMillis();
                    if( !zipMap.load() ) throw new Exception();
                    //Log.e("Load zip map","time - " + (System.currentTimeMillis()-loadTime));

                    if( cty.Load()<0 )   throw new Exception();

                    if( !transports.loadAll() ) throw new Exception();

                    mapStack = new ArrayList<>();
                    if( map.load("Metro.map")<0 )  throw new Exception();  // loading Metro.map
                    mapMetro = map;                                    // set Metro.map as main

                    info.load();

                    isLoading = false;
                    isReady = true;

                    MapActivity.mapActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            MapActivity.mapActivity.setMenu();
                        }
                    });
                } catch (Exception e) {
                    Log.e("MapData", String.format("Exception caught!\n%s", e.toString()));
                    e.printStackTrace();
                    mapMetro = map = null;
                    isLoading =false;
                    MapActivity.mapActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            MapActivity.mapActivity.loadFail();
                        }
                    });
                }
                MapActivity.mapActivity.mapView.contentChanged(null);
            }
        }.start();

        return 0;
    }

    public boolean mapBack() {
        TouchView.viewState vs;
        if( getIsReady() && mapStack.size()!=0 )
            synchronized( MapData.class ) {
                vs = mapStack.get(mapStack.size()-1);
                if( mapStack.size()==1 )  map = mapMetro;
                else   map.load( vs.name );
                mapStack.remove(mapStack.size() - 1);

                map.setActiveTransports();
                transports.redrawRoute();

                MapActivity.mapActivity.mapView.contentChanged(vs);

                return true;
            }
        return false;
    }

    public void singleTap(float x, float y, int hitCircle) {
        if( !getIsReady() ) return;

        String action;
        TouchView.viewState vs;
        if( (action=map.singleTap(x,y,hitCircle))!=null )  {
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
                        transports.redrawRoute();
                    }
                    MapActivity.mapActivity.mapView.contentChanged(null);
                }
        }
    }

    public synchronized void draw(Canvas c) {
        map.Draw(c);
    }

    public boolean getIsLoading() {
        return isLoading;
    }

    public boolean getIsReady() {
        return isReady;
    }
}
