package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.StationsNum;

/**
 * Created by Fedor on 26.10.2016.
 */
public class Transfer {
    public int trp1num = -1, line1num = -1, st1num = -1, trp2num = -1, line2num = -1, st2num = -1;
    String line1, st1, line2, st2;
    public float time;
    public boolean invisible = false;
    boolean isWrong;

    public Transfer(String str) {
        String[] strs = str.split(",");
        if (strs.length < 4) {
            Log.e("TRP /595", "Bad transfer parameters - " + str);
            return;
        }

        line1 = strs[0].trim();
        st1 = strs[1].trim();
        line2 = strs[2].trim();
        st2 = strs[3].trim();
        if (strs.length > 4) {
            time = TRP.String2Time(strs[4]);
            if (strs.length > 5 && strs[5].trim().toLowerCase().equals("invisible"))
                invisible = true;
        }
    }

    public void setNums(TRP_Collection transports) {  // call this method after all TRPs are loaded for set stations numbers
        StationsNum sn1 = transports.getLineNum(line1);
        StationsNum sn2 = transports.getLineNum(line2);
        if (sn1 == null || sn2 == null) {
            Log.e("TRP /609", "Wrong transfer Line name - " + line1 + " " + line2);
            isWrong = true;
            return;
        }

        trp1num = sn1.trp;
        trp2num = sn2.trp;
        line1num = sn1.line;
        line2num = sn2.line;
        //noinspection ConstantConditions
        st1num = transports.getTRP(trp1num).getLine(line1num).getStationNum(st1);
        //noinspection ConstantConditions
        st2num = transports.getTRP(trp2num).getLine(line2num).getStationNum(st2);
        if (trp1num != trp2num) invisible = true;
        if (trp1num == -1 || line1num == -1 || st1num == -1 || trp2num == -1 || line2num == -1 || st2num == -1)
            isWrong = true;
    }

    public boolean isCorrect() {
        return !isWrong;
    }
}
