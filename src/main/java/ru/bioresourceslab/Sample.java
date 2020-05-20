package ru.bioresourceslab;

import static ru.bioresourceslab.Location.DELIMITER;

public class Sample {
    public static final int SAMPLE_CODE = 0x01;
    public static final int SAMPLE_WEIGHT = 0x02;
    public static final int SAMPLE_PACKED = 0x04;
    public static final int SAMPLE_STORAGE = 0x08;
    public static final int SAMPLE_RACK = 0x10;
    public static final int SAMPLE_BOX = 0x20;
    public static final int SAMPLE_ROW = 0x40;
    public static final int SAMPLE_COLUMN = 0x80;
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
    public String getCode() {
        return code;
    }

    public String getWeight() {
        return weight;
    }

    public boolean getPacked() {
        return packed;
    }

    public String getStorage() {
        return storage;
    }

    public String getRack() {
        return rack;
    }

    public String getBox() {
        return box;
    }

    public String getRow() {
        return row;
    }

    public String getColumn() {
        return column;
    }

    /** Get full formatted sample location with delimiter */
    public String getLocation() {
        return storage + DELIMITER + rack + DELIMITER + box + DELIMITER + row + DELIMITER + column;
    }

}
