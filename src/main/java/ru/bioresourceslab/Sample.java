package ru.bioresourceslab;

import org.intellij.lang.annotations.MagicConstant;

import static ru.bioresourceslab.Location.DELIMITER;
import static ru.bioresourceslab.Location.SPACER;

public class Sample {
    public static final int SAMPLE_CODE = 0x01;
    public static final int SAMPLE_WEIGHT = 0x02;
    public static final int SAMPLE_PACKED = 0x04;
    public static final int SAMPLE_STORAGE = 0x08;
    public static final int SAMPLE_RACK = 0x10;
    public static final int SAMPLE_BOX = 0x20;
    public static final int SAMPLE_ROW = 0x40;
    public static final int SAMPLE_COLUMN = 0x80;
    public static final int SAMPLE_LOCATION = 0xF8;
    public static final int SAMPLE_ALL = 0xFF;

    private String code;
    private String weight = "";
    private boolean packed = false;
    private String storage, rack, box, row, column;

    public Sample(String code, String storage, String rack, String box, String row, String column) {
        this.code = code;
        this.storage = storage;
        this.rack = rack;
        this.box = box;
        this.row = row;
        this.column = column;
    }

    // SETTERS
    public void setCode(String code) {
        this.code = code;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    // GETTERS
    public boolean getPacked() {
        return packed;
    }

    /** Get Sample's fields as string */
    public String get(@MagicConstant(flags = {SAMPLE_CODE, SAMPLE_WEIGHT, SAMPLE_PACKED, SAMPLE_STORAGE,
            SAMPLE_RACK, SAMPLE_BOX, SAMPLE_ROW, SAMPLE_COLUMN, SAMPLE_LOCATION, SAMPLE_ALL}) int flags) {
        if ((flags & 0xFF) == 0) return "nothing requested";
        String result = "";
        if ((flags & SAMPLE_CODE) != 0) {
            result += code;
        }
        if ((flags & SAMPLE_WEIGHT) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += SPACER;
            result += weight;
        }
        if ((flags & SAMPLE_PACKED) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += SPACER;
            result += packed;
        }
        if ((flags & SAMPLE_STORAGE) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += SPACER;
            result += storage;
        }
        if ((flags & SAMPLE_RACK) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += DELIMITER;
            result += rack;
        }
        if ((flags & SAMPLE_BOX) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += DELIMITER;
            result += box;
        }
        if ((flags & SAMPLE_ROW) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += DELIMITER;
            result += row;
        }
        if ((flags & SAMPLE_COLUMN) != 0) {
            if (!result.endsWith(SPACER) & !result.equals("")) result += DELIMITER;
            result += column;
        }
        return result;
    }
}
