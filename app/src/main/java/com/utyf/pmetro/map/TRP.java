package com.utyf.pmetro.map;

import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.ExtFloat;
import com.utyf.pmetro.util.ExtPointF;
import com.utyf.pmetro.util.StationsNum;
import com.utyf.pmetro.util.Util;
import com.utyf.pmetro.util.zipMap;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


public class TRP extends Parameters {
    public String          Type;
    TRP_line[]             lines;
    private Transfer[]     transfers;

    static TRP[]   trpList; // all trp files
    private static int[]    allowedTRPs;
    private static int[]    activeTRPs;

    public static StationsNum routeStart, routeEnd;
    final static RouteTimes  rt = new RouteTimes();
    private static Route bestRoute;

    private static Paint    pline;

    static boolean loadAll()  {
        routeStart = routeEnd = null;
        bestRoute = null;

        String[]  names = zipMap.getFileList(".trp");
        if( names==null ) return false;

        ArrayList<TRP> tl = new ArrayList<>();
        for( String nm : names ) {
            TRP tt = new TRP();
            if( tt.load(nm)<0 ) return false;
            tl.add(tt);
        }
        trpList = tl.toArray(new TRP[tl.size()]);

        allowedTRPs = null;
        activeTRPs = new int[trpList.length];
        clearActiveTRP();
        //MapActivity.mapActivity.setTRPMenu();

        for( TRP tt : trpList )    // set numbers of line and station for all transfers
            for( Transfer tr : tt.transfers )
                tr.setNums();

        return true;
    }

    public static void clearActiveTRP() {  // disable all TRP
        for( int i=0; i<activeTRPs.length; i++ )  activeTRPs[i] = -1;
    }

    public static void setActive(int[] trpNums) {
        // Do not disable transports if start station is selected
        if (routeStart == null) {
            clearActiveTRP();
        }

        for( int tNum : trpNums ) addActive(tNum);
        synchronized (rt) {
            rt.createGraph();

            setStart(routeStart);
            setEnd(routeEnd);
        }
        MapActivity.mapActivity.setActiveTRP();
    }

    public static void checkActive() {  // remove disallowed from active
        for( int i=0; i<activeTRPs.length; i++ )
            if( !isAllowed(i) )  activeTRPs[i] = -1;
    }

    public static boolean addActive(int trpNum) {
        if( !isAllowed(trpNum) ) return false;
        checkActive();
        activeTRPs[trpNum] = trpNum;
        return true;
    }

    public static void removeActive(int trpNum) {
        checkActive();
        activeTRPs[trpNum] = -1;
    }

    public static boolean isActive(int trpNum) {
        return activeTRPs[trpNum]==trpNum;
    }

    public static boolean isAllowed(int trpNum) {
        if( allowedTRPs==null ) return false;
        for( int num : allowedTRPs )
            if( num==trpNum ) return true;
        return false;
    }

    public static void setAllowed(int[] ii) {
        allowedTRPs = ii;
        MapActivity.mapActivity.setAllowedTRP();
    }

    public static TRP getTRP(int trpNum)  {
        if( trpNum<0 || trpNum>trpList.length ) return null;
        return trpList[trpNum];
    }

    public static int getSize()  {
        return trpList.length;
    }

    public static int getTRPnum(String name)  {
        if( trpList==null ) return -1;
        for( int i=0; i<trpList.length; i++ )
            if( trpList[i].name.equals(name) ) return i;
        return -1;
    }

/*    public static TRP getTRP(String name)  {
        if( trpList==null ) return null;
        for( TRP tt : trpList )
            if( tt.name.equals(name) ) return tt;
        return null;
    } //*/

    public synchronized static void setStart(StationsNum ls)  {
        routeStart = ls;
        if (routeStart != null && TRP.isActive(routeStart.trp)) {
            calculateTimes(TRP.routeStart);
        }
    }

    public synchronized static void setEnd(StationsNum ls)  {
        routeEnd = ls;
        if (routeStart != null && routeEnd != null) {
            makeRoutes();
        }
    }

