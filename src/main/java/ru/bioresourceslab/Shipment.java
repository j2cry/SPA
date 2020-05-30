package ru.bioresourceslab;

import org.apache.poi.ss.usermodel.*;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Font;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.bioresourceslab.Sample.*;
import static ru.bioresourceslab.ShipmentEvent.*;


/** Класс, описывающий список упаковки и все возможные манипуляции с ним
 * how to use:
 *      1) think
 *      2) set shipment number
 *      3) set identifiers (needed for correct loading from excel)
 *      4) set box options and export parameters
 *      5) load list
 *      6) add listeners for refreshing UI: check documentation in ShipmentListener! */

public class Shipment extends AbstractShipment {
    private String number;                      // shipment number
    private Sample identifiers;                 // field identifiers (excel column names)
    private final Logger log = Logger.getLogger("SPA Logger");
    private final BoxOptions boxOptions;              // box options container
    private final DefaultListModel<Sample> samples;   // list of samples
    private final DefaultTableModel map;              // table with map

    // export params
    private int cellWidth = 16;
    private Font exportFont = new Font("Courier new", Font.PLAIN, 10);
    private Font exportHeaderFont = new Font("Arial", Font.BOLD, 10);


    // PUT ALL NEW INITIALIZATION HERE
    public Shipment(Sample identifiers) {
        this.identifiers = identifiers;
        boxOptions = new BoxOptions(9, 9, 2);
        samples = new DefaultListModel<>();
        map = new DefaultTableModel(9, 9);
        map.setRowCount(0);
        map.setColumnCount(boxOptions.getColumnsCount());
        number = "N";
    }

    // constructor with default identifiers
    public Shipment() {
        this(new Sample("Code", "Weight", "st0", "st1", "st2", "st3", "st4"));
    }

    /** Set shipment number */
    public void setNumber(String number) {
        this.number = number;
    }

    /** Set in-file names of columns */
    public void setIdentifiers(Sample identifiers) {
        this.identifiers = identifiers;
    }

    /** Set box parameters */
    public void setBoxOptions(int rows, int columns, int separator) {
        boxOptions.set(rows, columns, separator);
        map.setColumnCount(columns);
    }

    /** Set export fonts and cell width */
    public void setExportParameters(Font headerFont, Font font, int cellWidth) {
        if (headerFont != null) exportHeaderFont = headerFont;
        if (font != null) exportFont = font;
        if (cellWidth > 0) this.cellWidth = cellWidth;
    }

    /** Get model of samples list */
    public ListModel<Sample> getListModel() {
        return samples;
    }

    /** Get model of samples map */
    public TableModel getMapModel() {
        return map;
    }

    /** Get count of boxes in this shipment */
    public int getBoxesCount() {
        return boxOptions.getBoxesCount(samples.size());
    }

    /** Clear list and table; set shipment number to '0' */
    public void clear() {
        synchronized (samples) {
            samples.clear();
        }
        map.setRowCount(0);
        map.setColumnCount(boxOptions.getColumnsCount());
        number = "0";
        fireEvent(this, EVENT_SAMPLE_REMOVED, -1);
    }

    /** Insert array of samples {@param newSamples} at {@param index} */
    public void addSamples(@NotNull ArrayList<Sample> newSamples, int index) {
        for (Sample sample : newSamples) {
            synchronized (samples) {
                samples.add(index, sample);
            }
        }
        convertToMap();
        fireEvent(this, EVENT_SAMPLE_ADDED, index);
    }

    /** Remove sample from list at {@param index} */
    public void removeSample(int index) {
//      also can use if JavaSource 1.9+
//        Objects.checkIndex(index, samples.getSize());
        if ((index >= samples.size()) || (index < 0)) return;
        synchronized (samples) {
            samples.remove(index);
        }
        convertToMap();
        fireEvent(this, EVENT_SAMPLE_REMOVED, index);
    }

    /** Move element from {@param index} to {@param destination} */
    public void moveSample(int index, int destination) {
        if ((index >= samples.size()) || (index < 0)) return;
        if (destination >= samples.size())
            destination = samples.size() - 1;
        if (destination < 0)
            destination = 0;

        Sample sample = samples.get(index);
        Sample backup = samples.get(destination);
        synchronized (samples) {
            samples.set(destination, sample);
            samples.set(index, backup);
        }

        map.setValueAt(sample, translate(destination).y, translate(destination).x);
        map.setValueAt(backup, translate(index).y, translate(index).x);
        fireEvent(this, EVENT_SAMPLE_MOVED, destination);
    }

