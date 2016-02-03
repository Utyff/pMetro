package com.utyf.pmetro.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.utyf.pmetro.MapActivity;

/**
 * Created by Utyf on 07.01.2016.
 *
 */


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        //SimpleDateFormat sdf = new SimpleDateFormat("HH mm ss");
        //Toast.makeText(arg0, "I'm running "+sdf.format(new Date()), Toast.LENGTH_SHORT).show();

        SET.load(arg0);
        MapActivity.setDirs(arg0);

        int count=3;   // try to update 3 times
        while( !CatalogList.updateAll(true,arg0) && --count>0 )
            try {
                Thread.sleep(5*60*1000);  // timeout 5 minutes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

    }
}
