package com.utyf.pmetro.map;

import com.utyf.pmetro.util.zipMap;

/**
 * Loads and parses information about city from .cty file
 *
 * @author Utyf
 */


public class CTY {

    public String Name;
    public String CityName;
    public String Country;
    public String RusName;
    public String NeedVersion;
    public String MapAuthors;

    /**
     * Loads and parses information about city from .cty file
     *
     * @return 0 if successfully loaded, -2 if more than one .cty file was found, -1 if the file is invalid
     */
     public int Load(){
        // TODO: 09.06.2016 pass zipMap as parameter
        String[] strs = zipMap.getFileList(".cty");
        if( strs==null || strs.length!=1 ) return -2;
        Parameters parser = new Parameters();
        if( parser.load(strs[0])<0 ) return -1;
        return Parse(parser);
    }

    private int Parse(Parameters parser) {
        String str;
        Section secOpt = parser.getSec("Options");

        Name = secOpt.getParamValue("Name");
        CityName = secOpt.getParamValue("CityName");
        Country = secOpt.getParamValue("Country");
        RusName = secOpt.getParamValue("RusName");
        NeedVersion = secOpt.getParamValue("NeedVersion");
        RusName = secOpt.getParamValue("RusName");
        str = secOpt.getParamValue("DelayNames");
        // TODO: 09.06.2016 Avoid setting global value for delay
        if( !str.isEmpty() ) Delay.setNames( str );
        else                 Delay.setNames("Day,Night"); // old maps style

        MapAuthors="";
        for( param prm : secOpt.params )
            if( prm.name.equals("MapAuthors") )
                MapAuthors = MapAuthors.concat(prm.value+"\n");
        MapAuthors = MapAuthors.replaceAll("\\\\n","\n");

        return 0;
    }
}
