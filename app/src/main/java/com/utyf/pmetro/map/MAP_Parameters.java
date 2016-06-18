package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.Util;

import java.util.ArrayList;

/**
 * Loads and parses information about map from .map file
 *
 * @author Utyf
 */

public class MAP_Parameters {
    /** Name of the map file */
    public String name;
    /** Name of background image file */
    public String ImageFileName;
    /** Diameter of station */
    public float StationDiameter;
    /** Radius of station */
    public float StationRadius;
    /** Width of metro line */
    public float LinesWidth;
    /** Flag, showing if station names are displayed in upper case */
    public boolean UpperCase;
    /** Flag, showing if station names are splitted by space into two lines */
    public boolean WordWrap;
    /** Flag, showing if it is needed to draw metro lines, stations and transfers (true), or if
     * they are already included in background */
    public boolean IsVector;
    /** Names of transport *.trp files, which are allowed to use on the map */
    public String[] Transports;
    /** Transports that are available for routing */
    public int[] allowedTRPs;
    /** Transports that are set to be used in routing by default */
    public int[] activeTRPs;
    /** Vectors that are used to draw map background */
    public VEC[] vecs;
    /** Station labels, which are shown inside station circles */
    public StationLabels stnLabels = new StationLabels();
    /** Parameters of metro lines */
    public ArrayList<Line_Parameters> la;

    public MAP_Parameters() {}

    public synchronized int load(String nm, MAP_Parameters defaultParameters, TRP_Collection transports) {  // loading map file
        Line_Parameters ll;
        param prm;

        Parameters parser = new Parameters();

        name = nm;
        if (parser.load(name) < 0) return -1;

        WordWrap = true;
        IsVector = true;
        // If delay is not set to 0, the map will possibly not be able to load
        // because it can miss such delay type
        Delay.setType(0);

        Section secOpt = parser.getSec("Options");
        ImageFileName = secOpt.getParamValue("ImageFileName");
        UpperCase = secOpt.getParamValue("UpperCase").trim().toLowerCase().equals("true");

        String str = secOpt.getParamValue("Transports");
        if (!str.isEmpty()) {
            Transports = str.split(",");
            for (int i = 0; i < Transports.length; i++) Transports[i] = Transports[i].trim();
            allowedTRPs = new int[Transports.length];
            for (int i = 0; i < Transports.length; i++)
                allowedTRPs[i] = transports.getTRPnum(Transports[i]);
        } else {
            allowedTRPs = new int[transports.getSize()]; // if no transport parameter - set all transports as allowed
            for (int i = 0; i < allowedTRPs.length; i++) allowedTRPs[i] = i;
        }

        // copy from Transport
        int size = 0, ii = 0;
        for (int k : allowedTRPs) if (k != -1) size++;
        activeTRPs = new int[size];
        for (int k : allowedTRPs) if (k != -1) activeTRPs[ii++] = k;

        StationDiameter = ExtFloat.parseFloat(secOpt.getParamValue("StationDiameter"));
        if (StationDiameter == 0) StationDiameter = 16f;
        StationRadius = StationDiameter / 2;
        LinesWidth = ExtFloat.parseFloat(secOpt.getParamValue("LinesWidth"));
        if (LinesWidth == 0) LinesWidth = StationDiameter * 0.5625f;  //  9/16

        prm = secOpt.getParam("WordWrap");
        if (prm != null)
            WordWrap = prm.value.toLowerCase().equals("true");
        str = secOpt.getParamValue("IsVector");
        IsVector = str.isEmpty() || str.toLowerCase().equals("true") || str.equals("1");

        String[] strs = ImageFileName.split(",");
        vecs = new VEC[strs.length];
        for (int i = 0; i < strs.length; i++) {
            vecs[i] = new VEC();
            vecs[i].load(strs[i]);
        }
        System.gc();

        int i = 1;
        stnLabels.clear();
        if (i < parser.secsNum() && parser.getSec(i).name.equals("StationLabels")) {
            stnLabels.load(parser.getSec(i));
            i++;
        }
        Section addSec = null;
        la = new ArrayList<>();
        for (; i < parser.secsNum(); i++) {             // parsing lines parameters
            if (parser.getSec(i).name.equals("AdditionalNodes"))  // last section
            {
                addSec = parser.getSec(i);
                break;
            }
            Line_Parameters line_parameters = new Line_Parameters();
            Line_Parameters defaultLineParameters = defaultParameters != null ? defaultParameters.getLineParameters(parser.getSec(i).name) : null;
            line_parameters.load(parser.getSec(i), defaultLineParameters);
            la.add(line_parameters);
        }

        if (addSec != null) {  // if section AdditionalNodes was found
            for (int j = 0; j < addSec.ParamsNum(); j++) {
                // strs = addSec.getParam(j).value.split(",");
                strs = Util.split(addSec.getParam(j).value, ',');
                if (strs == null || strs.length < 5) continue;
                ll = getLineParameters(strs[0]);
                if (ll != null) ll.addAddNode(strs, transports);
                else
                    Log.e("MAP /137", "Wrong line name for additionalNode - " + addSec.getParam(j).value);
            }
        }

/*        try {  // to do switch by settings
            if( vec[0].bmp!=null ) {
                // bmp = Bitmap.createBitmap((int)(vec.sclSize.x), (int)(vec.sclSize.y), Bitmap.Config.ARGB_8888);
                bmp = vecs[0].bmp;
                Canvas c = new Canvas(bmp);
                for ( VEC vv : vecs )
                    vv.DrawVEC(c);
                DrawMAP(c, false);
            }
        } catch (OutOfMemoryError E) {
            Log.e("MAP /115","Catch Out Of Memory error");
            bmp = null;
        } */
        //MapActivity.mapActivity.mapView.contentChanged(null);
        return 0;
    }

    public Line_Parameters getLineParameters(String nm) {
        if (la == null) return null;
        for (Line_Parameters ll : la)
            if (ll.name.equals(nm)) return ll;
        return null;
    }

}
