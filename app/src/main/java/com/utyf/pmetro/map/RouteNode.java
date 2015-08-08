package com.utyf.pmetro.map;

import com.utyf.pmetro.util.StationsNum;

/**
 * Created by Utyf on 22.03.2015.
 *
 */

public class RouteNode extends StationsNum {
    public int    direction;    // 1 - forward, -1 - backward,  0 - both (not for route)
    public float  time, delay;  // time arriving to station, delay for next node

    public RouteNode(StationsNum stn, float tm, boolean del, int dir) {
        super( stn.trp, stn.line, stn.stn );
        time = tm;
        if( del ) //noinspection ConstantConditions
            delay = TRP.getTRP(trp).getLine(line).delays.get();
        direction = dir;
    }

    public RouteNode(int t, int l, int s, float tm, boolean del, int dir) {
        super( t, l, s );
        time = tm;
        if( del ) //noinspection ConstantConditions
            delay = TRP.getTRP(trp).getLine(line).delays.get();
        direction = dir;
    }
}