    public synchronized static void resetRoute() {
        final ProgressDialog progDialog = ProgressDialog.show(MapActivity.mapActivity, null, "Computing routes..", true);

        new Thread("Route computing") {
            public void run() {
                setPriority(MAX_PRIORITY);

                synchronized (rt) {
                    rt.createGraph();

                    setStart(routeStart);
                    setEnd(routeEnd);
                }

                progDialog.dismiss();
                MapActivity.mapActivity.mapView.redraw();
            }
        }.start();
    }

    private synchronized static void makeRoutes() {
        long tm = System.currentTimeMillis();

        bestRoute = null;
        synchronized (rt) {
            rt.setEnd(routeEnd);

            if( !isActive(routeStart.trp) || !isActive(routeEnd.trp) )
                return; // stop if transport not active

            if (rt.getTime(routeEnd) == -1)
                return; // routeEnd is not reachable

            bestRoute = rt.getRoute();
        }
        bestRoute.makePath();

        MapActivity.makeRouteTime = System.currentTimeMillis()-tm;
    }

    public static void calculateTimes(StationsNum start) {
        synchronized (rt) {
            rt.setStart(start);
        }
    }

    static float String2Time(String t) {
        t=t.trim();
        if( t.isEmpty() ) return -1;

        int i=t.indexOf('.');

        try {
            if( i==-1 )  return Integer.parseInt(t);
            else
                return (float)Integer.parseInt(t.substring(0,i)) + (float)Integer.parseInt(t.substring(i+1)) /60;
        } catch (NumberFormatException e) {
            Log.e("TRP /354", "TRP Driving fork wrong time - <" + t +"> ");
            return -1;
        }
    }

    public static boolean routeExists() {
        return bestRoute != null;
    }

    public static void clearRoute() {
        bestRoute = null;
    }

    public class TRP_Driving {
        public int     frwStNum=-1, bckStNum=-1;
        String         frwST, bckST;
        public float   frwDR=-1, bckDR=-1;

        TRP_Driving(String n1, String n2) {
            frwST=n1; bckST=n2;
            if( frwST.length()>1 && frwST.charAt(0)=='-' ) frwST = frwST.substring(1);
            if( bckST.length()>1 && bckST.charAt(0)=='-' ) bckST = bckST.substring(1);
        }

        boolean setTimes(String t1, String t2) {
            frwDR = String2Time(t1);
            bckDR = String2Time(t2);
            return frwDR>0 || bckDR>0;  // true if station is working
        }
    }

    public class TRP_Station {
        public String  name;  //, alias;  // todo
        boolean        isWorking;
        LinkedList<TRP_Driving> drivings;

        private void addDriving(String names) {

            String[] strs = Util.split(names,',');
            if( drivings==null ) drivings = new LinkedList<>();

            for( int i=0; i<strs.length; i+=2 )
                if( i+1>=strs.length )   // is there last name?
                    drivings.add( new TRP_Driving(strs[i].trim(),"") );
                else
                    drivings.add( new TRP_Driving(strs[i].trim(),strs[i+1].trim()) );
        }

        private void addDrivingEmpty() {
            if( drivings!=null ) { Log.e("TRP /354", "Driving not empty."); return; }
            drivings = new LinkedList<>();
            drivings.add( new TRP_Driving("-","-") );
        }

        private void setDrivingTime(String times) {

            String[] strs = times.split(",");
            int   j=0;
            for( int i=0; i<strs.length; i+=2 ) {
                if( drivings.size()<=j )  {
                    Log.e("TRP /405", "Driving fork not the same as station fork");
                    return;
                }
                if( i+1>=strs.length )  // is there last time?
                    drivings.get(j).setTimes( strs[i],"" );
                else
                    drivings.get(j).setTimes( strs[i],strs[i+1] );
                j++;
            }
        }
    }  // TRP_Station

    public class TRP_line {

        Delay delays;
        public String   name, alias, LineMap, Aliases;
        TRP_Station[] Stations;

