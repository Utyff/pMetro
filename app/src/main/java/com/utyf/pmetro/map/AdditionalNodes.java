package com.utyf.pmetro.map;

import android.graphics.PointF;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;

/**
 * Loads and parses information about additional nodes from .map files
 * <p/>
 * Additional nodes are used to define detailed geometry of metro lines by using polygons or splines.
 *
 * @author Utyf
 */

class AdditionalNodes {
    PointF[] points;
    int numSt1, numSt2;
    boolean spline;

    /**
     * Format of text file line with AdditionalNodes entry:
     * </p>
     * line, station1, station2, x1, y1, x2, y2, ..., xn, yn, [spline]
     * @param strings List of strings, must contain at least 5 elements
     * @param transports TRP_Collection to get station indices from their names
     */
    public AdditionalNodes(String[] strings, TRP_Collection transports) {
        // TODO: 17.06.2016 Pass strings as a single string and parse it inside of AdditionalNodes
        String st1 = strings[1].trim();
        String st2 = strings[2].trim();
        TRP_line tl = transports.getLine(strings[0]);
        if (tl != null) {
            numSt1 = tl.getStationNum(st1);
            numSt2 = tl.getStationNum(st2);
        } else {
            numSt1 = -1;
            numSt2 = -1;
            Log.e("AddNodes /31", "Wrong line name");
            return;
        }

        points = new PointF[(strings.length - 3) / 2];

        int p = 3;
        int pointCount = 0;
        while (p + 1 < strings.length) {
            float x = ExtFloat.parseFloat(strings[p++]);
            float y = ExtFloat.parseFloat(strings[p++]);
            points[pointCount++] = new PointF(x, y);
        }

        spline = p < strings.length && strings[p].trim().toLowerCase().equals("spline");
    }
}
