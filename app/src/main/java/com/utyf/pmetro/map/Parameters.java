package com.utyf.pmetro.map;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.settings.SET;

public class Parameters {
    public String   name;
    private String   zipFile;
    protected int      NameSeparator='=';  // default parameters name separator symbol "="
    private Section currentSection;
    public ArrayList<Section> secs; // = new ArrayList<>();

    public Parameters() {
        zipFile = MapActivity.catalogDir+"/"+SET.mapFile;
    }

    //public Parameters(String _zipFile) {
    //    zipFile = _zipFile;
    //}

    public int load(String filename) {
        byte[]      bb;     // buffer for file read
        String      str;    // file content

        bb = loadFile(filename);
        if( bb==null ) return -3;
        try {
            str = new String(bb, 0, bb.length, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            Log.e("Parameters /40","Wrong file encoding - " + filename);
            e.printStackTrace();
            return -2;
        }

        secs = new ArrayList<>();
        parseParameters(str);  // Parse file

        return 0;
    }

    public byte[] loadFile(String filename)  {
        int    size, count;
        ZipInputStream zis;
        ZipEntry    ze, zz;
        byte[]      bb;       // buffer for file read
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            if( zipFile==null || zipFile.isEmpty() ) return null; // zis = new ZipInputStream(MapActivity.asset.open("Moscow.pmz"));
            zis = new ZipInputStream( new BufferedInputStream(new FileInputStream(zipFile)) );

            ze=null;
            while( (zz=zis.getNextEntry()) != null )    // looking for file by name
                if( zz.getName().toLowerCase().equals(filename.toLowerCase()) )
                    { ze=zz; break; }                   // file is found.
            if( ze==null ) return null;                 // if file was not found

            name = ze.getName();
            size = (int) ze.getSize();
            bb = new byte[size*2];
            while( (count=zis.read(bb)) > 0 )           // read file to str
                baos.write(bb, 0, count);
            zis.close();
            return baos.toByteArray();

        } catch (IOException e) {
            MapActivity.errorMessage = e.toString();
            return null;
        }
    }

    private void parseParameters(String str) {
        int      i, i2;
        String   line;

        currentSection = null;
        str=str.replace("\r\n","\n");  // remove symbol \r
        str=str.replace("\r",  "\n");  // remove symbol \r

        i=0;
        while ( i<str.length() )
        {
            i2=str.indexOf('\n',i);                       // get one line (till symbol \n)
            if( i2 == -1 ) i2=str.length();               // if not found - to the end of file
            line = str.substring(i,i2).trim();

            if( !line.isEmpty() && line.charAt(0)!=';' )  // check for empty string or commented
                if( line.charAt(0)=='[' )                 // is it new section ?
                      addSection(line.substring(1, line.length() - 1));
                else  addParameter(line);

            i=i2+1; // set pointer to next line
        }
        currentSection = null;
    } // parseParameters()


    private void addSection(String SectionName) {
        currentSection = new Section(SectionName);
        secs.add(currentSection);
    }


    private void addParameter(String Line) {
        int i;

        if( currentSection==null ) addSection("");
        i=Line.indexOf(NameSeparator);  // looking for name separator
        if( i==-1 )                     // is there parameter name?
            currentSection.AddParameter( "", Line );
        else
            currentSection.AddParameter( Line.substring(0,i), Line.substring(i+1) );
    }

    int secsNum() {
       return secs.size();
    }

    Section getSec(int i)  {
        return secs.get(i);
    }

    public Section getSec(String name)  {
        if( secs==null ) return null;
        for( Section s : secs )
            if( name.toLowerCase().equals(s.name.toLowerCase()) )
                return s;

        return null;
    }

}
