package ru.bioresourceslab;

import org.intellij.lang.annotations.MagicConstant;

import java.awt.*;

/** Интерфейс, описывающий основные параметры коробки и содержащий базовые статические функции преобразования индексов
 *  для обеспечения навигации
 *  !!! коробка не может иметь размер менее 1х1 !!! */

public class BoxOptions {
    public static final int BX_ALL = 0x00;
    public static final int BX_FULL_ONLY = 0x01;

    // нельзя в интерфейсе держать переменные, но делать это классом неудобно - как быть?
    private int rows;       // number of rows in box
    private int columns;    // number of columns in box
    private int separator;  // number of free rows between boxes in the table

    public BoxOptions(int rows, int columns, int separator) {
        setRowsCount(rows);
        setColumnsCount(columns);
        setSeparator(separator);
    }

    public void setRowsCount(int rows) {
        this.rows = Math.max(rows, 1);
    }

    public void setColumnsCount(int columns) {
        this.columns = Math.max(columns, 1);
    }

    public void setSeparator(int separator) {
        this.separator = Math.max(separator, 0);
    }

    public void set(int rows, int columns, int separator) {
        setRowsCount(rows);
        setColumnsCount(columns);
        setSeparator(separator);
    }

    public int getColumnsCount() {
        return columns;
    }

    public int getRowsCount() {
        return rows;
    }

    public int getCapacity() { return columns * rows; }

    public int getSeparator() {
        return separator;
    }

    // calculate number of boxes fulled with such amount of samples or needed for them
    public int getBoxesCount(int samplesCount, @MagicConstant(intValues = {BX_ALL, BX_FULL_ONLY}) int state) {
        // округлить наверх(  [количество всех занятых мест] / [количество мест в одной коробке] )
        double count = (double) samplesCount / getCapacity();
        if ((state & BX_FULL_ONLY) != 0 ) {
            return (int) count;
        }
        return (int)Math.ceil(count);
    }

    // calculate number of boxes physically needed for such amount of samples
    public int getBoxesCount(int samplesCount) {
        return getBoxesCount(samplesCount, BX_ALL);
    }

    // translate index to table position considering separator: X = column, Y = row
    public Point translate(int index) {
        Point result = new Point();
        // add +1 if the first row/col are the headers
        result.y = index / columns + separator * getBoxesCount(index, BX_FULL_ONLY);
        result.x = index % columns;
        return result;
    }

    // translate table position to index considering separator
    public int translate(int row, int col) {
        // checking on-separator click
        while ((row % (rows + separator)) / rows >= 1) {
            row--;
        }

        int fullBoxesCount = row / (rows + separator);
        int fullRows = row - separator * fullBoxesCount;
        return fullRows * columns + col;

//        int ans = (row - separator * (row / (rows + separator))) * columns + col;
//        return (row - separator * (row / (rows + separator))) * columns + col;
    }

}