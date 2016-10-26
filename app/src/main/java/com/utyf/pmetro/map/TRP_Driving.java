package com.utyf.pmetro.map;

/**
 * Created by Fedor on 26.10.2016.
 */
public class TRP_Driving {
    public int frwStNum = -1, bckStNum = -1;
    String frwST, bckST;
    public float frwDR = -1, bckDR = -1;

    TRP_Driving(String n1, String n2) {
        frwST = n1;
        bckST = n2;
        if (frwST.length() > 1 && frwST.charAt(0) == '-') frwST = frwST.substring(1);
        if (bckST.length() > 1 && bckST.charAt(0) == '-') bckST = bckST.substring(1);
    }

    boolean setTimes(String t1, String t2) {
        frwDR = TRP.String2Time(t1);
        bckDR = TRP.String2Time(t2);
        return frwDR > 0 || bckDR > 0;  // true if station is working
    }
}
