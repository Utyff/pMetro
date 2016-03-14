package com.utyf.pmetro.map;

import com.utyf.pmetro.util.StationsNum;

/**
 * Created by Utyf on 22.03.2015.
 *
 */

public class RouteNode extends StationsNum {
    public float  time;  // time arriving to station

    public RouteNode(StationsNum stn, float tm) {
        super( stn.trp, stn.line, stn.stn );
        time = tm;
    }

    public RouteNode(int t, int l, int s, float tm) {
        super( t, l, s );
        time = tm;
    }
}
