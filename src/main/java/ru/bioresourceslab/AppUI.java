package ru.bioresourceslab;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.bioresourceslab.Sample.SAMPLE_CODE;
import static ru.bioresourceslab.Shipment.*;

public class AppUI extends JFrame {
    private JPanel mainPanel;
    private JList<Sample> samplesList;
    private JButton sampleAddButton;
    private JButton sampleDeleteButton;
    private JButton sampleEditButton;
    private JButton sampleMoveUpButton;
    private JButton sampleMoveDownButton;
    private JButton loadListButton;
    private JButton saveMapButton;
    private JButton debugButton;
    private JButton startButton;
    private JComboBox<String> statusBox;
    private JLabel currentSampleLabel;
    private JLabel fromPosLabel;
    private JLabel toPosLabel;
    private JLabel boxesCountLabel;
    private JLabel currentPosLabel;
    private JTextField shipmentNumberField;
    private MapTable mapTable;

    public static final int UI_SAMPLE_INFO = 0x01;
    public static final int UI_BOX_COUNTER = 0x02;
    public static final int UI_SELECTION = 0x04;
    public static final int UI_ALL = 0x07;

    private Shipment shipment;
    private RecognizerThread recThread;

    // debug variables
    final Logger log = Logger.getLogger("SPA Logger");
    boolean inDeveloping = false;

    public AppUI() {
        super();

        // frame settings
        this.setContentPane(mainPanel);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(1014, 360));
        pack();
        this.setLocationRelativeTo(null);

        // configure logger
        ComponentLogHandler logHandler = new ComponentLogHandler();
        logHandler.setComponent(statusBox);
        log.addHandler(logHandler);
        log.setLevel(Level.FINE);

        // adding autosave list on exit
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (recThread != null) recThread.close();
                if (shipment != null) shipment.saveListToFile();
            }
        });

        // initialize status bar
        statusBox.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                statusBox.setSelectedIndex(statusBox.getItemCount() - 1);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
            }
        });

        // loading icon
        Image titleIcon = Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("title.png"));
        setIconImage(titleIcon);
        //set title and debug status
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "IN DEVELOPING";
            inDeveloping = true;
        }
        setTitle("Shipment packing assistant (" + version + ") by Mirzoian Peter");

        // setting off debug features
        if (!inDeveloping) {
            debugButton.setVisible(false);
        }

// initializing recognizer
        log.fine("Инициализация распознавателя...");
        try {
            recThread = new RecognizerThread() {
                @Override
                protected void uiRefresh() {
                    startButton.setText(this.pauseFlag ? "Начать работу" : "Остановить работу");
                }

                @Override
                protected void analyze(String command) {
                    int index = samplesList.getSelectedIndex();
                    Sample sample = shipment.getSample(index);
                    if (sample == null) return;

                    // if weight was recognized, setWeight and go next sample
                    if (this.interpretWeight(command)) {
                        sample.setWeight(result);
                        // output result in status-bar
                        log.fine(sample.get(SAMPLE_CODE | Sample.SAMPLE_WEIGHT));

                        if (!sample.getPacked())
                            shipment.revertSampleStatus(index);
                        this.pauseFlag = !itemSelect(true, NEXT_DEFAULT);
                        if (this.pauseFlag)
                            log.fine("Работа закончена: список обработан.");
                    } else {
                        // analyze commands
                        switch (result) {
                            case COMMAND_END: {
                                this.pause();
                                log.info("Команда: завершить работу");
                                break;
                            }
                            case COMMAND_NEXT: {
                                itemSelect(true, NEXT_DEFAULT);
                                log.info("Команда: дальше");
                                break;
                            }
                            case COMMAND_PREVIOUS: {
                                itemSelect(true, NEXT_REVERSED | NEXT_EVERY_ITEM);
                                log.info("Команда: назад");
                                break;
                            }
                            default: {
                                if (inDeveloping) {
                                    log.config(result);
                                } else {
                                    log.config("Не распознано. Повторите.");
                                }
                            }
                        } // end switch
                    } // end if/else
                } // end analyze()
            };
        } catch (Exception ex) {
            log.severe("Ошибка распознавания: распознаватель не инициализирован!");
            return;
        }
        recThread.start();
        log.fine("Распознаватель инициализирован.");

// initialize Shipment object
        shipment = new Shipment();

