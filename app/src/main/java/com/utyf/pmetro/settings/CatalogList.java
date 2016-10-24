package com.utyf.pmetro.settings;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.R;
import com.utyf.pmetro.util.ExtInteger;
import com.utyf.pmetro.util.Util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Code for load and decode catalog file (Files.xml) and load maps
 *
 * Created by Utyf on 14.04.2015.
 */

public class CatalogList {
    private static int    dataVersion;
    private static long   date;
    //private static String status;
    static ArrayList<ArrayList<CatalogFile>> catFilesGroup;
    static ArrayList<String> countries;

    private static Timer   timer;
    private static String  downloadFile, downloadPMZ;

    private static boolean isReady() {
        return dataVersion==1 && date!=0;
    }

    static boolean isLoaded() {
        return countries!=null && catFilesGroup!=null;
    }

    private static class taskCatLoad extends TimerTask {
        public void run() {
            if( timer==null ) return; // wrong call

            if( DownloadFile.status==1 ) {  // keep waiting
                if( CatalogManagement.cat!=null ) {
                    Message msg = CatalogManagement.cat.pbHandler.obtainMessage(1, DownloadFile.loaded, DownloadFile.size);
                    CatalogManagement.cat.pbHandler.sendMessage(msg);
                }
                return;
            }

            timer.cancel(); // loading finished

            if( DownloadFile.status==0 ) {
                DownloadFile.moveFile(MapActivity.shortCatalogFile);
                //status = "Ok.";
                loadFileInfo();
                loadData();
            } else {
                DownloadFile.status = 0;
                //status = "Fail.";
            }
            if( CatalogManagement.cat!=null )
                CatalogManagement.cat.pbHandler.sendEmptyMessage(2);

            timer = null;
        }
    }

    private static class taskMapLoad extends TimerTask {
        public void run() {
            if( timer==null ) return; // wrong call

            if( DownloadFile.status==1 ) {  // keep waiting
                if( CatalogManagement.cat!=null ) {
                    Message msg = CatalogManagement.cat.pbHandler.obtainMessage(1, DownloadFile.loaded, DownloadFile.size);
                    CatalogManagement.cat.pbHandler.sendMessage(msg);
                }
                return;
            }

            timer.cancel(); // loading finished

            if( DownloadFile.status==0 ) {
                DownloadFile.unzipFile(downloadFile, downloadPMZ);
                //status = "Ok.";
                //loadFileInfo();
            } else {
                DownloadFile.status = 0;
                //status = "Fail.";
            }
            if( CatalogManagement.cat!=null )
                CatalogManagement.cat.pbHandler.sendEmptyMessage(3);

            timer = null;
        }
    }

    private static boolean downloadCat(boolean quite, Context cntx) {
        if( timer==null ) {
            //status = "loading..";
            if (CatalogManagement.cat != null)
                CatalogManagement.cat.pbHandler.sendEmptyMessage(0);

            if( !DownloadFile.start(SET.site + SET.catalogList, quite, cntx) ) {
                if (CatalogManagement.cat != null)
                    CatalogManagement.cat.pbHandler.sendEmptyMessage(4);
                return false;
            }

            timer = new Timer();
            timer.scheduleAtFixedRate(new taskCatLoad(), 0, 100);
            return true;
        }
        //DownloadFile.stopRequest = true;
        return false;
    }

    private static Thread thrUpdate;

    static boolean startUpdate(final boolean quite, final Context cntx) {
        if( thrUpdate!=null && thrUpdate.isAlive() ) return false;

        thrUpdate = new Thread(new Runnable() {
            public void run() {
                updateAll(quite, cntx);
            }
        });
        thrUpdate.start();
        return true;
    }

    public static boolean updateAll(boolean quite, Context cntx) {
Log.e("CatalogList","Start UPDATE tread");
        if( quite && !checkLastUpdate(20*60*60*1000) ) return true; // in quite mode, minimum update period 20 hours

        if( !downloadCat(quite, cntx) ) return false; // start download new Files.xml

        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while( timer!=null );  // wait until download complete

        if( !isReady() ) return false;                // check for load Files.xml succeed
        if( !MapList.isLoaded() ) MapList.loadData(); // Get list of all local maps
        if( !MapList.isLoaded() || catFilesGroup==null ) return false;

        for( ArrayList<CatalogFile> cntry : catFilesGroup )  // check all local maps for update
            for( CatalogFile cty : cntry )
                for( MapFile mf : MapList.mapFiles )
                    if ( mf.fileShortName.equals(cty.PmzName) ) { //&& cty.ZipDate>SET.cat_date_last )
                        Log.e("CatalogList", "name: " + cty.PmzName + " time: " + cty.ZipDate + ", Catalog time:" + SET.cat_date_last);
                        if( cty.ZipDate>SET.cat_date_last)        // if the new files.xml date later than the time of the last update
                            updateMap(cty, quite, cntx);          // download updated map
                        break;
                    }

        SET.cat_date_last = date;
        SET.save();
Log.e("CatalogList", "Stop UPDATE tread");
        return true;
    }