        boolean Load(Section sec) {
            float  day,night;
            String str;

            name  = sec.getParamValue("Name");
            alias = sec.getParamValue("Alias");
            LineMap = sec.getParamValue("LineMap");
            Aliases = sec.getParamValue("Aliases"); // todo
            str = sec.getParamValue("Delays");
            if( !str.isEmpty() )  delays = new Delay( str );
            else {
                if( sec.getParamValue("DelayDay").isEmpty() || sec.getParamValue("DelayNight").isEmpty() ) delays = new Delay();
                else {
                    day = ExtFloat.parseFloat(sec.getParamValue("DelayDay"));
                    night = ExtFloat.parseFloat(sec.getParamValue("DelayNight"));
                    delays = new Delay( day,night );
                }
            }

            Stations = new TRP_Station[0];
            LoadStations( sec.getParamValue("Stations") );
            LoadDriving ( sec.getParamValue("Driving") );

            for( TRP_Station st : Stations )
                for( TRP_Driving drv : st.drivings ) {
                    if (drv.bckStNum < 0 && drv.bckDR > 0)
                        Log.e("TRP /452", "Bad back driving. Line - "+name+", Station - " + st.name);
                    if (drv.frwStNum < 0 && drv.frwDR > 0)
                        Log.e("TRP /454", "Bad forw driving. Line - "+name+", Station - " + st.name);
                }
            return true;
        }

        void LoadDriving(String drv) {
            int i, i2, stNum=0;
            TRP_Station  st;

            drv = drv.trim();

            while( !drv.isEmpty() ) {
                if( stNum>=Stations.length ) {
                    Log.e("TRP /467", "Driving more then stations");
                    return;
                }

                st = Stations[stNum];

                if( drv.charAt(0)=='(' )  { // is it fork?
                    i = drv.indexOf(')');
                    if( i==-1 ) { Log.e("TRP /475","Bad driving times. There is not closing ')' - "+drv); return; }

                    st.setDrivingTime(drv.substring(1, i));
                    drv = drv.substring(i+1);                    // remove till ')'
                    if( !drv.isEmpty() ) drv = drv.substring(1); // remove ','
                } else {
                    i = drv.indexOf(',');
                    if( i==-1 )  i2 = i = drv.length();
                    else         i2 = i+1;

                    st.drivings.get(0).frwDR = String2Time( drv.substring(0,i) );
                    if( stNum>0 )  st.drivings.get(0).bckDR = getForwTime(Stations[stNum - 1], st);
                    else           st.drivings.get(0).bckDR = -1;

                    drv = drv.substring(i2);
                }

                stNum++;
            }

            if( stNum<Stations.length ) {  // if for last station was not data
                st = Stations[stNum];
                if( stNum>0 )  st.drivings.get(0).bckDR = getForwTime(Stations[stNum - 1], st);
                else           st.drivings.get(0).bckDR = -1;
                st.drivings.get(0).frwDR = -1;
            }

            for( TRP_Station st2 : Stations )          // set stations numbers for fork drivings
                for( TRP_Driving dr : st2.drivings ) {
                    if( dr.frwST.equals("-") || dr.bckST.equals("-") )  Log.e("TRP /461", "Station "+st2.name+" bad driving name.");
                    if( dr.frwStNum==-1 ) dr.frwStNum = getStationNum( dr.frwST );
                    if( dr.bckStNum==-1 ) dr.bckStNum = getStationNum( dr.bckST );
                    if( dr.frwDR>0 || dr.bckDR>0 ) st2.isWorking = true;  // check all stations - is it working
                }
        }

        public float getForwTime(TRP_Station st1, TRP_Station st2 ) {
            for( TRP_Driving td : st1.drivings )
                if( td.frwST.equals(st2.name) ) return td.frwDR;
            return -1;
        }

        /*private float getBackTime(TRP_Station st1, TRP_Station st2 ) {
            for( TRP_Driving td : st1.drivings )
                if( td.bckST.equals(st2.name) ) return td.bckDR;
            return -1;
        } //*/