// loading settings (fonts, box parameters, etc)
        log.fine("Загрузка параметров...");
        String settingsFileName = "spa.properties";
        final Properties properties = new Properties();
        InputStream projectProperties;
        // initialize settings source
        try {
            String currentPath = Paths.get("").toAbsolutePath().toString();
            File settingsFile = new File(currentPath + "\\" + settingsFileName);
            projectProperties = new FileInputStream(settingsFile);
        } catch (FileNotFoundException e) {
            log.config("Ошибка загрузки параметров: файл не найден. Загружены параметры умолчанию.");
            projectProperties = this.getClass().getClassLoader().getResourceAsStream(settingsFileName);
        }
        try {
            assert projectProperties != null;
            properties.load(new InputStreamReader(projectProperties, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.severe("Критическая ошибка загрузки параметров. Обратитесь к разработчику. ");
            return;
        }

        // loading parameters for UI
        Font tableHeaderFont = parseFont(properties, "table.header.font.name", "table.header.font.style", "table.header.font.size");
        Font tableFont = parseFont(properties, "table.font.name", "table.font.style", "table.font.size");
        Font infoLabelFont = parseFont(properties, "info.font.name", "info.font.style", "info.font.size");
        Font posFont = parseFont(properties, "pos.font.name", "pos.font.style", "pos.font.size");
        String packedColor = properties.getProperty("table.color.packed");
        String bgColor = properties.getProperty("table.color.notpacked");
        String bgSelection = properties.getProperty("table.color.selection");
        currentSampleLabel.setFont(infoLabelFont);
        fromPosLabel.setFont(posFont);
        toPosLabel.setFont(posFont);
        samplesList.setFont(tableFont);
        mapTable.setFont(tableFont);
        mapTable.getTableHeader().setFont(tableHeaderFont);
        mapTable.setPackedBackground(parseColorDef(packedColor, Color.green));
        mapTable.setNotPackedBackground(parseColorDef(bgColor, Color.red));
        mapTable.setSelectionBackground(parseColorDef(bgSelection, Color.blue));

        // loading fonts and settings for exporting
        Font exportHeaderFont = parseFont(properties, "export.header.font.name", "export.header.font.style", "export.header.font.size");
        Font exportFont = parseFont(properties, "export.font.name", "export.font.style", "export.font.size");
        int cellWidth = parseNumDef(properties.getProperty("export.width"), 16);
        shipment.setExportParameters(exportHeaderFont, exportFont, cellWidth);

        // loading box parameters
        int separator = parseNumDef(properties.getProperty("separator"), 2);
        int columns = parseNumDef(properties.getProperty("box.columns"), 9);
        int rows = parseNumDef(properties.getProperty("box.rows"), 9);
        shipment.setBoxOptions(rows, columns, separator);

        // loading identifiers
        String fCode = properties.getProperty("list.code");
        String fWeight = properties.getProperty("list.weight");
        String fStorage = properties.getProperty("list.storage");
        String fRack = properties.getProperty("list.rack");
        String fBox = properties.getProperty("list.box");
        String fRow = properties.getProperty("list.row");
        String fColumn = properties.getProperty("list.column");
        Sample fIDs = new Sample(fCode, fWeight, fStorage, fRack, fBox, fRow, fColumn);
        shipment.setIdentifiers(fIDs);

        // loading weight range
        double lower = parseNumDef(properties.getProperty("range.lower"), 0.0);
        double upper = parseNumDef(properties.getProperty("range.upper"), 1.5);
        recThread.setRanges(lower, upper);

        // loading parameters
        boolean autoSelect = properties.getProperty("autoselect").equals("true");

        // SHIPMENT LISTENER TO REFRESH UI
        shipment.addListener(new ShipmentListener() {
            @Override
            public void dataAdded(ShipmentEvent source) {
                refreshUI(UI_ALL);
            }

            @Override
            public void dataRemoved(ShipmentEvent source) {
                refreshUI(UI_ALL, source.getTarget() - 1);
            }

            @Override
            public void dataChanged(ShipmentEvent source) {
                refreshUI(UI_SAMPLE_INFO);
            }

            @Override
            public void dataMoved(ShipmentEvent source) {
                refreshUI(UI_SELECTION, source.getTarget());
            }
        });

// initializing models
        mapTable.setModel(shipment.getMapModel());
        samplesList.setModel(shipment.getListModel());

// initializing renderers&listeners on Table&List
        {
            // mapTable click listener
            mapTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
//                    super.mouseClicked(e);
                    int index = shipment.translate(mapTable.rowAtPoint(e.getPoint()), mapTable.columnAtPoint(e.getPoint()));
                    if ((index >= shipment.getSamplesCount()) || (index < 0))
                        refreshUI(UI_SAMPLE_INFO | UI_SELECTION, samplesList.getSelectedIndex());
                    else
                        refreshUI(UI_SAMPLE_INFO | UI_SELECTION, index);
                }
            });

            // samplesList look&feel
            samplesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            samplesList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Sample sample = (value instanceof Sample) ? (Sample) value : null;
                    if (sample == null) return null;

                    JCheckBox listItem = new JCheckBox(sample.get(SAMPLE_CODE | Sample.SAMPLE_LOCATION));
                    listItem.setSelected(sample.getPacked());
                    listItem.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                    return listItem;
                }
            });
            // TODO: drag&drop moving!!

            // list selection listener for map auto refreshing
