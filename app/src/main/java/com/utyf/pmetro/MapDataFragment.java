package com.utyf.pmetro;

import android.app.Fragment;
import android.os.Bundle;

import com.utyf.pmetro.map.MapData;

/**
 * Stores map data and preserves it through configuration changes
 */
public class MapDataFragment extends Fragment {
    private MapData mapData;

    public MapDataFragment() {
        mapData = new MapData();
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
        if (!mapData.getIsReady()) {
            mapData.Load();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapData.routingState.close();
    }

    public MapData getMapData() {
        return mapData;
    }
}
