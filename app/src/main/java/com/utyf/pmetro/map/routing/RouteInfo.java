package com.utyf.pmetro.map.routing;

import com.utyf.pmetro.util.StationsNum;

/**
 * Represents computed route as list of stations
 *
 * @author Fedor
 */
public class RouteInfo {
    private StationsNum[] stations;
    private float time;

    public RouteInfo(StationsNum[] stations, float time) {
        this.stations = stations;
        this.time = time;
    }

    public StationsNum[] getStations() {
        return stations;
    }

    public float getTime() {
        return time;
    }
}