    /** Get sample by index.
     * Returns 'null' if index is out of range or sample was set to 'null' */
    @Nullable
    public Sample getSample(int index) {
        if ((index >= samples.size()) || (index < 0)) return null;
        return samples.get(index);
    }

    /** Return packed status of sample by index.
     * If index is out of range, returns FALSE. */
    public boolean sampleIsNotPacked(int index) {
        if ((index >= samples.size()) || (index < 0)) return true;
        if (samples.get(index) == null) return true;
        return !samples.get(index).getPacked();
    }

    /** Replace element at {@param index} with {@param newSample} */
    public void setSample(int index, Sample newSample) {
        if ((index >= samples.size()) || (index < 0)) return;
        synchronized (samples) {
            samples.set(index, newSample);
        }
        fireEvent(this, EVENT_SAMPLE_CHANGED, index);
    }

    /** Flag for {@code getNextIndex(...)} to default finding: down the list, list as loop, bypass packed */
    public static final int NEXT_DEFAULT = 0;

    /** Flag for {@code getNextIndex(...)} to find next sample in reverse */
    public static final int NEXT_REVERSED = 1;

    /** Flag for {@code getNextIndex(...)} to continue searching from the beginning of the list when the end is reached */
    public static final int NEXT_STOP_WHEN_END = 1 << 1;

    /** Flag for {@code getNextIndex(...)} to skip samples checked as packed */
    public static final int NEXT_EVERY_ITEM = 1 << 2;

    /** Return the next sample index in the list starting with {@param index} and corresponding to {@param flags}
     * If there is no samples that meet the requirements, returns '-1' */
    public int getNextIndex(int index, @MagicConstant(flags = {NEXT_DEFAULT, NEXT_REVERSED, NEXT_STOP_WHEN_END, NEXT_EVERY_ITEM}) int flags) {
        int count = samples.size();
        for (int i = 0; i < count; i++) {
            // refresh index
            index = ((flags & NEXT_REVERSED) != 0) ? --index : ++index;
            // check if index out of range and loop selection flag is set
            if (((flags & NEXT_STOP_WHEN_END) != 0) && ((index >= count) || (index < 0))) return -1;  // reached end of list
            if (index < 0)
                index = count - 1;
            if (index >= count)
                index = 0;

            if (((flags & NEXT_EVERY_ITEM) != 0) || sampleIsNotPacked(index)) {
                return index;
            }
        }
        // if all samples are packed
        return -1;
    }

    /** Return index of last sample with the same sample mask */
    // TODO: in developing
    public int getLastIndex(int index) {
        Sample sample = samples.get(index);
        // get sample mask


        Sample lastSample;
        int last;
        do {
            last = getNextIndex(++index, NEXT_EVERY_ITEM | NEXT_STOP_WHEN_END);
            if (last < 0) return -1;        // end of list reached
            lastSample = samples.get(last);

            // analyze codes

        } while (sample.get(SAMPLE_CODE).equals(lastSample.get(SAMPLE_CODE)));

        return last;
    }

    /** Get position of sample with {@param index} in the table according to box options.
     * Returns: Point.y = row;
     *          Point.x = column */
    public Point translate(int index) {
        if ((index >= samples.size()) || (index < 0)) return new Point(-1, -1);
        return boxOptions.translate(index);
    }

    /** Get index of sample with table position {@param row}, {@param column} according to box options.
     * Returns: '-1': if row is out of range
                '-2': if column is out of range */
    public int translate(int row, int column) {
        if ((row >= map.getRowCount()) || (row < 0)) return -1;
        if ((column >= map.getColumnCount()) || (column < 0)) return -2;
        return boxOptions.translate(row, column);
    }

    /** Reverse sample packed status */
    public void revertSampleStatus(int index) {
        if ((index >= samples.size()) || (index < 0)) return;
        synchronized (samples) {
            samples.get(index).setPacked(!samples.get(index).getPacked());
        }
        fireEvent(this, EVENT_SAMPLE_CHANGED, index);
    }

