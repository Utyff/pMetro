package com.utyf.pmetro.map;

import android.util.Log;

import java.util.ArrayList;

/**
 * Loads and parses information about transports from *.trp files
 *
 * @author Utyf
 */
public class TRP {
    public final Transfer[] transfers;
    public final TRP_line[] lines;
    public final String type;

    private final String name;

    public String getType() {
        return type;
    }

    static float String2Time(String t) {
        int strLength = t.length();
        boolean isDecimal = false;
        boolean hasInteger = false;
        boolean hasDecimal = false;
        boolean isInvalid = false;
        int integer = 0;
        int decimal = 0;
        for (int i = 0; i < strLength; ++i) {
            char c = t.charAt(i);
            if (c >= '0' && c <= '9') {
                int digit = c - '0';
                if (isDecimal) {
                    decimal = decimal * 10 + digit;
                    hasDecimal = true;
                }
                else {
                    integer = integer * 10 + digit;
                    hasInteger = true;
                }
            }
            else if (c == '.') {
                isDecimal = true;
            }
            else if (c == ' ') {
                // skip
            }
            else {
                isInvalid = true;
            }
        }
        if (!hasInteger && !hasDecimal && !isDecimal)
            return -1;
        if (isInvalid || !hasInteger || isDecimal && !hasDecimal) {
            Log.e("TRP /354", "TRP Driving fork wrong time - <" + t + "> ");
            return -1;
        }
        return integer + decimal / 60f;
    }

    public String getName() {
        return name;
    }

    private TRP(String name, Transfer[] transfers, TRP_line[] lines, String type) {
        this.name = name;
        this.transfers = transfers;
        this.lines = lines;
        this.type = type;
    }

    public static TRP load(String name) {
        Parameters parser = new Parameters();

        if (parser.load(name) < 0)
            return null;
        // parsing TRP file
        int i;
        TRP_line ll;

        String type;
        if (parser.getSec("Options") != null)
            type = parser.getSec("Options").getParamValue("Type");
        else
            type = name.substring(0, name.lastIndexOf("."));

        TRP_line[] lines;
        ArrayList<TRP_line> la = new ArrayList<>();
        for (i = 0; i < parser.secsNum(); i++) {
            if (parser.getSec(i).name.equals("Options")) continue;
            if (!parser.getSec(i).name.startsWith("Line")) break;
            ll = TRP_line.Load(parser.getSec(i));
            if (ll != null)
                la.add(ll);
        }
        lines = la.toArray(new TRP_line[la.size()]);

        Transfer[] transfers;
        Section sec = parser.getSec("Transfers"); // load transfers
        ArrayList<Transfer> ta = new ArrayList<>();
        if (sec != null)
            for (i = 0; i < sec.ParamsNum(); i++)
                ta.add(new Transfer(sec.getParam(i).value));  // sec.getParam(i).name,
        transfers = ta.toArray(new Transfer[ta.size()]);

        //  = getSec("AdditionalInfo");  todo
        return new TRP(name, transfers, lines, type);
    }

    public TRP_line getLine(int ln) {
        if (lines == null || lines.length < ln || ln < 0) return null;
        return lines[ln];
    }
}
