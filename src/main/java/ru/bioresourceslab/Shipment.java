package ru.bioresourceslab;

import org.apache.poi.ss.usermodel.*;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.bioresourceslab.Sample.*;

/** Класс, описывающий список упаковки и все возможные манипуляции с ним
 * how to use:
 *      1) think
 *      2) shipment number
 *      3) set identifiers (needed for correct loading from excel)
 *      4) set box options and export parameters
 *      5) load list */

public class Shipment {
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
        this(new Sample("Code", "st0", "st1", "st2", "st3", "st4"));
        identifiers.setWeight("Weight");
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setIdentifiers(Sample identifiers) {
        this.identifiers = identifiers;
    }

    public void setBoxOptions(int rows, int columns, int separator) {
        boxOptions.set(rows, columns, separator);
        map.setColumnCount(columns);
    }

    public void setExportParameters(Font headerFont, Font font, int cellWidth) {
        if (headerFont != null) exportHeaderFont = headerFont;
        if (font != null) exportFont = font;
        if (cellWidth > 0) this.cellWidth = cellWidth;
    }

    public DefaultListModel<Sample> getListModel() {
        return samples;
    }

    public DefaultTableModel getMapModel() {
        return map;
    }

    public int getBoxesCount() {
        return boxOptions.getBoxesCount(samples.getSize());
    }

    // clear list and table
    public void clear() {
        samples.clear();
        map.setRowCount(0);
        map.setColumnCount(boxOptions.getColumnsCount());
        number = "0";
    }

    // add new element to list
    public void addSample(Sample newSample) {
        samples.addElement(newSample);
        convertToMap();
    }

    // remove element from list at [index]
    public void removeSample(int index) {
        if ((index >= samples.getSize()) || (index < 0)) return;
        samples.remove(index);
        convertToMap();
    }

    // move element at [index] to [destination]
    public int moveSample(int index, int destination) {
        if ((destination >= samples.getSize()) || (destination < 0)) return -1;
        if ((index >= samples.getSize()) || (index < 0)) return -2;

        Sample sample = samples.getElementAt(index);
        if (index < destination) {
            samples.add(destination + 1, sample);
            samples.remove(index);
        }
        if (index > destination) {
            samples.add(destination, sample);
            samples.remove(index + 1);
        }
        convertToMap();
        return 0;
    }

    // translates to Table position from index
    public Point translate(int index) {
        return boxOptions.translate(index);
    }

    public void revertSampleStatus(int index) {
        if ((index >= 0) & (index <= samples.getSize() - 1)) {
            samples.get(index).setPacked(!samples.get(index).getPacked());
        }
    }

    // replace all fields in element at [index] with [newSample]
    public void setSample(int index, Sample newSample) {
        samples.setElementAt(newSample, index);
    }

    // rewrite some fields in element at [index] with [newSample]
    public void setSample(int index, @MagicConstant(flags = {SAMPLE_CODE, SAMPLE_WEIGHT, SAMPLE_PACKED, SAMPLE_STORAGE,
            SAMPLE_RACK, SAMPLE_BOX, SAMPLE_ROW, SAMPLE_COLUMN, SAMPLE_ALL}) int flags, Sample newSample) {
        if ((flags & 0xFF) == 0) return;

        if ((flags & SAMPLE_CODE) != 0) {
            samples.getElementAt(index).setCode(newSample.getCode());
        }

        if ((flags & SAMPLE_WEIGHT) != 0) {
            samples.getElementAt(index).setWeight(newSample.getWeight());
        }

        if ((flags & SAMPLE_PACKED) != 0) {
            samples.getElementAt(index).setPacked(newSample.getPacked());
        }

        if ((flags & SAMPLE_STORAGE) != 0) {
            samples.getElementAt(index).setStorage(newSample.getStorage());
        }

        if ((flags & SAMPLE_RACK) != 0) {
            samples.getElementAt(index).setRack(newSample.getRack());
        }

        if ((flags & SAMPLE_BOX) != 0) {
            samples.getElementAt(index).setBox(newSample.getBox());
        }

        if ((flags & SAMPLE_ROW) != 0) {
            samples.getElementAt(index).setRow(newSample.getRow());
        }

        if ((flags & SAMPLE_COLUMN) != 0) {
            samples.getElementAt(index).setColumn(newSample.getColumn());
        }
    }