    /** Get number of samples in this shipment */
    public int getSamplesCount() {
        return samples.size();
    }

    /** Load samples list from .XLS or .XLSX file */
    public void importList() {
        JFileChooser openDialog = new JFileChooser();
        openDialog.setFileFilter(new FileNameExtensionFilter("Excel files", "xls", "xlsx"));
        openDialog.setAcceptAllFileFilterUsed(false);
        openDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File desktopDir = new File((System.getProperty("user.home") + "/Desktop"));
        openDialog.setCurrentDirectory(desktopDir);
        File file;

        if (openDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = openDialog.getSelectedFile();
        } else return;

        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(file, null, true);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Ошибка импорта: не удалось открыть файл. ");
            return;
        }

        int firstRowIndex = workbook.getSheetAt(0).getFirstRowNum();
        Row fileRow = workbook.getSheetAt(0).getRow(firstRowIndex);
        short firstColIndex = fileRow.getFirstCellNum();
        short lastColIndex = fileRow.getLastCellNum();
        // indexes of columns corresponding to the sample data
        short indStorage = -1;
        short indRack = -1;
        short indBox = -1;
        short indRow = -1;
        short indColumn = -1;
        short indCode = -1;
        short indWeight = -1;

        // finding columns with needed data
        for (int index = firstColIndex; index < lastColIndex; index++) {
            String value = fileRow.getCell(index).toString();

            indStorage = value.equals(identifiers.get(SAMPLE_STORAGE)) ? (short) index : indStorage;
            indRack = value.equals(identifiers.get(SAMPLE_RACK)) ? (short) index : indRack;
            indBox = value.equals(identifiers.get(SAMPLE_BOX)) ? (short) index : indBox;
            indRow = value.equals(identifiers.get(SAMPLE_ROW)) ? (short) index : indRow;
            indColumn = value.equals(identifiers.get(SAMPLE_COLUMN)) ? (short) index : indColumn;
            indCode = value.equals(identifiers.get(SAMPLE_CODE)) ? (short) index : indCode;
            indWeight = value.equals(identifiers.get(SAMPLE_WEIGHT)) ? (short) index : indWeight;
        } // end for
        // check if not all columns found
        if ((indStorage == -1) || (indRack == -1) || (indBox == -1) || (indRow == -1) || (indColumn == -1) || (indCode == -1)) {
            log.log(Level.WARNING, "Ошибка импорта: не все столбцы данных найдены! ");
            return;
        }
        // clear list
        clear();
        // reading data
        int lastRowIndex = workbook.getSheetAt(0).getLastRowNum();
        if (firstRowIndex == lastRowIndex) {
            log.log(Level.WARNING, "Ошибка импорта: файл не содержит данных! ");
            return;
        }
        for (int index = firstRowIndex + 1; index <= lastRowIndex; index++) {
            fileRow = workbook.getSheetAt(0).getRow(index);

            String storage = getCellString(fileRow.getCell(indStorage).toString());
            String rack = getCellString(fileRow.getCell(indRack).toString());
            String box = getCellString(fileRow.getCell(indBox).toString());
            String row = getCellString(fileRow.getCell(indRow).toString());
            String column = getCellString(fileRow.getCell(indColumn).toString());
            String code = fileRow.getCell(indCode).toString();
            String weight = (indWeight != -1) ? fileRow.getCell(indWeight).toString() : "";
            boolean packed = !weight.equals("");
            Sample sample = new Sample(code, weight, storage, rack, box, row, column);
            sample.setPacked(packed);
            samples.addElement(sample);
        }

