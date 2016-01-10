package com.utyf.pmetro.settings;

import android.content.Context;
import android.util.Log;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.util.Util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Utyf on 12.04.2015.
 *
 */

class DownloadFile {
    public static boolean res, stopRequest;
    public static int     status, loaded, size;
    public static long    timeStart, timeLast;
    public static String  errMessage="";
    static Thread  thr;

/*    private static void td() {

        try {
            URL u = new URL("http://www.whatsmyuseragent.com/");

            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestProperty("Accept-Encoding", "identity");
            //String agent1 = System.getProperty("http.agent", "");
            //String agent2 = connection.getRequestProperty("User-Agent");
            // connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_5_8; en-US) AppleWebKit/532.5 (KHTML, like Gecko) Chrome/4.0.249.0 Safari/532.5");
            connection.setRequestProperty("User-Agent", "pMetro/1.0 (Android)");
            connection.connect();
            size = connection.getContentLength();
            //connection.setRequestProperty("User-Agent", "pMetro/1.0 (Android)");
            //Log.e("Download","file size - "+size);

            InputStream is = u.openStream();
            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int count;

            File cache = new File(MapActivity.fileDir.getAbsolutePath());
            res = cache.mkdirs();
            File outFile = new File(cache + "/whatsMyUserAgent.html");
            outFile.delete();
            FileOutputStream fos = new FileOutputStream(outFile);
            //Log.w("DOWNLOAD", "Dest path - "+cache);
            //Log.w("DOWNLOAD", "Dest file - "+cache + fileName);
            while ((count = dis.read(buffer))>0) {
                timeLast = System.currentTimeMillis();
                if( stopRequest ) break;
                fos.write(buffer, 0, count);
                loaded += count;
                Log.e("Download","loaded - "+loaded);
            }

        } catch (MalformedURLException mue) {
            Log.e("DOWNLOAD", "malformed url error - "+mue.toString(), mue);
        } catch (IOException ioe) {
            Log.e("DOWNLOAD", "io error - "+ioe.toString(), ioe);
        } catch (SecurityException se) {
            Log.e("DOWNLOAD", "security error - "+se.toString(), se);
        }
    }
*/
    private static void download(String url){
        timeStart = System.currentTimeMillis();
//td();
        try {
            errMessage = "";
            loaded = 0;
            timeLast = timeStart;
            //Log.w("DOWNLOAD", "url2 - "+url);
            String fileName = url.substring(url.lastIndexOf("/"));

            File cache = new File(MapActivity.cacheDir.getAbsolutePath());
            res = cache.mkdirs();

            URL u = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "pMetro/1.0 (Android; build " +MapActivity.buildNum+ ")");
            connection.connect();
            size = connection.getContentLength();
            //Log.e("Download","file size - "+size);

            InputStream is = u.openStream();
            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int count;

            File outFile = new File(cache + fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            //Log.w("DOWNLOAD", "Dest path - "+cache);
            //Log.w("DOWNLOAD", "Dest file - "+cache + fileName);
            while ((count = dis.read(buffer))>0) {
                timeLast = System.currentTimeMillis();
                if( stopRequest ) break;
                fos.write(buffer, 0, count);
                loaded += count;
                //Log.e("Download","loaded - "+loaded);
            }
            status = 0;
            if( stopRequest ) {
                fos.close();
                if( outFile.delete() ) Log.e("Download","Can`t delete file - " +outFile);
                status = 3;
            }
            //Log.w("DOWNLOAD", "End load. status - "+status+" loaded - "+loaded);

        } catch (MalformedURLException mue) {
            Log.e("DOWNLOAD", "Malformed url error - "+mue.toString(), mue);
            status =-1; errMessage = mue.toString();
        } catch (IOException ioe) {
            Log.e("DOWNLOAD", "IO error - "+ioe.toString(), ioe);
            status =-2; errMessage = ioe.toString();
        } catch (SecurityException se) {
            Log.e("DOWNLOAD", "Security error - "+se.toString(), se);
            status =-3; errMessage = se.toString();
        }

        timeLast = System.currentTimeMillis();
    }


    public static boolean start(final String url) {
        return start(url, false, MapActivity.mapActivity);
    }

    public static boolean start(final String url, boolean quite, Context cntx) {
        if( status!=0 || !Util.isOnline(quite,cntx) ) return false;
        status = 1;
        stopRequest = false;
        //Log.w("DOWNLOAD", "URL - "+url);
        thr = new Thread(new Runnable() {
            public void run() {
                download(url);
            }
        }); //.start();
        thr.start();
        return true;
    }

/*    @SuppressWarnings("deprecation")  // for Thread.stop()
    public static void stop() {
        stopRequest = true;
        try {
            // Thread.sleep(500);
            thr.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if( thr!=null && thr.isAlive() )  thr.stop();
        thr=null;
        status = 0;
    } //*/

    public static boolean unzipFile(String zipName, String fileName) {
        int          count;
        byte[]       bb = new byte[4096];       // buffer for file read
        String       destPath = MapActivity.catalogDir + "/";
        ZipInputStream zis;
        ZipEntry     ze, zz;
        InputStream  in;
        OutputStream out;

        try {
            File fl = new File(destPath);
            if( !fl.mkdirs() && !fl.isDirectory() ) return false;

            out = new FileOutputStream(destPath+fileName);
            in  = new FileInputStream(MapActivity.cacheDir+"/"+zipName);
            zis = new ZipInputStream(in);
            //Log.w("UNZIP", "OUT file - "+destPath+fileName);
            //Log.w("UNZIP", "ZIP file - "+MapActivity.cacheDir+"/"+zipName);

            ze=null;
            while( (zz=zis.getNextEntry()) != null )    // looking for file by name
                if( zz.getName().endsWith(fileName) )
                    { ze=zz; break; }                   // file is found.

            if( ze==null ) {
                MapActivity.errorMessage = "File not found in ZIP";
                return false;                           // if file was not found
            }

            while( (count=zis.read(bb)) > 0 )           // unzip and write file
                out.write(bb, 0, count);

            zis.close();
            out.flush(); // write the output file
            out.close();
            return new File(zipName).delete();  // delete source zip

        } catch (IOException e) {
            MapActivity.errorMessage = e.toString();
            return false;
        }
    }

    public static boolean moveFile(String fileName) {
        File from = new File(MapActivity.cacheDir.getAbsolutePath() + fileName);
        File to = new File(MapActivity.fileDir.getAbsolutePath() + "/catalog/"+fileName);
        return from.renameTo(to) ||
            moveFile(MapActivity.cacheDir+"/", fileName, MapActivity.fileDir+"/catalog/");
    }

    private static boolean moveFile(String inputPath, String inputFile, String outputPath) {
        InputStream in;
        OutputStream out;
        try {
            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if( !dir.mkdirs() && !dir.isDirectory() ) return false;

      //      Log.w("MOVE", "Srs file - "+inputPath + inputFile);
      //      Log.w("MOVE", "Dest file - "+outputPath + inputFile);
            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[4096];
            int count;
            while( (count = in.read(buffer)) != -1 )
                out.write(buffer, 0, count);

            in.close();
            out.flush(); // write the output file
            out.close();

            // delete the original file
            return new File(inputPath + inputFile).delete();

        } catch (FileNotFoundException e) {
            Log.e("MOVE File", e.toString() );
            return false;
        } catch (Exception e) {
            Log.e("MOVE File", e.toString() );
            return false;
        }
    }
}