    // convert current list state to map using [BoxOptions]
    public void convertToMap() {
        map.setRowCount(0);
        map.setRowCount(boxOptions.translate(samples.getSize() - 1).y + 1);
        map.setColumnCount(boxOptions.getColumnsCount());
        for (int index = 0; index < samples.getSize(); index++) {
            Point pos = boxOptions.translate(index);
            if (samples.get(index).getPacked())
                map.setValueAt(samples.get(index).getCode() + " " + samples.get(index).getWeight(), pos.y, pos.x);
            else
                map.setValueAt(samples.get(index).getCode(), pos.y, pos.x);
        }
    }

    // import list from .XLS or .XLSX file
    // TODO: не работает открывание .XLSX
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
            workbook = WorkbookFactory.create(file);
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

            indStorage = value.equals(identifiers.getStorage()) ? (short) index : indStorage;
            indRack = value.equals(identifiers.getRack()) ? (short) index : indRack;
            indBox = value.equals(identifiers.getBox()) ? (short) index : indBox;
            indRow = value.equals(identifiers.getRow()) ? (short) index : indRow;
            indColumn = value.equals(identifiers.getColumn()) ? (short) index : indColumn;
            indCode = value.equals(identifiers.getCode()) ? (short) index : indCode;
            indWeight = value.equals(identifiers.getWeight()) ? (short) index : indWeight;
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
            Sample sample = new Sample(code, storage, rack, box, row, column);
            sample.setWeight(weight);
            sample.setPacked(packed);
            samples.addElement(sample);
        }
        try {
            workbook.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Ошибка импорта: невозможно закрыть файл. ");
        }
        convertToMap();
        log.info("Список успешно загружен. ");
    }

    // auto save current list state to .XLS file
    public void saveListToFile() {
        if (samples.getSize() == 0) return;

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
                    cell.setCellValue(identifiers.getStorage());
                    break;
                }
                case 1: {
                    cell.setCellValue(identifiers.getRack());
                    break;
                }
                case 2: {
                    cell.setCellValue(identifiers.getBox());
                    break;
                }
                case 3: {
                    cell.setCellValue(identifiers.getRow());
                    break;
                }
                case 4: {
                    cell.setCellValue(identifiers.getColumn());
                    break;
                }
                case 5: {
                    cell.setCellValue(identifiers.getCode());
                    break;
                }
                case 6: {
                    cell.setCellValue(identifiers.getWeight());
                    break;
                }
            } // end switch
            cell.setCellStyle(boldCellStyle);
        }
        workbook.getSheetAt(0).setColumnWidth(5,  cellWidth * 256);

        // generating data
        for (int i = 0; i < samples.getSize(); i++) {
            dataRow = workbook.getSheetAt(0).createRow(i + 1);
            for (int column = 0; column < 7; column++) {
                Cell cell = dataRow.createCell(column, CellType.STRING);
                switch (column) {
                    case 0: {
                        cell.setCellValue(samples.get(i).getStorage());
                        break;
                    }
                    case 1: {
                        cell.setCellValue(samples.get(i).getRack());
                        break;
                    }
                    case 2: {
                        cell.setCellValue(samples.get(i).getBox());
                        break;
                    }
                    case 3: {
                        cell.setCellValue(samples.get(i).getRow());
                        break;
                    }
                    case 4: {
                        cell.setCellValue(samples.get(i).getColumn());
                        break;
                    }
                    case 5: {
                        cell.setCellValue(samples.get(i).getCode());
                        break;
                    }
                    case 6: {
                        cell.setCellValue(samples.get(i).getWeight());
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

    // TODO: не работает сохранение .XLSX
    public void saveMapToFile() {
        if (samples.getSize() == 0) {
            log.log(Level.WARNING, "Ошибка экспорта: нет данных в таблице! ");
            return;
        }
        if (number.equals("")) {
            log.log(Level.WARNING, "Ошибка экспорта: не указан номер отправки! ");
            return;
        }

        JFileChooser saveDialog = new JFileChooser();
        saveDialog.setFileFilter(new FileNameExtensionFilter("Excel file", "xls", "xlsx"));
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
            workbook = WorkbookFactory.create(false);
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
                        value = samples.get(index).getCode() + " " + samples.get(index).getWeight();
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

    // Refactors float string value to int string value
    private String getCellString(@NotNull String source) {
        int i = source.indexOf(".");
        if (i > 0) {
            return source.substring(0, source.indexOf("."));
        }
        return source;
    }

}