        try {
            workbook.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка импорта: невозможно закрыть файл. ");
        }
        convertToMap();
        fireEvent(this, EVENT_SAMPLE_ADDED, samples.size() - 1);
        log.info("Список успешно загружен. ");
    }

    /** Save samples list to .XLS file */
    public void saveListToFile() {
        if (samples.size() == 0) return;

        File file = new File("list_autosave.xls");
        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(false);
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка: не удалось создать файл автосохранения. ");
            return;
        }
        workbook.createSheet("shipment list " + number);

        // style for headers
        CellStyle boldCellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        boldCellStyle.setFont(headerFont);
        boldCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        boldCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        boldCellStyle.setBorderRight(BorderStyle.THIN);
        boldCellStyle.setBorderLeft(BorderStyle.THIN);
        boldCellStyle.setBorderTop(BorderStyle.THIN);
        boldCellStyle.setBorderBottom(BorderStyle.THIN);
        boldCellStyle.setAlignment(HorizontalAlignment.CENTER);
        boldCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row dataRow = workbook.getSheetAt(0).createRow(0);

        // set sheet width
        int widRate = 4;
        // generating headers
        for (int column = 0; column < 7; column++) {
            workbook.getSheetAt(0).setColumnWidth(column, widRate * 256);

            Cell cell = dataRow.createCell(column, CellType.STRING);
            switch (column) {
                case 0: {
                    cell.setCellValue(identifiers.get(SAMPLE_STORAGE));
                    break;
                }
                case 1: {
                    cell.setCellValue(identifiers.get(SAMPLE_RACK));
                    break;
                }
                case 2: {
                    cell.setCellValue(identifiers.get(SAMPLE_BOX));
                    break;
                }
                case 3: {
                    cell.setCellValue(identifiers.get(SAMPLE_ROW));
                    break;
                }
                case 4: {
                    cell.setCellValue(identifiers.get(SAMPLE_COLUMN));
                    break;
                }
                case 5: {
                    cell.setCellValue(identifiers.get(SAMPLE_CODE));
                    break;
                }
                case 6: {
                    cell.setCellValue(identifiers.get(SAMPLE_WEIGHT));
                    break;
                }
            } // end switch
            cell.setCellStyle(boldCellStyle);
        }
        workbook.getSheetAt(0).setColumnWidth(5,  cellWidth * 256);

        // generating data
        for (int i = 0; i < samples.size(); i++) {
            dataRow = workbook.getSheetAt(0).createRow(i + 1);
            for (int column = 0; column < 7; column++) {
                Cell cell = dataRow.createCell(column, CellType.STRING);
                switch (column) {
                    case 0: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_STORAGE));
                        break;
                    }
                    case 1: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_RACK));
                        break;
                    }
                    case 2: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_BOX));
                        break;
                    }
                    case 3: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_ROW));
                        break;
                    }
                    case 4: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_COLUMN));
                        break;
                    }
                    case 5: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_CODE));
                        break;
                    }
                    case 6: {
                        cell.setCellValue(samples.get(i).get(SAMPLE_WEIGHT));
                        break;
                    }
