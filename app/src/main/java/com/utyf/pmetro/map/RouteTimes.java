package com.utyf.pmetro.map;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.StationsNum;

import java.util.LinkedList;

/**
 * Created by Utyf on 22.03.2015.
 *
 *  Calculate best times from station to all other and to station from all other.
 */

public class RouteTimes {

    public  AllTimes fromStart,toEnd; //,tooEnd;
    private AllTimes atm;  // temporary array
    private LinkedList<RouteNode> rNodes;

    public class Line {
        float[] stns;
        float[] delay;
        public Line(int size) {
            stns = new float[size];
            delay = new float[size];
        }
    }

    public class TRPtimes {
        Line[] lines;
        public TRPtimes(int size) {
            lines = new Line[size];
        }
    }

    public class AllTimes {
        TRPtimes[] trps;

        public AllTimes() {
            trps = new TRPtimes[TRP.trpList.length];
            for( int k=0; k<TRP.trpList.length; k++ ) {  // create and erase all arrays
                TRP tt1 = TRP.getTRP(k);
                assert tt1 != null;
                trps[k] = new TRPtimes(tt1.lines.length);
                for( int i=0; i<tt1.lines.length; i++ ) {
                    trps[k].lines[i] = new Line(tt1.getLine(i).Stations.length);
                    for( int j=0; j<trps[k].lines[i].stns.length; j++ ) {
                        trps[k].lines[i].stns[j] = -1;
                        trps[k].lines[i].delay[j] = 0;
                    }
                }
            }
        }

        float getTime(int t, int l, int s) {
            if( t==-1 || l==-1 || s==-1 ) return -1;
            return trps[t].lines[l].stns[s];
        }

        float getTime(StationsNum stn) {
            if( stn.trp==-1 || stn.line==-1 || stn.stn==-1 ) return -1;
            return trps[stn.trp].lines[stn.line].stns[stn.stn];
        }

        float getDelay(int t, int l, int s) {
            if( t==-1 || l==-1 || s==-1 ) return -1;
            return trps[t].lines[l].delay[s];
        }

        float getDelay(StationsNum stn) {
            if( stn.trp==-1 || stn.line==-1 || stn.stn==-1 ) return -1;
            return trps[stn.trp].lines[stn.line].delay[stn.stn];
        }

        void setTime(int t, int l, int s, float tm) {
            if( t==-1 || l==-1 || s==-1 ) return;
            trps[t].lines[l].stns[s] = tm;
        }

        void setTime(StationsNum stn, float tm) {
            if( stn.trp==-1 || stn.line==-1 || stn.stn==-1 ) return;
            trps[stn.trp].lines[stn.line].stns[stn.stn] = tm;
        }

        void setDelay(int t, int l, int s, float dl) {
            if( t==-1 || l==-1 || s==-1 ) return;
            trps[t].lines[l].delay[s] = dl;
        }

        void setDelay(StationsNum stn, float dl) {
            if( stn.trp==-1 || stn.line==-1 || stn.stn==-1 ) return;
            trps[stn.trp].lines[stn.line].delay[stn.stn] = dl;
        }
    }

    public synchronized void setStart(StationsNum start) {
        long tm1, tm2;
        tm1 = System.currentTimeMillis();
        fromStart = calculateStation(start);
        tm2 = System.currentTimeMillis();
        MapActivity.calcTime = tm2-tm1;
        //if( MapActivity.debugMode ) {
        //    tooEnd = calculateStationBack(start);
        //    MapActivity.calcBTime = System.currentTimeMillis()-tm2;
        //}
    }

    public void setEnd(StationsNum end) {
        //long tm1 = System.currentTimeMillis();
        toEnd = calculateStationBack(end);
        //MapActivity.calcBTime = System.currentTimeMillis()-tm1;
    }

    private AllTimes calculateStation( StationsNum start ) {

        atm = new AllTimes();
        if( !TRP.isActive(start.trp) )  return atm;  // if start station transport not active

        rNodes = new LinkedList<>();
        addRNode(new RouteNode(start, 0, true, 0));

        while( !rNodes.isEmpty() )  {
            RouteNode rnd = rNodes.get(0);
            rNodes.remove(0);

            if( rnd.direction>=0 ) calculateTimesForward(rnd);
            if( rnd.direction<=0 ) calculateTimesBack   (rnd);
        }
        rNodes = null;

        return atm;
    }

