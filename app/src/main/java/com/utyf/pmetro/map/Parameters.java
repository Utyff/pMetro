package com.utyf.pmetro.map;

import android.util.Log;

import com.utyf.pmetro.util.zipMap;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Loads from file and stores parameters file. The file consists of several sections, which contain
 * parameters. Each parameter has name and value.
 */
public class Parameters {
    private int nameSeparator = '=';  // default parameters name separator symbol "="
    private Section currentSection;
    private ArrayList<Section> secs; // = new ArrayList<>();

    //public Parameters(String _zipFile) {
    //    zipFile = _zipFile;
    //}

    public int load(String filename) {
        byte[] bb;     // buffer for file read
        String str;    // file content

        bb = zipMap.getFile(filename);
        //bb = loadFile(filename);
        if (bb == null) return -3;
        try {
            str = new String(bb, 0, bb.length, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            Log.e("Parameters /40", "Wrong file encoding - " + filename);
            e.printStackTrace();
            return -2;
        }

        secs = new ArrayList<>();
        parseParameters(str);  // Parse file

        return 0;
    }

    private void parseParameters(String str) {
        int i, i2;
        String line;

        currentSection = null;
        str = str.replace("\r\n", "\n");  // remove symbol \r
        str = str.replace("\r", "\n");  // remove symbol \r

        i = 0;
        while (i < str.length()) {
            i2 = str.indexOf('\n', i);                       // get one line (till symbol \n)
            if (i2 == -1) i2 = str.length();               // if not found - to the end of file
            line = str.substring(i, i2).trim();

            if (!line.isEmpty() && line.charAt(0) != ';')  // check for empty string or commented
                if (line.charAt(0) == '[')                 // is it new section ?
                    addSection(line.substring(1, line.length() - 1));
                else addParameter(line);

            i = i2 + 1; // set pointer to next line
        }
        currentSection = null;
    } // parseParameters()


    private void addSection(String sectionName) {
        currentSection = new Section(sectionName);
        secs.add(currentSection);
    }


    private void addParameter(String Line) {
        int i;

        if (currentSection == null) addSection("");
        i = Line.indexOf(nameSeparator);  // looking for name separator
        if (i == -1)                     // is there parameter name?
            currentSection.AddParameter("", Line);
        else
            currentSection.AddParameter(Line.substring(0, i), Line.substring(i + 1));
    }

    int secsNum() {
        return secs.size();
    }

    /**
     * Get section by its index. Sections are ordered by their occurrence in the file
     *
     * @param index Index of the section
     * @return Section
     */
    Section getSec(int index) {
        return secs.get(index);
    }

    /**
     * Get section by its name. Comparison of names is case insensitive
     *
     * @param name Name of the section
     * @return Section with given name
     */
    public Section getSec(String name) {
        if (secs == null) return null;
        for (Section s : secs)
            if (name.equalsIgnoreCase(s.name))
                return s;

        return null;
    }

    /**
     * Set custom separator symbol between name and value
     *
     * @param nameSeparator Separator symbol
     */
    public void setNameSeparator(char nameSeparator) {
        this.nameSeparator = nameSeparator;
    }
}