        private String stnStr;
        void LoadStations(final String stn) {
            TRP_Driving  dr, dr2;
            TRP_Station  st=null, st2=null;
            stnStr = stn;
            ArrayList<TRP_Station> sa = new ArrayList<>();

            while( !stnStr.isEmpty() )  {  // loading stations names

                st = getStationEntry();
                if( st==null || st.name==null || st.name.isEmpty() ) {
                    Log.e("TRP /490", "Bad station string - " + stn);
                    return;
                }

                if( st2==null ) {  // set stations for default directions
                    if( st.drivings.get(0).bckST.equals("-") )   st.drivings.get(0).bckST = "";
                }
                else {
                    dr = st.drivings.get(0); dr2 = st2.drivings.get(0);
                    if( dr2.frwST.equals("-") ) { dr2.frwST = st.name; dr2.frwStNum = sa.size();   }
                    if(  dr.bckST.equals("-") ) {  dr.bckST = st2.name; dr.bckStNum = sa.size()-1; }
                }

                st2 = st;
                sa.add(st);
            }
            Stations = sa.toArray(new TRP_Station[sa.size()]);
            stnStr = null; // free memory

            if( st!=null && st.drivings.get(0).frwST.equals("-") ) st.drivings.get(0).frwST = "";  // remove forward for last station
        }

        private TRP_Station getStationEntry() {
            TRP_Station st = new TRP_Station();
            int  n;

            st.name = getName();
            if( st.name==null || st.name.isEmpty() )  return null;

            if( stnStr.isEmpty() || stnStr.charAt(0)==',' )  // is there fork ?
                st.addDrivingEmpty();                        // will set next on the stage
            else {
                if(stnStr.charAt(0)!='(' ) return null;
                n = stnStr.indexOf(')');
                if( n<0 ) return null;

                st.addDriving( stnStr.substring(1,n) );  // load fork
                stnStr = stnStr.substring(n+1);          // remove till ")"
            }
            stnStr = stnStr.trim();
            if( !stnStr.isEmpty() )
                if( stnStr.charAt(0)==',' ) stnStr = stnStr.substring(1);
                else Log.e("TRP /532", "Wrong station format");

            return st;
        }

        private String getName() {
            int i;
            String  name="";

            if( stnStr==null || stnStr.isEmpty() )  return null;
            stnStr = stnStr.trim();
            if( stnStr.charAt(0)=='"' ) {
                i=stnStr.indexOf('"',1);
                if( i<0 )  return null;

                name = stnStr.substring(1,i);
                stnStr = stnStr.substring(i+1);  // remove name and next symbol "
            } else
                while( !stnStr.isEmpty() ) {
                    if( stnStr.charAt(0)=='(' || stnStr.charAt(0)==')' || stnStr.charAt(0)==',' )  return name;
                    name = name + stnStr.charAt(0);
                    stnStr = stnStr.substring(1);
                }

            stnStr = stnStr.trim();
            return name.trim();
        }

        /*private String getBackWay(TRP_Station st1, TRP_Station st2) {  // return back way if there is forward way
            for( TRP_Driving td : st2.drivings )
                if( td.frwST.equals(st1.name) )    return st2.name;
            return "";
        } //*/

        public TRP_Station getStation(int st) {
            if( Stations==null || st<0 || Stations.length<=st ) return null;
            return Stations[st];
        }

        public String getStationName(int num)  {
            if( Stations==null || Stations.length<=num )   return null;
            return Stations[num].name;
        }

        int getStationNum(String name)  {
            if( Stations==null )   return -1;
            if( name.isEmpty() )   return -1;
            int sz = Stations.length;
            for( int i=0; i<sz; i++ )
                if( Stations[i].name.equals(name) ) return i;
            return -1;
        }
    }  // class TRP_line

    public class Transfer {
        public int     trp1num=-1, line1num=-1, st1num=-1, trp2num=-1, line2num=-1, st2num=-1;
        String         line1, st1, line2, st2;
        public float   time;
        public boolean invisible=false;
        boolean isWrong;