//                cell.setCellStyle(cellStyle);
                } // end switch
            }
        }

        // writing excel file
        try {
            FileOutputStream outStream = new FileOutputStream(file);
            workbook.write(outStream);
            outStream.close();
            workbook.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка: не удалось записать файл автосохранения! ");
        }
    }

    /** Save shipment map to .XLS file */
    public void saveMapToFile() {
        if (samples.size() == 0) {
            log.log(Level.WARNING, "Ошибка экспорта: нет данных в таблице! ");
            return;
        }
        if (number.equals("")) {
            log.log(Level.WARNING, "Ошибка экспорта: не указан номер отправки! ");
            return;
        }

        JFileChooser saveDialog = new JFileChooser();
        saveDialog.addChoosableFileFilter(new FileNameExtensionFilter("Excel file 97-2003", "xls"));
        saveDialog.addChoosableFileFilter(new FileNameExtensionFilter("Excel file", "xlsx"));

        saveDialog.setAcceptAllFileFilterUsed(false);
        saveDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File desktopDir = new File((System.getProperty("user.home") + "/Desktop"));
        saveDialog.setCurrentDirectory(desktopDir);
        File file;

        if (saveDialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = saveDialog.getSelectedFile();
            if (!file.getName().endsWith(".xls") && !file.getName().endsWith(".xlsx"))
                file = new File(file.getAbsolutePath() + ".xls");
        } else return;

        Workbook workbook;
        try {
            // set true if .XLSX
            workbook = WorkbookFactory.create(file.getName().endsWith(".xlsx"));
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка экспорта: не удалось создать файл. ");
            return;
        }
        workbook.createSheet("Shipment " + number);
        // set sheet width
        for (int column = 0; column < boxOptions.getColumnsCount(); column++) {
            workbook.getSheetAt(0).setColumnWidth(column + 1, cellWidth * 256);
        }

        // style for cells
        CellStyle cellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font cellFont = workbook.createFont();
        cellFont.setFontName(exportFont.getName());
        cellFont.setFontHeightInPoints((short) exportFont.getSize());
        cellFont.setBold((Font.BOLD & exportFont.getStyle()) == Font.BOLD);
        cellFont.setItalic((Font.ITALIC & exportFont.getStyle()) == Font.ITALIC);
        cellStyle.setFont(cellFont);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);

        // style for headers
        CellStyle boldCellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setFontName(exportHeaderFont.getName());
        headerFont.setFontHeightInPoints((short) exportHeaderFont.getSize());
        headerFont.setBold((Font.BOLD & exportHeaderFont.getStyle()) == Font.BOLD);
        headerFont.setItalic((Font.ITALIC & exportHeaderFont.getStyle()) == Font.ITALIC);
        boldCellStyle.setFont(headerFont);
        boldCellStyle.setBorderRight(BorderStyle.THIN);
        boldCellStyle.setBorderLeft(BorderStyle.THIN);
        boldCellStyle.setBorderTop(BorderStyle.THIN);
        boldCellStyle.setBorderBottom(BorderStyle.THIN);
        boldCellStyle.setAlignment(HorizontalAlignment.CENTER);
        boldCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // generating excel file
        int blockCount = this.getBoxesCount();
        int rowsInBlock = 2 + boxOptions.getRowsCount() + boxOptions.getSeparator();

        for (int block = 0; block < blockCount; block++) {
            // create first row in block - with box number
            Row dataRow = workbook.getSheetAt(0).createRow(rowsInBlock * block);
            Cell cell = dataRow.createCell(1, CellType.STRING);
            cell.setCellValue(number + "." + (block + 1));
            cell.setCellStyle(boldCellStyle);

            // second row in block - box header
            dataRow = workbook.getSheetAt(0).createRow(rowsInBlock * block + 1);
            for (int column = 1; column < boxOptions.getColumnsCount() + 1; column++) {
                cell = dataRow.createCell(column, CellType.STRING);
                cell.setCellValue(String.valueOf((char) ('a' + column - 1)));
                cell.setCellStyle(boldCellStyle);
            }

            // export the list data
            for (int row = 0; row < boxOptions.getRowsCount(); row++) {
                int fileRow = 2 + rowsInBlock * block + row;
                dataRow = workbook.getSheetAt(0).createRow(fileRow);
                // make the first cell as header
                Cell firstCell = dataRow.createCell(0, CellType.STRING);
                firstCell.setCellValue(String.valueOf(1 + row));
                firstCell.setCellStyle(boldCellStyle);
                // export data
                for (int column = 1; column < boxOptions.getColumnsCount() + 1; column++) {
                    cell = dataRow.createCell(column, CellType.STRING);
                    String value;
                    int index = (column - 1) + boxOptions.getColumnsCount() * row + boxOptions.getCapacity() * block;
                    try {
                        value = samples.get(index).get(SAMPLE_CODE | SAMPLE_WEIGHT);
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                        value = "";
                    }
                    cell.setCellValue(value);
                    cell.setCellStyle(cellStyle);
                } // for column
            } // for row
        } // for block

        // writing excel file
        try {
            FileOutputStream outStream = new FileOutputStream(file);
            workbook.write(outStream);
            outStream.close();
            workbook.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка экспорта: невозможно записать файл! ");
        }

        log.info("Экспорт успешно завершен. ");
    }

    /** Convert current list to table according to box options. */
    protected void convertToMap() {
        map.setRowCount(0);
        map.setRowCount(this.translate(samples.size() - 1).y + 1);
        map.setColumnCount(boxOptions.getColumnsCount());
        for (int index = 0; index < samples.size(); index++) {
            Point pos = this.translate(index);
            map.setValueAt(samples.get(index), pos.y, pos.x);
//            if (samples.get(index).getPacked())
//                map.setValueAt(samples.get(index).get(SAMPLE_CODE | SAMPLE_WEIGHT), pos.y, pos.x);
//            else
//                map.setValueAt(samples.get(index).get(SAMPLE_CODE), pos.y, pos.x);
        }
    }

    // Refactors float string value to int string value
    protected String getCellString(@NotNull String source) {
        int i = source.indexOf(".");
        if (i > 0) {
            return source.substring(0, source.indexOf("."));
        }
        return source;
    }

}