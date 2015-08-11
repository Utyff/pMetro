package com.utyf.pmetro.settings;

import android.os.Message;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.R;
import com.utyf.pmetro.util.ExtInteger;

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
 * Created by Utyf on 14.04.2015.
 *
 */

public class CatalogList {
    static int    dataVersion;
    static long   date;
    static String status;
    static ArrayList<ArrayList<CatalogFile>> catFilesGroup;
    static ArrayList<String> countries;

    static Timer   timer;
    static String downloadFile, downloadPMZ;
    final static String catalogFile = "/catalog/Files.xml";

    static boolean isReady() {
        return dataVersion==1 && date!=0;
    }

    static boolean isLoaded() {
        return countries!=null && catFilesGroup!=null;
    }

    static class taskCatLoad extends TimerTask {
        public void run() {
            if( timer==null ) return; // wrong calling

            if( DownloadFile.status==1 ) {  // keep waiting
                if( CatalogManagement.cat!=null ) {
                    Message msg = CatalogManagement.cat.pbHandler.obtainMessage(1, DownloadFile.loaded, DownloadFile.size);
                    CatalogManagement.cat.pbHandler.sendMessage(msg);
                }
                return;
            }

            timer.cancel(); // loading finished
            timer = null;

            if( DownloadFile.status==0 ) {
                DownloadFile.moveFile("Files.xml");
                status = "Ok.";
                loadFileInfo();
            } else {
                DownloadFile.status = 0;
                status = "Fail.";
            }
            CatalogManagement.cat.pbHandler.sendEmptyMessage(2);
        }
    }

    static class taskMapLoad extends TimerTask {
        public void run() {
            if( timer==null ) return; // wrong calling

            if( DownloadFile.status==1 ) {  // keep waiting
                if( CatalogManagement.cat!=null ) {
                    Message msg = CatalogManagement.cat.pbHandler.obtainMessage(1, DownloadFile.loaded, DownloadFile.size);
                    CatalogManagement.cat.pbHandler.sendMessage(msg);
                }
                return;
            }

            timer.cancel(); // loading finished
            timer = null;

            if( DownloadFile.status==0 ) {
                DownloadFile.unzipFile(downloadFile, downloadPMZ);
                status = "Ok.";
                loadFileInfo();
            } else {
                DownloadFile.status = 0;
                status = "Fail.";
            }
            CatalogManagement.cat.pbHandler.sendEmptyMessage(3);
        }
    }

    static void downloadCat() {
        if( timer==null ) {
            status = "loading..";
            if (CatalogManagement.cat != null)
                CatalogManagement.cat.pbHandler.sendEmptyMessage(0);

            DownloadFile.start("http://pmetro.su/Files.xml");

            timer = new Timer();
            timer.scheduleAtFixedRate(new taskCatLoad(), 0, 100);
        } else
            DownloadFile.stopRequest = true;
    }

    public static void downloadMap(int countryNum, int fileNum) {
        if( !MapActivity.isOnline(false) ) return; // check inet access
        if( !isLoaded() || timer!=null ) return;
        //Log.w("Download", fileNum+" - "+countryNum);
        status = "loading..";
        if( CatalogManagement.cat!=null )
            CatalogManagement.cat.pbHandler.sendEmptyMessage(0);

        downloadFile = catFilesGroup.get(countryNum).get(fileNum).ZipName;
        downloadPMZ  = catFilesGroup.get(countryNum).get(fileNum).PmzName;
        DownloadFile.start("http://pmetro.su/" + downloadFile);

        timer = new Timer();
        timer.scheduleAtFixedRate(new taskMapLoad(), 0, 100);
    }

    static String getLastChanges() {
        if( date==0 ) loadFileInfo();
        if( date==0 ) return MapActivity.mapActivity.getString(R.string.no_data);

        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new java.util.Date(date));
    }

    static String getLastUpdate() {
        File fl = new File(MapActivity.fileDir+ catalogFile);
     //   Log.w("CatList", "File - " + fl.getAbsoluteFile() ); //MapActivity.fileDir + "/Files.xml");
        if( !fl.exists() ) return MapActivity.mapActivity.getString(R.string.no_data);

        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new java.util.Date(fl.lastModified()));
    }

    static CatalogFile getCatFile(int countryNum, int fileNum) {
        if( isLoaded() ) return catFilesGroup.get(countryNum).get(fileNum);
        return null;
    }

    private static void loadFileInfo() {
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
            Log.e("XML /111", e.toString());
        }

        if( date==0 )          status = "Bad catalog data";
        if( dataVersion!=1 )   status = "Bad catalog version";
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
                        if( cFile.Country.startsWith(" ") ) Log.e("Catalog /202", "Country name starts with space - "+cFile.Country);
                        i = findCountryPosition(cFile.Country);
                        jj = findCityPosition(i,cFile.CityName);
                        catFilesGroup.get(i).add(jj,cFile);
                        cFile = null;
                        break;
                }
                xpp.next();
            }
        } catch ( XmlPullParserException | IOException | NullPointerException e ) {
            Log.e("XML /273", e.toString());
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
        catFilesGroup.add(i, new ArrayList<CatalogFile>() );
        return i;
    }

    private static int findCityPosition(int cPos, String city) {
        int i;
        ArrayList<CatalogFile> catalogFiles = catFilesGroup.get(cPos);

        for( i=0; i<catalogFiles.size(); i++ )
            if( catalogFiles.get(i).CityName.compareToIgnoreCase(city)>0 ) return i;

        return i;
    }

    static long date2long(long date) { // convert Delphi date to java milliseconds
        return (date - 25569l) * 86400l * 1000l;
    }

    static XmlPullParser prepareXpp() {
        FileInputStream in;
        XmlPullParser   xpp;
        XmlPullParserFactory factory;

        try {
            factory = XmlPullParserFactory.newInstance(); // получаем фабрику
          //  factory.setNamespaceAware(true); // включаем поддержку namespace (по умолчанию выключена)
            xpp = factory.newPullParser();   // создаем парсер
            in = new FileInputStream(MapActivity.fileDir + catalogFile);
            xpp.setInput( in, null );
            return xpp;
        } catch (XmlPullParserException | FileNotFoundException e) {
            Log.e("XML /221", e.toString());
        }
        return null;
    }

    public static void eraseData() {
        catFilesGroup = null;
        countries = null;
    }
}
