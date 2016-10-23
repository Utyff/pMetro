package com.utyf.pmetro.map;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.utyf.pmetro.R;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;


public class StationInfoActivity extends AppCompatActivity {

    StationsNum numStation;
    StationSchemaView schView;
    ArrayList<StationSchemaView> listVw;
    StationData  stationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        numStation = new StationsNum( intent.getIntExtra("trp",-1),
                intent.getIntExtra("line",-1), intent.getIntExtra("station",-1) );

        ActionBar a = getSupportActionBar();
        if( a!=null )  //noinspection deprecation
            a.setIcon( new ColorDrawable(getResources().getColor(android.R.color.transparent)) );
/*        ColorDrawable cd = new ColorDrawable(getResources().getColor(android.R.color.transparent));
        cd.setBounds(0,0,0,0);
        getActionBar().setIcon(cd); */

        try { // Bug #12
            setTitle(TRP.getStationName(numStation));
        } catch (Exception e) {
            this.finish();
        }
//        stationSchemaView = new StationSchemaView(this, numStation);
//        setContentView(stationSchemaView);
        setContentView(R.layout.station_info);

        stationData = new StationData();
        stationData.load(numStation);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHostStation);
        tabHost.setup();
        TabHost.TabSpec tabSpec;

        listVw = new ArrayList<>();
        for( int i=0; i<stationData.vecs.size(); i++ ) {
            schView = new StationSchemaView(this, stationData.vecs.get(i));
            listVw.add(schView);
            tabSpec = tabHost.newTabSpec("schema");
            tabSpec.setIndicator(stationData.vecsCap.get(i));
            tabSpec.setContent(new TabHost.TabContentFactory() {
                @Override
                public View createTabContent(String tag) { return schView; }
            });
            tabHost.addTab(tabSpec);
        }

        String infoStr="";
        if( stationData.items.size()>0 ) {
            tabSpec = tabHost.newTabSpec("info");
            tabSpec.setIndicator("Information");
            tabSpec.setContent(R.id.scrollView);
            tabHost.addTab(tabSpec);

            TextView tv = (TextView)findViewById(R.id.textViewInfo);
            for( StationData.InfoItem ii : stationData.items )
                infoStr = infoStr + "<font color = 'blue';>" + ii.caption + "</font><br/><br/>" + ii.text.replaceAll("\n","<br/>") + "<br/><br/>";
            //noinspection deprecation
            tv.setText(Html.fromHtml(infoStr));
        }
        if( infoStr.length()==0 && listVw.size()==0 ) {
            Toast.makeText(this,getString(R.string.no_data),Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

/*    @Override
    public Intent  getSupportParentActivityIntent () {
        return super.getSupportParentActivityIntent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_station_info, menu);
        return true;
    }
*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
    //    if (id == R.id.action_settings) {
     //       return true;
      //  }

        return super.onOptionsItemSelected(item);
    }
}
