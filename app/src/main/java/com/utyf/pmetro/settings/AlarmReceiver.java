package com.utyf.pmetro.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Utyf on 07.01.2016.
 *
 */


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        // For our recurring task, we'll just display a message
        SimpleDateFormat sdf = new SimpleDateFormat("HH mm ss");
        Toast.makeText(arg0, "I'm running "+sdf.format(new Date()), Toast.LENGTH_SHORT).show();
    }
}