        public Transfer(String str) {
            String[] strs = str.split(",");
            if( strs.length<4 ) { Log.e("TRP /595","TRP - "+name+"  Bad transfer parameters - "+str); return; }

            line1 = strs[0].trim();   st1 = strs[1].trim();
            line2 = strs[2].trim();   st2 = strs[3].trim();
            if( strs.length>4 ) {
                time = String2Time(strs[4]);
                if( strs.length>5 && strs[5].trim().toLowerCase().equals("invisible") )  invisible = true;
            }
        }

        public void setNums() {  // call this method after all TRPs loaded for set stations numbers
            StationsNum sn1 = TRP.getLineNum(line1);
            StationsNum sn2 = TRP.getLineNum(line2);
            if( sn1==null || sn2==null ) {
                Log.e("TRP /609","Wrong transfer Line name - " + line1+" "+line2);
                isWrong = true;
                return;
            }

            trp1num = sn1.trp;
            trp2num = sn2.trp;
            line1num = sn1.line;
            line2num = sn2.line;
            //noinspection ConstantConditions
            st1num = getTRP(trp1num).getLine(line1num).getStationNum(st1);
            //noinspection ConstantConditions
            st2num = getTRP(trp2num).getLine(line2num).getStationNum(st2);
            if( trp1num!=trp2num )  invisible = true;
            if( trp1num==-1 || line1num==-1 || st1num==-1 || trp2num==-1 || line2num==-1 || st2num==-1 ) isWrong=true;
        }

        public boolean isCorrect() {
            return  !isWrong;
        }
    }

    public int load(String name) {
        if( super.load(name)<0 ) return -1;
        Parsing();
        return 0;
    }

    void Parsing()  {  // parsing TRP file
        int i;
        TRP_line ll;

        if( getSec("Options")!=null )
            Type = getSec("Options").getParamValue("Type");
        else
            Type = name.substring(0,name.lastIndexOf("."));

        ArrayList<TRP_line> la = new ArrayList<>();
        for( i=0; i< secsNum(); i++) {
            if( getSec(i).name.equals("Options") )  continue;
            if( !getSec(i).name.startsWith("Line") )  break;
            ll = new TRP_line();
            ll.Load( getSec(i) );
            la.add(ll);
        }
        lines = la.toArray(new TRP_line[la.size()]);

        Section sec = getSec("Transfers"); // load transfers
        ArrayList<Transfer> ta = new ArrayList<>();
        if( sec!=null )
            for( i=0; i<sec.ParamsNum(); i++ )
                ta.add( new Transfer(sec.getParam(i).value) );  // sec.getParam(i).name,
        transfers = ta.toArray( new Transfer[ta.size()] );

        //  = getSec("AdditionalInfo");  todo
        secs=null;
    }

    public static StationsNum getLineNum(String name)  {
        for( int i=0; i< trpList.length; i++ ) {
            if( trpList[i].lines == null ) return null;
            for (int j = 0; j < trpList[i].lines.length; j++)
                if( trpList[i].lines[j].name.equals(name) ) return new StationsNum(i, j, -1);
        }

        return null;
    }

    public static TRP_line getLine(String name)  {
        for( TRP tt : trpList  ) {
            if( tt.lines == null ) return null;
            for( TRP_line tl : tt.lines )
                if( tl.name.equals(name) ) return tl;
        }
        return null;
    }

    public static TRP_line getLine(int tr, int ln)  {
        TRP tt = trpList[tr];
        return tt.getLine(ln);
    }

    public TRP_line getLine(int ln)  {
        if( lines==null || lines.length<ln || ln<0 )  return null;
        return lines[ln];
    }

