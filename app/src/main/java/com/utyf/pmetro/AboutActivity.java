package com.utyf.pmetro;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.utyf.pmetro.map.MapData;

/**
 * Created by Utyf on 25.02.2015.
 *
 */

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView text = (TextView) findViewById(R.id.mapInfo);
        if( MapData.isReady )
            text.setText( MapData.cty.MapAuthors );

        text = (TextView) findViewById(R.id.version_num);
        text.setText( MapActivity.versionNum );

        text = (TextView) findViewById(R.id.build);
        text.setText( "build "+MapActivity.buildNum );
        showDedug();
    }

    private int tapNum = 0;
    private long firstTap = 0;

    public void versionClick(View v) {

        long currentTap = System.currentTimeMillis();
        if( firstTap==0 ) {
            firstTap = currentTap;
            tapNum = 1;
            return;
        }
        if( currentTap-firstTap<5000 ) {
            if( ++tapNum>=5 ) {
                firstTap = 0;
                MapActivity.debugMode = !MapActivity.debugMode;
                showDedug();
            }
        } else  firstTap = 0;
    }

    private void showDedug() {
        if( MapActivity.debugMode ) {
            TextView text = (TextView)findViewById(R.id.deviceInfo);
            //noinspection deprecation
            text.setText(Html.fromHtml("Device information"
                    + "<br/><br/>CPU number: <font color = 'blue';>" + MapActivity.numberOfCores
                    + "</font><br/>Max RAM : <font color = 'blue';>" + MapActivity.maxMemory
                    + "</font><br/>Model : <font color = 'blue';>" + Build.MODEL
                    + "</font><br/>Board : <font color = 'blue';>" + Build.BOARD
                    + "</font><br/>Brand : <font color = 'blue';>" + Build.BRAND
                    + "</font><br/>Manufacturer : <font color = 'blue';>" + Build.MANUFACTURER
                    + "</font><br/>Hardware : <font color = 'blue';>" + Build.HARDWARE
                    + "</font><br/>Device : <font color = 'blue';>" + Build.DEVICE
                    + "</font><br/>Product : <font color = 'blue';>" + Build.PRODUCT
                    + "</font><br/>Display : <font color = 'blue';>" + Build.DISPLAY
                    + "</font><br/>FingerPrint : <font color = 'blue';>" + Build.FINGERPRINT
                    + "</font><br/>ID : <font color = 'blue';>" + Build.ID
                    + "</font><br/>TAGS : <font color = 'blue';>" + Build.TAGS
                    + "</font><br/>Type : <font color = 'blue';>" + Build.TYPE
                    + "</font><br/>CPU_ABI : <font color = 'blue';>" + Build.CPU_ABI
//                        + "</font><br/>Time : <font color = 'blue';>" + Build.TIME
                    + "<br/>"
                    + "</font><br/>Android Version : <font color = 'blue';>" + Build.VERSION.RELEASE
                    + "</font><br/>API Level : <font color = 'blue';>" + Build.VERSION.SDK_INT
                    + "</font><br/>CodeName : <font color = 'blue';>" + Build.VERSION.CODENAME
                    + "</font><br/>INCREMENTAL : <font color = 'blue';>" + Build.VERSION.INCREMENTAL + "</font>"));
        } else ((TextView)findViewById(R.id.deviceInfo)).setText("");
    }
/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    } */
}
