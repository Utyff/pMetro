package com.utyf.pmetro.map;

import java.util.ArrayList;

public class Section {

    public String name;
    public ArrayList<param> params = new ArrayList<>();

    Section(String nm) {
        name = nm;
    }

    void AddParameter(String name, String value) {
        params.add( new param(name, value) );
    }

    public int ParamsNum() {
        return params.size();
    }

    public param getParam(int i)  {
        return params.get(i);
    }

    public param getParam(String name)  {
        for( param prm : params )
            if( name.toLowerCase().equals(prm.name.toLowerCase()) )
                return prm;

        return null;
    }

    public String getParamValue(String name)  {
        param pp;
        if( (pp=getParam(name)) != null )
            return pp.value;
        return "";
    }
}

