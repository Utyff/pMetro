package com.utyf.pmetro.map;

import android.graphics.Canvas;
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

    public static CTY  cty;
    public static Info info;
    public static boolean isReady, loading;

    private static ArrayList<TouchView.viewState> mapStack;
    public static MAP map, mapMetro;

    public static synchronized int Load()
    {
        isReady = false;
        loading = true;

        new Thread("Load map") {
            @Override
            public void run() {

                try {
                    //long loadTime = System.currentTimeMillis();
                    if( !zipMap.load() ) throw new Exception();
                    //Log.e("Load zip map","time - " + (System.currentTimeMillis()-loadTime));

                    cty = new CTY();
                    if( cty.Load()<0 )   throw new Exception();

                    if( !TRP.loadAll() ) throw new Exception();

                    mapStack = new ArrayList<>();
                    map = new MAP();
                    if( map.load("Metro.map")<0 )  throw new Exception();  // loading Metro.map
                    mapMetro = map;                                    // set Metro.map as main

                    info = new Info();
                    info.load();

                    loading = false;
                    isReady = true;

                    MapActivity.mapActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            MapActivity.mapActivity.setMenu();
                        }
                    });
                } catch (Exception e) {
                    mapMetro = map = null;
                    loading=false;
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

    public static boolean mapBack() {
        TouchView.viewState vs;
        if( isReady && mapStack.size()!=0 )
            synchronized( MapData.class ) {
                vs = mapStack.get(mapStack.size()-1);
                if( mapStack.size()==1 )  map = mapMetro;
                else   map.load( vs.name );
                mapStack.remove(mapStack.size() - 1);

                map.setActiveTransports();
                TRP.resetRoute(); // redraw route

                MapActivity.mapActivity.mapView.contentChanged(vs);

                return true;
            }
        return false;
    }

    public static void singleTap(float x, float y, int hitCircle) {
        if( !isReady ) return;

        String action;
        TouchView.viewState vs;
        if( (action=map.singleTap(x,y,hitCircle))!=null )  {
            String[] strs = action.split(" ");
            if( strs[0].toLowerCase().equals("loadmap") )
                synchronized( MapData.class ) {
                    vs = MapActivity.mapActivity.mapView.getState();
                    vs.name = map.name;
                    mapStack.add(vs);
                    if( map.name.equals("Metro.map") )  map = new MAP();
                    if( map.load(strs[1])<0 ) {
                        map = null;
                        Toast.makeText(MapActivity.mapActivity, "Can`t load map.", Toast.LENGTH_LONG).show();
                    } else   TRP.resetRoute(); // redraw route
                    MapActivity.mapActivity.mapView.contentChanged(null);
                }
        }
    }

    public static synchronized void draw(Canvas c) {
        map.Draw(c);
    }
}