//            samplesList.addListSelectionListener(e -> refreshUI(UI_SAMPLE_INFO | UI_SELECTION));

            // list mouse adapter
            MouseAdapter listMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    int index = samplesList.locationToIndex(e.getPoint());
                    // select the sample under the cursor and invert packed-flag on right-click
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        samplesList.setSelectedIndex(samplesList.locationToIndex(e.getPoint()));
                    }
                    // invert packed-flag on double-click
                    if (SwingUtilities.isRightMouseButton(e) | (e.getClickCount() == 2)) {
                        shipment.revertSampleStatus(index);
                        samplesList.repaint();
                    }
                    // refresh infoLabels and selection
                    refreshUI(UI_SAMPLE_INFO | UI_SELECTION);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    if (autoSelect) {
                        samplesList.setSelectedIndex(samplesList.locationToIndex(e.getPoint()));
                        refreshUI(UI_SAMPLE_INFO | UI_SELECTION);
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    super.mouseWheelMoved(e);
//                    listScroll
//                    samplesList.setSelectedIndex(samplesList.locationToIndex(e.getPoint()));
//                    refreshUI(UI_SAMPLE_INFO | UI_SELECTION);
                }
            };
            samplesList.addMouseListener(listMouseAdapter);
            samplesList.addMouseMotionListener(listMouseAdapter);
            // TODO: scrolling&selecting on mouse wheel
