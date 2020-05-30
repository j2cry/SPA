package ru.bioresourceslab;

import org.intellij.lang.annotations.MagicConstant;

public final class Sample {
    public static final int SAMPLE_CODE = 1;
    public static final int SAMPLE_WEIGHT = 1 << 1;
    public static final int SAMPLE_PACKED = 1 << 2;
    public static final int SAMPLE_STORAGE = 1 << 3;
    public static final int SAMPLE_RACK = 1 << 4;
    public static final int SAMPLE_BOX = 1 << 5;
    public static final int SAMPLE_ROW = 1 << 6;
    public static final int SAMPLE_COLUMN = 1 << 7;
//    public static final int SAMPLE_LOCATION = SAMPLE_STORAGE | SAMPLE_RACK | SAMPLE_BOX | SAMPLE_ROW | SAMPLE_COLUMN;
    public static final int SAMPLE_LOCATION = 0xF8;
    public static final int SAMPLE_ALL = 0xFF;

    public static final String DELIMITER = ".";
    public static final String SPACER = " ";

    private final String code;
    private String weight;
    private boolean packed = false;
    private final String storage, rack, box, row, column;

    public Sample(String code, String weight, String storage, String rack, String box, String row, String column) {
        this.code = code;
        this.weight = weight;
        this.storage = storage;
        this.rack = rack;
        this.box = box;
        this.row = row;
        this.column = column;
    }

    // SETTERS
    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    // GETTERS
    public boolean getPacked() {
        return packed;
    }

    /** Get Sample's fields as string */
    public String get(@MagicConstant(flags = {SAMPLE_CODE, SAMPLE_WEIGHT, SAMPLE_PACKED, SAMPLE_STORAGE,
            SAMPLE_RACK, SAMPLE_BOX, SAMPLE_ROW, SAMPLE_COLUMN, SAMPLE_LOCATION, SAMPLE_ALL}) int flags) {
        if ((flags & SAMPLE_ALL) == 0) return "nothing requested";
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
