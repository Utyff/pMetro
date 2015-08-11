package com.utyf.pmetro.settings;

import android.widget.Toast;

import com.utyf.pmetro.MapActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Utyf on 16.04.2015.
 *
 */

public class MapList {
    static ArrayList<MapFile> mapFiles;

    static boolean isLoaded() {
        return mapFiles!=null;
    }

    static void loadData() {
        MapFile mpf;
        File catDir = MapActivity.catalogDir;

        mapFiles = null;

        File[] fls = catDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) { return name.toLowerCase().endsWith(".pmz"); }
            });

        if( fls==null ) return;

        mapFiles = new ArrayList<>();
        for( File fl : fls ) {
            if( !fl.isFile() ) continue;

            mpf = loadPMZ(fl);
            if( mpf==null ) continue;

            mapFiles.add( getIndex(mpf.cityName), mpf );
        }
    }

    static MapFile loadPMZ( File fl ) {
        MapFile    mpf;

        mpf = new MapFile();
        mpf.fileName = fl.getAbsolutePath();
        mpf.fileShortName = fl.getName();
        mpf.size = fl.length();
//        mpf.date = Util.milli2string( getLastModification(fl) );

        mpf.mapName = mpf.fileShortName.substring( 0,mpf.fileShortName.indexOf('.') );
        mpf.cityName = mpf.mapName;

/*        String[]  strs;
        strs = zipMap.getFileList(".cty", fl.getName());  // .CTY must be only one
        if( strs==null || strs.length!=1 )  return null;

        Parameters pr = new Parameters( fl.getAbsolutePath() );

        if( pr.loadFile("metro.map")==null ) return null;  // show only main map-file
        if( pr.load(strs[0])<0 )             return null;

        Section sec = pr.getSec("Options");
        if( sec==null ) return null;

        mpf.mapName = sec.getParamValue("Name");
        mpf.comment = sec.getParamValue("Comment");
        mpf.cityName = sec.getParamValue("CityName");
        mpf.country = sec.getParamValue("Country");  //*/

        return mpf;
    }

    static long getLastModification(File fl) {

        long           time=0;
        ZipInputStream zis;
        ZipEntry       ze;

        try {
            zis = new ZipInputStream(new FileInputStream(fl));
            while( (ze = zis.getNextEntry()) != null )    // looking for last modified file
                if( ze.getTime()>time )  time = ze.getTime();
        } catch ( IOException e ) {
            return 0;
        }
        return time; // fl.lastModified();
    }

    static int getIndex(String city) {
        int i;

        for( i=0; i<mapFiles.size(); i++ )
            if( mapFiles.get(i).cityName.compareToIgnoreCase(city)>0 ) return i;

        return i;
    }

    public static void deleteFile(int pos) {
        if( !isLoaded() ) return;  // if list not loaded - do nothing
        if( !new File(mapFiles.get(pos).fileName).delete() )
            Toast.makeText(SettingsActivity.listAct.get(0), "Can't delete file.", Toast.LENGTH_LONG).show();
        CatalogManagement.cat.catalogMapUpdate();
    }

//    MapFile getMapFile(int pos) {
//        if( !isLoaded() ) return null;
//        return mapFiles.get(pos);
//    }
}