    private static void updateMap(CatalogFile cf, boolean quite, Context cntx) {
        Log.e("CatalogList","Start MAP update tread - "+cf.ZipName);
        downloadMap(cf, quite, cntx);

        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while( timer!=null );

        Log.e("CatalogList","Stop MAP update tread");
        // TODO  reload map if it open now
    }

    static void downloadMap(CatalogFile cf) {
        downloadMap(cf, false, MapActivity.mapActivity);
    }

    //public static void downloadMap(int countryNum, int fileNum, boolean quite, Context cntx) {
    private static void downloadMap(CatalogFile cf, boolean quite, Context cntx) {
        if( !Util.isOnline(quite,cntx) ) return; // check internet access
        if( !isLoaded() || timer!=null ) return;
        //Log.w("Download", fileNum+" - "+countryNum);
        //status = "loading..";
        if( CatalogManagement.cat!=null )
            CatalogManagement.cat.pbHandler.sendEmptyMessage(0);

        downloadFile = cf.ZipName;
        downloadPMZ  = cf.PmzName;
        DownloadFile.start(SET.site + SET.mapPath + "/" + downloadFile, quite, cntx);

        timer = new Timer();
        timer.scheduleAtFixedRate(new taskMapLoad(), 0, 100);
    }

    private static boolean checkLastUpdate(long max) {  // true for allow update
        File fl = new File (MapActivity.catalogFile);
        return fl.lastModified() < System.currentTimeMillis()+max;
    }

