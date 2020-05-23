package ru.bioresourceslab;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface Location {
    int LOC_STORAGE = 0x01;
    int LOC_RACK = 0x02;
    int LOC_BOX = 0x04;
    int LOC_ROW = 0x08;
    int LOC_COLUMN = 0x10;
    int LOC_ALL = 0x1F;

    String DELIMITER = Pattern.quote(".");

    /** THIS FUNCTION IS USELESS
     * get location from formatted string using delimiter.
     *      returns: "X" if wrong source format;
     *               "F" if wrong location flag */
    @Deprecated
    static String getLocationFromStr(@NotNull String formattedSource, @MagicConstant(intValues = {LOC_STORAGE, LOC_RACK, LOC_BOX, LOC_ROW, LOC_COLUMN}) int locationFlags) {
        String[] str = formattedSource.split(DELIMITER, 5);
        if (str.length < 5) return "X";
        if ((locationFlags & LOC_ALL) == 0) return "F";

        if ((locationFlags & LOC_STORAGE) != 0) {
            return str[0];
        }

        if ((locationFlags & LOC_RACK) != 0) {
            return str[1];
        }

        if ((locationFlags & LOC_BOX) != 0) {
            return str[2];
        }

        if ((locationFlags & LOC_ROW) != 0) {
            return str[3];
        }

        if ((locationFlags & LOC_COLUMN) != 0) {
            return str[4];
        }
        return "F";
    }

}