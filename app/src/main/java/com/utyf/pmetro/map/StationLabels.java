package com.utyf.pmetro.map;

import java.util.ArrayList;

/**
 * Created by Utyf on 06.04.2015.
 *
 */

public class StationLabels {

    ArrayList<Item> items;

    class Item {
        String type, label;
        public Item(String t, String l) {
            type = t;
            label = l;
        }
    }

    public void load(Section sec) {
        items = new ArrayList<>();

        for( param prm : sec.params )
            items.add(new Item(prm.name, prm.value));
    }

    String get(String type) {
        if( items==null ) return null;
        for( Item it : items )
            if( it.type.equals(type) )    return it.label;
        return null;
    }

    void clear() {
        items = null;
    }
}