    boolean addRNode(RouteNode rn) {
        if( rn.trp<0 || rn.line<0 || rn.stn<0 ) return false;

        //int i=0;
        //for( RouteNode r1 : rNodes )
        //    { if( r1.time>rn.time ) break; i++; }

        rNodes.add(rn); // i,
        return true;
    }

    private void calculateTimesForward(RouteNode rnd) {
        int             nextStn=rnd.stn;
        float           time, tm, dl, nextTime=rnd.time, nextDelay=rnd.delay;
        TRP.TRP_line    tl = TRP.getLine(rnd.trp, rnd.line);

        RouteNode node=new RouteNode(rnd.trp, rnd.line, rnd.stn, rnd.time, rnd.delay!=0, rnd.direction);

        do {
            node.stn = nextStn;
            node.delay = nextDelay;
            time = nextTime;
            nextStn = -1;
            tm = atm.getTime(node);
            dl = atm.getDelay(node);
            if( tm>=0 && tm+dl<time+node.delay )  return;    // if there is better time - stop
            if( tm<0 || tm>time ) {                    // overwrite only if new time is better
                atm.setTime(node, time);
                atm.setDelay(node, node.delay);
            }

            checkTransfer(node.trp, node.line, node.stn, time);

            dl = TRP.getLine(node.trp,node.line).delays.get();
            for( TRP.TRP_Driving drv : tl.getStation(node.stn).drivings ) {
                if( drv.frwDR>0 )
                    /* if( nextStn<0 ) { nextStn = drv.frwStNum; nextTime = time + drv.frwDR + node.delay; nextDelay=0; }
                    else*/ addRNode(new RouteNode(node.trp, node.line, drv.frwStNum, time + drv.frwDR + node.delay, false, 1));
                if( drv.bckDR>0 && drv.bckStNum>=0 ) {          // check for transfer to revers way
                    tm = atm.getTime(node.trp,node.line,drv.bckStNum);    // for quick drop bad way
                    if( tm<0 || tm>time+drv.bckDR )
                        addRNode(new RouteNode(node.trp, node.line, drv.bckStNum, time + drv.bckDR + dl, false, -1));
                }
            }
        } while( nextStn>=0 );
    }

    private void calculateTimesBack(RouteNode node) {
        float           time = node.time, tm, dl;
        TRP.TRP_line    tl = TRP.getLine(node.trp, node.line);

        tm = atm.getTime(node);
        dl = atm.getDelay(node);
        if( tm>=0 && tm+dl<time+node.delay )  return;    // if there is better time - stop
        if( tm<0 || tm>time ) {                    // overwrite only if new time is better
            atm.setTime(node, time);
            atm.setDelay(node, node.delay);
        }

        checkTransfer(node.trp, node.line, node.stn, time);

        dl = TRP.getLine(node.trp,node.line).delays.get();
        for( TRP.TRP_Driving drv : tl.getStation(node.stn).drivings ) {
            if( drv.bckDR>0 )
                addRNode(new RouteNode(node.trp, node.line, drv.bckStNum, time + drv.bckDR + node.delay, false, -1));
            if( drv.frwDR>0 && drv.frwStNum>=0 ) {          // check for transfer to revers way
                tm = atm.getTime(node.trp,node.line,drv.frwStNum);    // for quick drop bad way
                if( tm<0 || tm>time+drv.frwDR )
                    addRNode(new RouteNode(node.trp, node.line, drv.frwStNum, time + drv.frwDR + dl, false, 1));
            }
        }
    }

    private AllTimes calculateStationBack( StationsNum Stn ) {
        atm = new AllTimes();

        rNodes = new LinkedList<>();
        addRNode(new RouteNode(Stn, 0, true, 0));

        while( !rNodes.isEmpty() )  {
            RouteNode rn = rNodes.get(0);
            rNodes.remove(0);

            if( rn.direction>=0 ) calculateBackTimesForward(rn);
            if( rn.direction<=0 ) calculateBackTimesBack   (rn);
        }
        rNodes = null;

        return atm;
    }

