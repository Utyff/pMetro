package com.utyf.pmetro;

import android.app.Fragment;
import android.os.Bundle;

import com.utyf.pmetro.map.MapData;

import java.util.ArrayList;

/**
 * Stores map data and preserves it through configuration changes
 */
public class MapDataFragment extends Fragment {
    private MapData mapData;
    private ArrayList<MapDataCallback> listeners;
    private boolean isLoadingStarted;

    public MapDataFragment() {
        mapData = null;
        listeners = new ArrayList<>();
        isLoadingStarted = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Do not load mapData if it has been previously loaded, but a new activity is created,
        // e.g. because configuration has been changed
        boolean needLoading = false;
        synchronized (this) {
            if (!isLoadingStarted && mapData == null) {
                isLoadingStarted = true;
                needLoading = true;
            }
        }
        if (needLoading) {
            MapData.loadAsync(new MapData.LoadCallback() {
                @Override
                public void onMapDataLoaded(MapData mapData) {
                    MapDataFragment.this.onMapDataLoaded(mapData);
                }

                @Override
                public void onMapDataFailed() {
                    MapDataFragment.this.onMapDataFailed();
                }
            });
        }
    }

    private void onMapDataFailed() {
        ArrayList<MapDataCallback> unprocessedHandlers;
        synchronized (this) {
            unprocessedHandlers = new ArrayList<>(listeners);
            listeners.clear();
            isLoadingStarted = false;
        }
        for (MapDataCallback handler : unprocessedHandlers) {
            handler.onMapDataFailed();
        }
    }

    private void onMapDataLoaded(MapData mapData) {
        ArrayList<MapDataCallback> unprocessedListeners;
        MapData mapDataCopy;
        synchronized (this) {
            this.mapData = mapData;
            unprocessedListeners = new ArrayList<>(listeners);
            listeners.clear();
            mapDataCopy = mapData;
        }
        for (MapDataCallback handler : unprocessedListeners) {
            handler.onMapDataLoaded(mapDataCopy);
        }
    }

    public void getMapDataAsync(MapDataCallback listener) {
        MapData mapDataCopy;
        synchronized (this) {
            if (mapData != null) {
                mapDataCopy = mapData;
            }
            else {
                listeners.add(listener);
                mapDataCopy = null;
            }
        }
        if (mapDataCopy != null) {
            listener.onMapDataLoaded(mapDataCopy);
        }
    }

    public interface MapDataCallback {
        void onMapDataLoaded(MapData mapData);
        void onMapDataFailed();
    }
}
