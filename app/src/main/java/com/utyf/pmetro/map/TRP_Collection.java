package com.utyf.pmetro.map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import com.utyf.pmetro.util.ExtPointF;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.zipMap;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Stores information about current state of the map
 *
 * @author Utyf
  */

public class TRP_Collection {
    private final TRP[] trpList; // all trp files

    private final Paint pline;

    TRP_Collection(TRP[] trpList) {
        this.trpList = trpList;

        pline = new Paint();
        pline.setStyle(Paint.Style.STROKE);
    }

    public static TRP_Collection loadAll() {
        String[] names = zipMap.getFileList(".trp");
        if (names.length == 0)
            return null;

        ArrayList<TRP> tl = new ArrayList<>();
        for (String nm : names) {
            TRP tt = TRP.load(nm);
            if (tt == null)
                return null;
            tl.add(tt);
        }
        TRP[] trpList = tl.toArray(new TRP[tl.size()]);

        //MapActivity.mapActivity.setTRPMenu();

        TRP_Collection collection = new TRP_Collection(trpList);

        for (TRP tt : trpList)    // set numbers of line and station for all transfers
            for (TRP.Transfer tr : tt.transfers)
                tr.setNums(collection);

        return collection;
    }

    public TRP getTRP(int trpNum) {
        if (trpNum < 0 || trpNum > trpList.length) return null;
        return trpList[trpNum];
    }

    public int getSize() {
        return trpList.length;
    }

    public int getTRPnum(String name) {
        if (trpList == null) return -1;
        for (int i = 0; i < trpList.length; i++)
            if (trpList[i].getName().equals(name)) return i;
        return -1;
    }

    public TRP.TRP_line getLine(String name) {
        for (TRP tt : trpList) {
            if (tt.lines == null) return null;
            for (TRP.TRP_line tl : tt.lines)
                if (tl.name.equals(name)) return tl;
        }
        return null;
    }

    public TRP.TRP_line getLine(int tr, int ln) {
        TRP tt = trpList[tr];
        return tt.getLine(ln);
    }

    public StationsNum getLineNum(String name) {
        for (int i = 0; i < trpList.length; i++) {
            if (trpList[i].lines == null) return null;
            for (int j = 0; j < trpList[i].lines.length; j++)
                if (trpList[i].lines[j].name.equals(name)) return new StationsNum(i, j, -1);
        }

        return null;
    }

    public TRP.Transfer[] getTransfers(int trp, int line, int stn) {
        LinkedList<TRP.Transfer> listT = new LinkedList<>();
        int i = 0;

        for (TRP tt : trpList)
            for (TRP.Transfer trn : tt.transfers)
                if ((trn.trp1num == trp && trn.line1num == line && trn.st1num == stn)
                        || (trn.trp2num == trp && trn.line2num == line && trn.st2num == stn)) {
                    listT.add(trn);
                    i++;
                }

        if (i > 0)
            return listT.toArray(new TRP.Transfer[i]);

        return null;
    }

/*    public static TRP getTRP(String name)  {
        if( trpList==null ) return null;
        for( TRP tt : trpList )
            if( tt.name.equals(name) ) return tt;
        return null;
    } //*/

    public TRP.Transfer getTransfer(StationsNum ls1, StationsNum ls2) {

        for (TRP tt : trpList)
            for (TRP.Transfer trn : tt.transfers) {
                if ((trn.trp1num == ls1.trp && trn.line1num == ls1.line && trn.st1num == ls1.stn)
                        && (trn.trp2num == ls2.trp && trn.line2num == ls2.line && trn.st2num == ls2.stn))
                    return trn;
                if ((trn.trp1num == ls2.trp && trn.line1num == ls2.line && trn.st1num == ls2.stn)
                        && (trn.trp2num == ls1.trp && trn.line2num == ls1.line && trn.st2num == ls1.stn))
                    return trn;
            }

        return null;
    }

    //public static TRP_Station getStation(StationsNum ls)  {
    //    return trpList.get(ls.trp).getLineParameters(ls.line).getStation(ls.stn);
    //}

    public TRP.TRP_Station getStation(int t, int l, int s) {
        return trpList[t].getLine(l).getStation(s);
    }

    public String getStationName(StationsNum ls) {
        return trpList[ls.trp].getLine(ls.line).getStationName(ls.stn);
    }

    public void DrawTransfers(Canvas c, Paint p, MAP map) {
        PointF p1, p2;
        Line ll;

        p.setColor(0xff000000);
        pline.setColor(0xff000000);
        pline.setStrokeWidth(map.parameters.LinesWidth + 6);
        for (TRP trp: trpList) {   // draw black edging
            for (TRP.Transfer t : trp.transfers) {
                if (t.invisible || !t.isCorrect()) continue;

                if ((ll = map.getLine(t.trp1num, t.line1num)) == null) continue;
                if (ExtPointF.isNull(p1 = ll.getCoord(t.st1num))) continue;

                if ((ll = map.getLine(t.trp2num, t.line2num)) == null) continue;
                if (ExtPointF.isNull(p2 = ll.getCoord(t.st2num))) continue;

                c.drawCircle(p1.x, p1.y, map.parameters.StationRadius + 3, p);
                c.drawCircle(p2.x, p2.y, map.parameters.StationRadius + 3, p);
                c.drawLine(p1.x, p1.y, p2.x, p2.y, pline);
            }
        }

        p.setColor(0xffffffff);
        pline.setColor(0xffffffff);
        pline.setStrokeWidth(map.parameters.LinesWidth + 4);
        for (TRP trp : trpList) {   // draw white transfer
            for (TRP.Transfer t : trp.transfers) {
                if (t.invisible || !t.isCorrect()) continue;

                if ((ll = map.getLine(t.trp1num, t.line1num)) == null) continue;
                if (ExtPointF.isNull(p1 = ll.getCoord(t.st1num))) continue;

                if ((ll = map.getLine(t.trp2num, t.line2num)) == null) continue;
                if (ExtPointF.isNull(p2 = ll.getCoord(t.st2num))) continue;

                c.drawCircle(p1.x, p1.y, map.parameters.StationRadius + 2, p);
                c.drawCircle(p2.x, p2.y, map.parameters.StationRadius + 2, p);
                c.drawLine(p1.x, p1.y, p2.x, p2.y, pline);
            }
        }
    }
}