    public static Transfer[] getTransfers(int trp, int line, int stn) {
        LinkedList<Transfer> listT = new LinkedList<>();
        int i=0;

        for( TRP tt : TRP.trpList )
            for( TRP.Transfer trn : tt.transfers )
                if( (trn.trp1num==trp && trn.line1num==line && trn.st1num==stn)
                  || (trn.trp2num==trp && trn.line2num==line && trn.st2num==stn) ) { listT.add(trn); i++; }

        if( i>0 )
            return listT.toArray( new Transfer[i] );

        return null;
    }

    public static Transfer getTransfer(StationsNum ls1, StationsNum ls2) {

        for( TRP tt : TRP.trpList )
            for( TRP.Transfer trn : tt.transfers ) {
                if(  (trn.trp1num == ls1.trp && trn.line1num == ls1.line && trn.st1num == ls1.stn)
                  && (trn.trp2num == ls2.trp && trn.line2num == ls2.line && trn.st2num == ls2.stn) ) return trn;
                if(  (trn.trp1num == ls2.trp && trn.line1num == ls2.line && trn.st1num == ls2.stn)
                  && (trn.trp2num == ls1.trp && trn.line2num == ls1.line && trn.st2num == ls1.stn) ) return trn;
            }

        return null;
    }

    //public static TRP_Station getStation(StationsNum ls)  {
    //    return trpList.get(ls.trp).getLine(ls.line).getStation(ls.stn);
    //}

    public static TRP_Station getStation(int t, int l, int s)  {
        return trpList[t].getLine(l).getStation(s);
    }

    public static String getStationName(StationsNum ls)  {
        return trpList[ls.trp].getLine(ls.line).getStationName(ls.stn);
    }

    static synchronized void drawRoute(Canvas c, Paint p) {
        if( bestRoute==null ) return;
        bestRoute.Draw(c,p);
    }

    static void DrawTransfers(Canvas c, Paint p, MAP map) {
        PointF p1, p2;
        Line   ll;
        if( pline==null ) {
            pline = new Paint(p);
            pline.setStyle(Paint.Style.STROKE);
        }

        p.setColor(0xff000000);
        pline.setColor(0xff000000);
        pline.setStrokeWidth(map.LinesWidth+6);
        for( int trpNum : TRP.allowedTRPs )  {   // draw black edging
            if( trpNum==-1 ) continue;
            TRP ttt = getTRP(trpNum);
            if( ttt==null ) continue;
            for( Transfer t : ttt.transfers) {
                if( t.invisible || !t.isCorrect() ) continue;

                if( (ll=map.getLine(t.trp1num,t.line1num))==null ) continue;
                if( ExtPointF.isNull(p1=ll.getCoord(t.st1num)) ) continue;

                if( (ll=map.getLine(t.trp2num,t.line2num))==null ) continue;
                if( ExtPointF.isNull(p2=ll.getCoord(t.st2num)) ) continue;

                c.drawCircle(p1.x, p1.y, map.StationRadius+3, p);
                c.drawCircle(p2.x, p2.y, map.StationRadius+3, p);
                c.drawLine( p1.x,p1.y, p2.x,p2.y, pline);
            }
        }

        p.setColor(0xffffffff);
        pline.setColor(0xffffffff);
        pline.setStrokeWidth(map.LinesWidth+4);
        for( int trpNum : TRP.allowedTRPs )  {   // draw white transfer
            if( trpNum==-1 ) continue;
            TRP ttt = getTRP(trpNum);
            if( ttt==null ) continue;
            for( Transfer t : ttt.transfers) {
                if( t.invisible || !t.isCorrect() ) continue;

                if( (ll=map.getLine(t.trp1num,t.line1num))==null ) continue;
                if( ExtPointF.isNull(p1=ll.getCoord(t.st1num)) ) continue;

                if( (ll=map.getLine(t.trp2num,t.line2num))==null ) continue;
                if( ExtPointF.isNull(p2=ll.getCoord(t.st2num)) ) continue;

                c.drawCircle(p1.x, p1.y, map.StationRadius+2, p);
                c.drawCircle(p2.x, p2.y, map.StationRadius+2, p);
                c.drawLine( p1.x,p1.y, p2.x,p2.y, pline);
            }
        }
    }
}