    static String getLastChanges() {
        if( date==0 ) loadFileInfo();
        if( date==0 ) return MapActivity.mapActivity.getString(R.string.no_data);

        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new java.util.Date(date));
    }

    static String getLastUpdate() {
        File fl = new File(MapActivity.catalogFile);
     //   Log.w("CatList", "File - " + fl.getAbsoluteFile() ); //MapActivity.fileDir + "/Files.xml");
        if( !fl.exists() ) return MapActivity.mapActivity.getString(R.string.no_data);

        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new java.util.Date(fl.lastModified()));
    }

    static CatalogFile getCatFile(int countryNum, int fileNum) {
        if( isLoaded() ) return catFilesGroup.get(countryNum).get(fileNum);
        return null;
    }

    static void loadFileInfo() {
        dataVersion = 0;
        date = 0;

        try {
            XmlPullParser xpp = prepareXpp();
            if( xpp==null ) return;

            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_TAG: // начало тэга
                        if( xpp.getName().toLowerCase().equals("filelist") )
                            for( int i=0; i<xpp.getAttributeCount(); i++ )
                                switch( xpp.getAttributeName(i) ) {
                                    case "DataVersion":
                                        dataVersion = ExtInteger.parseInt(xpp.getAttributeValue(i));
                                        break;
                                    case "Date":
                                        date = date2long( ExtInteger.parseInt(xpp.getAttributeValue(i)) );
                                        break;
                                }
                        break;
                }
                xpp.next();  // следующий элемент
            }
        } catch ( XmlPullParserException | IOException | NullPointerException e ) {
            Log.e("XML /177", e.toString());
        }

        //if( date==0 )          status = "Bad catalog data";
        //if( dataVersion!=1 )   status = "Bad catalog version";
    }

    static void loadData() {
        int i, jj;
        CatalogFile cFile=null;

        if( !isReady() ) return;
        catFilesGroup = new ArrayList<>();
        countries = new ArrayList<>();

        try {
            XmlPullParser xpp = prepareXpp();
            if( xpp==null ) return;

            while( xpp.getEventType()!=XmlPullParser.END_DOCUMENT ) {
                switch (xpp.getEventType()) {
                    case XmlPullParser.START_TAG:
                        switch( xpp.getName().toLowerCase() ) {
                            case "file":
                                cFile = new CatalogFile();
                                break;
                            case "zip":
                                if( cFile!=null )
                                    for( i=0; i<xpp.getAttributeCount(); i++ )
                                        switch( xpp.getAttributeName(i) ) {
                                            case "Name":
                                                cFile.ZipName = xpp.getAttributeValue(i);
                                                break;
                                            case "Size":
                                                cFile.ZipSize = ExtInteger.parseInt(xpp.getAttributeValue(i));
                                                break;
                                            case "Date":
                                                cFile.ZipDate = date2long( ExtInteger.parseInt(xpp.getAttributeValue(i)) );
                                                break;
                                        }
                                break;
                            case "pmz":
                                if( cFile!=null )
                                    for( i=0; i<xpp.getAttributeCount(); i++ )
                                        switch( xpp.getAttributeName(i) ) {
                                            case "Name":
                                                cFile.PmzName = xpp.getAttributeValue(i);
                                                jj = cFile.PmzName.lastIndexOf(".");
                                                if( jj!=-1 ) cFile.MapName = cFile.PmzName.substring(0,jj);
                                                else cFile.MapName = cFile.PmzName;
                                                break;
                                            case "Size":
                                                cFile.PmzSize = ExtInteger.parseInt(xpp.getAttributeValue(i));
                                                break;
                                            case "Date":
                                                cFile.PmzDate = date2long( ExtInteger.parseInt(xpp.getAttributeValue(i)) );
                                                break;
                                        }
                                break;
                            case "city":
                                if( cFile!=null )
                                    for( i=0; i<xpp.getAttributeCount(); i++ )
                                        switch( xpp.getAttributeName(i) ) {
                                            case "Name":
                                                cFile.Name = xpp.getAttributeValue(i);
                                                break;
                                            case "CityName":
                                                cFile.CityName = xpp.getAttributeValue(i);
                                                break;
                                            case "Country":
                                                cFile.Country = xpp.getAttributeValue(i);
                                                break;
                                        }
                                break;
                            case "map":
                                if( cFile!=null )
                                    if( xpp.getAttributeName(0).equals("Comment") )
                                        cFile.MapComment = xpp.getAttributeValue(0);
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if( !xpp.getName().toLowerCase().equals("file") ) break;
                        if( cFile==null || cFile.Country.equals(" языки") || cFile.Country.equals(" ѕрограмма") ) break;  // skip languages files and program binary
                        if( cFile.PmzName.equals("Moscow3d.pmz") || cFile.PmzName.equals("MoscowGrd.pmz") || cFile.PmzName.equals("MoscowHistory.pmz")
                                || cFile.PmzName.equals("MoscowTrams.pmz") || cFile.PmzName.equals("MoscowTrolleys.pmz")
                                || cFile.PmzName.equals("MoscowZelBuses.pmz") ) break;  // skip maps extensions
                        if( cFile.Country.startsWith(" ") ) Log.e("Catalog /264", "Country name starts with space - "+cFile.Country);
                        i = findCountryPosition(cFile.Country);
                        jj = findCityPosition(i,cFile.CityName);
                        catFilesGroup.get(i).add(jj,cFile);
                        cFile = null;
                        break;
                }
                xpp.next();
            }
        } catch ( XmlPullParserException | IOException | NullPointerException e ) {
            Log.e("XML /274", e.toString());
        }
    }

    private static int findCountryPosition(String cntry) {
        int i, res;
        for( i=0; i<countries.size(); i++ ) {
            res = countries.get(i).compareToIgnoreCase(cntry);
            if( res<0 ) continue;
            if( res==0 ) return i;
            if( res>0 ) break;
        }
        countries.add(i,cntry);
        catFilesGroup.add( i, new ArrayList<CatalogFile>() );
        return i;
    }

    private static int findCityPosition(int cPos, String city) {
        int i;
        ArrayList<CatalogFile> catalogFiles = catFilesGroup.get(cPos);

        for( i=0; i<catalogFiles.size(); i++ )
            if( catalogFiles.get(i).CityName.compareToIgnoreCase(city)>0 ) return i;

        return i;
    }

    private static long date2long(long date) { // convert Delphi date to java milliseconds
        return (date - 25569L) * 86400L * 1000L;
    }

    private static XmlPullParser prepareXpp() {
        FileInputStream in;
        XmlPullParser   xpp;
        XmlPullParserFactory factory;

        try {
            factory = XmlPullParserFactory.newInstance(); // получаем фабрику
          //  factory.setNamespaceAware(true); // включаем поддержку namespace (по умолчанию выключена)
            xpp = factory.newPullParser();   // создаем парсер
            in = new FileInputStream(MapActivity.catalogFile);
            xpp.setInput( in, null );
            return xpp;
        } catch (XmlPullParserException | FileNotFoundException e) {
            Log.e("XML /318", e.toString());
        }
        return null;
    }

    static void eraseData() {
        catFilesGroup = null;
        countries = null;
    }
}