    private void calculateBackTimesForward(RouteNode node) {
        float           time = node.time, tm, dl;
        TRP.TRP_line    tl = TRP.getLine(node.trp,node.line);

        tm = atm.getTime(node);
        dl = atm.getDelay(node);
        if( tm>=0 && tm+dl<time+node.delay )  return;    // if there is better time - stop
        if( tm<0 || tm>time ) {                    // overwrite only if new time is better
            atm.setTime(node, time);
            atm.setDelay(node, node.delay);
        }

        checkTransfer(node.trp, node.line, node.stn, time);

        dl = TRP.getLine(node.trp,node.line).delays.get();
        for( int n=0; n<tl.Stations.length; n++ )  // check all station - is there back-drive to current station
            for( TRP.TRP_Driving drv : tl.getStation(n).drivings ) {
                if( drv.bckDR>0 && drv.bckStNum==node.stn )
                    addRNode(new RouteNode(node.trp, node.line, n, time + drv.bckDR + node.delay, false, 1));
                if( drv.frwDR>0 && drv.frwStNum==node.stn ) {
                    tm = atm.getTime( node.trp, node.line, n );    // for quick drop bad way
                    if( tm<0 || tm>time+drv.frwDR+dl )
                        addRNode(new RouteNode(node.trp, node.line, n, time + drv.frwDR + dl, false, -1));
                }
            }
    }

    private void calculateBackTimesBack(RouteNode node) {
        float           time = node.time, tm, dl;
        TRP.TRP_line    tl = TRP.getLine(node.trp, node.line);

        tm = atm.getTime(node);
        dl = atm.getDelay(node);
        if( tm>=0 && tm+dl<time+node.delay )  return;    // if there is better time - stop
        if( tm<0 || tm>time ) {                    // overwrite only if new time is better
            atm.setTime(node, time);
            atm.setDelay(node, node.delay);
        }

        checkTransfer(node.trp, node.line, node.stn, time);

        dl = TRP.getLine(node.trp,node.line).delays.get();
        for( int n=0; n<tl.Stations.length; n++ )  // check all station - is there back-drive to current station
            for( TRP.TRP_Driving drv : tl.getStation(n).drivings ) {
                if( drv.frwDR>0 && drv.frwStNum==node.stn )
                    addRNode(new RouteNode(node.trp, node.line, n, time + drv.frwDR + node.delay, false, -1));
                if( drv.bckDR>0 && drv.bckStNum==node.stn ) {
                    tm = atm.getTime( node.trp, node.line, n );    // for quick drop bad way
                    if( tm<0 || tm>time+drv.bckDR+dl )
                        addRNode(new RouteNode(node.trp, node.line, n, time + drv.bckDR + dl, false, 1));
                }
            }
    }

    private void checkTransfer(int trpNum, int line, int stn, float time) {
        float   tm;
        TRP.Transfer[] arrT;
        if( (arrT=TRP.getTransfers(trpNum, line, stn)) != null )
            for( TRP.Transfer trn : arrT )
                if( trn!=null && trn.isCorrect() )
                    if( trn.trp1num==trpNum && trn.line1num==line && trn.st1num==stn ) {
                        if( !TRP.isActive(trn.trp2num) ) continue; // skip non active TRP
                        if( !TRP.getStation(trn.trp2num,trn.line2num,trn.st2num).isWorking ) continue;
                        tm = atm.trps[trn.trp2num].lines[trn.line2num].stns[trn.st2num];
                        if( tm<0 || tm>time+trn.time )
                            addRNode(new RouteNode(trn.trp2num, trn.line2num, trn.st2num, time + trn.time, true, 0));
                    } else {
                        if( !TRP.isActive(trn.trp1num) ) continue; // skip non active TRP
                        if( !TRP.getStation(trn.trp1num,trn.line1num,trn.st1num).isWorking ) continue;
                        tm = atm.trps[trn.trp1num].lines[trn.line1num].stns[trn.st1num];
                        if( tm<0 || tm>time+trn.time  )
                            addRNode(new RouteNode(trn.trp1num, trn.line1num, trn.st1num, time + trn.time, true, 0));
                    }
    }
}
