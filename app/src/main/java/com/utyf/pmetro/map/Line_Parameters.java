package com.utyf.pmetro.map;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtInteger;
import com.utyf.pmetro.util.Util;

import java.util.ArrayList;

/**
 * Loads and parses information about metro lines from .map file
 *
 * @author Utyf
 */


public class Line_Parameters {
    public int Color, LabelsColor, LabelsBColor, shadowColor;
    String name;
    boolean drawLblBkgr = true;
    PointF[] coordinates;
    RectF[] Rects;
    RectF lineSelect;
    ArrayList<AdditionalNodes> addNodes;

    void load(Section sec) {
        name = sec.name;

        Line_Parameters lMetro = null;
        if (MapData.mapMetro != null)
            lMetro = MapData.mapMetro.parameters.getLineParameters(name); // to get default values

        param prm;
        if ((prm = sec.getParam("Color")) != null)
            Color = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);
        else if (lMetro != null) Color = lMetro.Color;
        else Color = 0xFF000000;

        if ((prm = sec.getParam("LabelsColor")) == null) {
            if (lMetro != null) LabelsColor = lMetro.LabelsColor;
            else LabelsColor = Color;
        } else LabelsColor = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);
        shadowColor = Util.getDarkColor(LabelsColor);

        if ((prm = sec.getParam("LabelsBColor")) == null) LabelsBColor = 0xffffffff;
        else if (prm.value.equals("-1")) drawLblBkgr = false;
        else LabelsBColor = 0xFF000000 + ExtInteger.parseInt(prm.value, 16);

        LoadCoordinates(sec.getParamValue("Coordinates"));
        LoadRects(sec.getParamValue("Rects"));
        LoadRect(sec.getParam("Rect"));
    }

    void addAddNode(String[] strs) {

        AdditionalNodes ad = new AdditionalNodes(strs);
        if (addNodes == null) addNodes = new ArrayList<>();
        addNodes.add(ad);
    }

    AdditionalNodes findAddNode(int st1, int st2) {
        if (addNodes == null) return null;

        for (AdditionalNodes an : addNodes) {
            if (an.numSt1 == st1 && an.numSt2 == st2) return an;
            if (an.numSt1 == st2 && an.numSt2 == st1) return an;
        }
        return null;
    }

    private void LoadCoordinates(String cc) {
        int i, j;
        float x, y;

        if (cc.isEmpty())  // if no data
        {
            coordinates = null;
            return;
        }

        String[] strs = cc.split(",");

        if (strs.length % 2 != 0) {  // if wrong data
            Log.e("Line /80", "Line " + name + ". Number of coordinates not even - " + strs.length + " -" + cc + "- ");
            coordinates = null;
            return;
        }

        coordinates = new PointF[strs.length / 2];

        j = 0;
        for (i = 0; i < strs.length; i += 2) {
            x = ExtFloat.parseFloat(strs[i]);
            y = ExtFloat.parseFloat(strs[i + 1]);
            coordinates[j++] = new PointF(x, y);
        }
    } // LoadCoordinates()

    private void LoadRects(String cc) {
        int i, j;
        float x1, y1, x2, y2;

        if (cc.length() < 1)  // if no data
        {
            Rects = null;
            return;
        }

        String[] strs = cc.split(",");

        if (strs.length % 4 != 0) {  // if wrong data
            Log.e("Line /127", "Line " + name + ". Number of rectangle coordinates not even - " + strs.length + " -" + cc + "- ");
            Rects = null;
            return;
        }

        Rects = new RectF[strs.length / 4];

        j = 0;
        for (i = 0; i < strs.length; i += 4) {
            x1 = ExtFloat.parseFloat(strs[i]);
            y1 = ExtFloat.parseFloat(strs[i + 1]);
            x2 = ExtFloat.parseFloat(strs[i + 2]) + x1;
            y2 = ExtFloat.parseFloat(strs[i + 3]) + y1;

            Rects[j] = new RectF(x1, y1, x2, y2);
            j++;
        }
    }

    private void LoadRect(param prm) {
        float x1, y1, x2, y2;

        if (prm == null || prm.value.isEmpty())  // if no data
        {
            lineSelect = null;
            return;
        }
        String[] strs = prm.value.split(",");

        if (strs.length != 4) {  // if wrong data
            Log.e("Line /154", "Line " + name + ". Number of rectangle coordinates not 4 - " + strs.length + " -" + prm.value + "- ");
            Rects = null;
            return;
        }

        x1 = ExtFloat.parseFloat(strs[0]);
        y1 = ExtFloat.parseFloat(strs[1]);
        x2 = ExtFloat.parseFloat(strs[2]) + x1;
        y2 = ExtFloat.parseFloat(strs[3]) + y1;
        lineSelect = new RectF(x1, y1, x2, y2);
    }
}