//            samplesList.addMouseWheelListener(listMouseAdapter);
        }

        // sample add
        sampleAddButton.addActionListener(e -> {
            EditSampleUI addUI = new EditSampleUI(this, true, false);
            if (addUI.showModal()) {
                ArrayList<Sample> list = addUI.getData();
                Collections.reverse(list);
                // set index to add/insert
                int index;
                if (addUI.isAdding()) {
                    index = shipment.getSamplesCount();
                } else {
                    index = addUI.insBeforeSelection() ? samplesList.getSelectedIndex() : samplesList.getSelectedIndex() + 1;
                }
                shipment.addSamples(list, index);
            } // end if
        });

        // sample delete
        sampleDeleteButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            shipment.removeSample(index);
        });

        // sample edit
        sampleEditButton.addActionListener(e -> {
            EditSampleUI editUI = new EditSampleUI(this, true, true);
            Sample sample = shipment.getSample(samplesList.getSelectedIndex());
            if (sample == null) return;
            editUI.setData(sample.get(SAMPLE_CODE | Sample.SAMPLE_LOCATION));
            if (editUI.showModal()) {
                shipment.setSample(samplesList.getSelectedIndex(), editUI.getData().get(0));
            }
        });

        // sample move up by 1
        sampleMoveUpButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            int destination = index - 1;
            if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) destination = index - 10;
            shipment.moveSample(index, destination);
        });

        // sample move down by 1
        sampleMoveDownButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            int destination = index + 1;
            if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) destination = index + 10;
            shipment.moveSample(index, destination);
        });

        // load list
        loadListButton.addActionListener(e -> {
            // Reminder msg
            JOptionPane.showMessageDialog(this, "Проследите, чтобы случай не разбивался по разным коробкам!", "Уведомление", JOptionPane.INFORMATION_MESSAGE);
            shipment.importList();
        });

        // save map to file
        saveMapButton.addActionListener(e -> {
            shipment.setNumber(shipmentNumberField.getText());
            shipment.saveMapToFile();
        });

        // start recognizer
        startButton.addActionListener(e -> startWork());

        // debug button
        debugButton.addActionListener(e -> {
//            // up when SHIFT pressed
//            int moveUp = Boolean.compare(((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0), false);
//            // don't bypass when ALT pressed
//            int selEverything = Boolean.compare(((e.getModifiers() & ActionEvent.ALT_MASK) != 0), false) << 2;
//            // stop on end when CTRL pressed
//            int stopOnEnd = Boolean.compare(((e.getModifiers() & ActionEvent.CTRL_MASK) != 0), false) << 1;
//            String msg = "" + itemSelect(true, moveUp + stopOnEnd + selEverything);
//            log.info(msg);
//            itemSelect(false, true, true);
        });
        log.fine("Готов к работе.");
    }


    //======================================================================================================================
    private int parseNumDef(String source, int defaultValue) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            log.config("Ошибка чтения параметров: недопустимый параметр int.");
            return defaultValue;
        }
    }

    private double parseNumDef(String source, double defaultValue) {
        try {
            return Double.parseDouble(source);
        } catch (NumberFormatException e) {
            log.config("Ошибка чтения параметров: недопустимый параметр double.");
            return defaultValue;
        }
    }

    private @NotNull Font parseFont(@NotNull Properties p, String nameID, String styleID, String sizeID) {
        String fName = p.getProperty(nameID);
        int fStyle = parseNumDef(p.getProperty(styleID), Font.PLAIN);
        int fSize = parseNumDef(p.getProperty(sizeID), 10);

        switch (fStyle) {
            case 0x01:
                return new Font(fName, Font.BOLD, fSize);
            case 0x02:
                return new Font(fName, Font.ITALIC, fSize);
            case 0x03:
                return new Font(fName, Font.BOLD & Font.ITALIC, fSize);
            default:
                return new Font(fName, Font.PLAIN, fSize);
        }
    }

    private Color parseColorDef(String color, Color defColor) {
        if (color.length() < 8) return defColor;
        int[] cl = {0, 0, 0, 0}; // RGBA
        for (int i = 0; i < 4; i++) {
            try {
                cl[i] = Integer.parseInt(color.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                log.config("Ошибка чтения параметров: недопустимый параметр цвета.");
                return defColor;
            }
        }
        return new Color(cl[0], cl[1], cl[2], cl[3]);
    }

    private synchronized void refreshUI(@MagicConstant(flags = {UI_SAMPLE_INFO, UI_BOX_COUNTER, UI_SELECTION, UI_ALL}) int flags, int index) {
        if ((flags & UI_ALL) == 0) return;          // nothing to refresh
        if (shipment.getSamplesCount() == 0) return; // no refresh if list is empty
        if ((index >= shipment.getSamplesCount()) || (index < 0)) index = 0;  // if wrong index

//        int index = samplesList.isSelectionEmpty() ? 0 : samplesList.getSelectedIndex();
        // refresh infoLabels
        if ((flags & UI_SAMPLE_INFO) != 0) {
            Sample sample = shipment.getSample(index);
            if (sample != null) {
                currentSampleLabel.setText(sample.get(SAMPLE_CODE));
                fromPosLabel.setText(sample.get(Sample.SAMPLE_LOCATION));
                // TODO: сделать вычисление и отображение toPos
//                toPosLabel.setText();
            }
        }
        // refresh boxCounter
        if ((flags & UI_BOX_COUNTER) != 0) {
            boxesCountLabel.setText("Кол-во коробок: " + shipment.getBoxesCount());
        }
        // refresh selection
        if ((flags & UI_SELECTION) != 0) {
//            assert samplesList.getSelectedIndex() != 0;
            samplesList.setSelectedIndex(index);
            mapTable.changeSelection(shipment.translate(index).y, shipment.translate(index).x, false, false);
        }
    }

    private synchronized void refreshUI(@MagicConstant(flags = {UI_SAMPLE_INFO, UI_BOX_COUNTER, UI_SELECTION, UI_ALL}) int flags) {
        if (samplesList.isSelectionEmpty()) {
            refreshUI(flags, 0);
        } else {
            refreshUI(flags, samplesList.getSelectedIndex());
        }
    }

    // select next/previous (unpacked) sample in the list and return true, if the list is end may return to beginning
    private boolean itemSelect(boolean refresh, @MagicConstant(flags = {NEXT_DEFAULT, NEXT_REVERSED, NEXT_STOP_WHEN_END, NEXT_EVERY_ITEM}) int flags) {
        int index = samplesList.getSelectedIndex();
        int next = shipment.getNextIndex(index, flags);
        if (next < 0) return false;
        if (refresh) {
            refreshUI(UI_SAMPLE_INFO | UI_SELECTION, next);
        }
        return true;
    }

    private void startWork() {
        int samplesCount = shipment.getSamplesCount();
        // check if list is empty
        if (samplesCount == 0) {
            log.info("Невозможно начать работу: список пуст!");
            return;
        }
        // check if list is already done
        // Здесь можно использовать результат метода itemSelection. FALSE, если следующего элемента нет
        if (!itemSelect(false, NEXT_DEFAULT)) {
            log.info("Все образцы уже обработаны.");
            return;
        }

        if (recThread.paused()) {
            recThread.proceed();
        } else {
            recThread.pause();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setVerticalScrollBarPolicy(22);
        scrollPane1.setVisible(true);
        mainPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mapTable = new MapTable();
        mapTable.setVisible(false);
        scrollPane1.setViewportView(mapTable);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setVerticalScrollBarPolicy(22);
        mainPanel.add(scrollPane2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, -1), null, 0, false));
        samplesList = new JList<>();
        samplesList.setSelectionMode(0);
        scrollPane2.setViewportView(samplesList);
        final JSeparator separator1 = new JSeparator();
        mainPanel.add(separator1, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusBox = new JComboBox<>();
        mainPanel.add(statusBox, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(2, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        currentSampleLabel = new JLabel();
        currentSampleLabel.setText("<текущий образец>");
        panel1.add(currentSampleLabel, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("от:");
        panel1.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("до:");
        panel1.add(label2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fromPosLabel = new JLabel();
        fromPosLabel.setText("<from>");
        panel1.add(fromPosLabel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        toPosLabel = new JLabel();
        toPosLabel.setText("<to>");
        panel1.add(toPosLabel, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(372, 14), null, 0, false));
        boxesCountLabel = new JLabel();
        boxesCountLabel.setText("Кол-во коробок:");
        panel2.add(boxesCountLabel, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentPosLabel = new JLabel();
        currentPosLabel.setText("Позиция:");
        panel2.add(currentPosLabel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Номер отправки:");
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shipmentNumberField = new JTextField();
        panel2.add(shipmentNumberField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(60, -1), null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel2.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        mainPanel.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 28), null, 0, false));
        sampleDeleteButton = new JButton();
        sampleDeleteButton.setIcon(new ImageIcon(getClass().getResource("/delSample16.png")));
        sampleDeleteButton.setMinimumSize(new Dimension(-1, -1));
        sampleDeleteButton.setPreferredSize(new Dimension(32, -1));
        sampleDeleteButton.setText("");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(sampleDeleteButton, gbc);
        sampleEditButton = new JButton();
        sampleEditButton.setIcon(new ImageIcon(getClass().getResource("/change16.png")));
        sampleEditButton.setMinimumSize(new Dimension(-1, -1));
        sampleEditButton.setPreferredSize(new Dimension(32, -1));
        sampleEditButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(sampleEditButton, gbc);
        sampleMoveUpButton = new JButton();
        sampleMoveUpButton.setIcon(new ImageIcon(getClass().getResource("/upSample10.png")));
        sampleMoveUpButton.setMinimumSize(new Dimension(-1, -1));
        sampleMoveUpButton.setPreferredSize(new Dimension(32, -1));
        sampleMoveUpButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(sampleMoveUpButton, gbc);
        sampleMoveDownButton = new JButton();
        sampleMoveDownButton.setIcon(new ImageIcon(getClass().getResource("/downSample10.png")));
        sampleMoveDownButton.setMinimumSize(new Dimension(-1, -1));
        sampleMoveDownButton.setPreferredSize(new Dimension(32, -1));
        sampleMoveDownButton.setText("");
        sampleMoveDownButton.setVerticalAlignment(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(sampleMoveDownButton, gbc);
        sampleAddButton = new JButton();
        sampleAddButton.setIcon(new ImageIcon(getClass().getResource("/addSample16.png")));
        sampleAddButton.setMinimumSize(new Dimension(-1, -1));
        sampleAddButton.setPreferredSize(new Dimension(32, -1));
        sampleAddButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel3.add(sampleAddButton, gbc);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel4, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(180, -1), null, 0, false));
        loadListButton = new JButton();
        loadListButton.setIcon(new ImageIcon(getClass().getResource("/import16.png")));
        loadListButton.setText("");
        panel4.add(loadListButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(48, -1), null, 0, false));
        startButton = new JButton();
        startButton.setText("Начать работу");
        panel4.add(startButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        debugButton = new JButton();
        debugButton.setText("debug");
        panel4.add(debugButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveMapButton = new JButton();
        saveMapButton.setIcon(new ImageIcon(getClass().getResource("/save16.png")));
        saveMapButton.setText("");
        panel4.add(saveMapButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


//======================================================================================================================

}
