package com.utyf.pmetro.map;

import com.utyf.pmetro.util.zipMap;

/**
 * Created by Utyf on 25.02.2015.
 *
 */


public class CTY extends Parameters {

    public String Name;
    public String CityName;
    public String Country;
    public String RusName;
    public String NeedVersion;
    public String MapAuthors;

    int Load(){
        String[] strs = zipMap.getFileList(".cty");
        if( strs==null || strs.length!=1 ) return -2;
        if( super.load(strs[0])<0 ) return -1;
        return Parse();
    }

    int Parse() {
        String str;
        Section secOpt = getSec("Options");

        Name = secOpt.getParamValue("Name");
        CityName = secOpt.getParamValue("CityName");
        Country = secOpt.getParamValue("Country");
        RusName = secOpt.getParamValue("RusName");
        NeedVersion = secOpt.getParamValue("NeedVersion");
        RusName = secOpt.getParamValue("RusName");
        str = secOpt.getParamValue("DelayNames");
        if( !str.isEmpty() ) Delay.setNames( str );
        else                 Delay.setNames("Day,Night"); // old maps style

        MapAuthors="";
        for( param prm : secOpt.params )
            if( prm.name.equals("MapAuthors") )
                MapAuthors = MapAuthors.concat(prm.value+"\n");
        MapAuthors = MapAuthors.replaceAll("\\\\n","\n");

        secs = null;
        return 0;
    }
}
