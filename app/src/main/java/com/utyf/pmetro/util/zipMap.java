package com.utyf.pmetro.util;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.settings.SET;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Utyf on 03.03.2015.
 *
 */

public class zipMap {

    static public String[] getFileList(String ext)  {
       return getFileList(ext, SET.mapFile);
    }

    static public String[] getFileList(String ext, String _zFile)  {
        String[]       names;
        ZipInputStream zis;
        ZipEntry       ze;
        LinkedList<String> strs = new LinkedList<>();

        if( _zFile==null || _zFile.isEmpty() ) return null;
        String  zFile = MapActivity.catalogDir+"/"+_zFile;

        try {
            zis = new ZipInputStream( new BufferedInputStream(new FileInputStream(zFile)) );

            while( (ze = zis.getNextEntry()) != null )    // get file names
                if (ze.getName().toLowerCase().endsWith(ext))
                    strs.add(ze.getName());

            names = strs.toArray( new String[strs.size()] );
            zis.close();
        } catch (IOException e) {
            MapActivity.errorMessage = e.toString();
            return null;
        }

        return names;
    }

}
