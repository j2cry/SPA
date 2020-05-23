package ru.bioresourceslab;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private JScrollPane listScroll;
    private JTextField shipmentNumberField;
    private MapTable mapTable;

    public static final String ACOUSTIC_MODEL =
            "resource:/cmusphinx-ru-5.2/";
    public static final String DICTIONARY_PATH =
            "resource:/cmusphinx-ru-5.2/spa.dic";
    public static final String LANGUAGE_MODEL =
            "resource:/cmusphinx-ru-5.2/spa.lm";
//    public static final String LANGUAGE_MODEL =
//            "resource:/cmusphinx-ru-5.2/3589.lm";
//    public static final String LANGUAGE_MODEL =
//            "resource:/cmusphinx-ru-5.2/6147.lm";

    Configuration configuration;
    LiveSpeechRecognizer recognizer;

    private Shipment shipment;

    // debug variables
    final Logger log = Logger.getLogger("SPA Logger");
    boolean inDeveloping = false;

    public AppUI() {
        super();

        // frame settings
        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1062, 360));
        setSize(new Dimension(1100, 480));
        setLocationRelativeTo(null);

        // configure logger
        ComponentLogHandler logHandler = new ComponentLogHandler();
        logHandler.setComponent(statusBox);
        log.addHandler(logHandler);

        // adding autosave list on exit
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
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
        });//*/
        log.info("Загрузка параметров...");

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
        configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setLanguageModelPath(LANGUAGE_MODEL);
        try {
            recognizer = new LiveSpeechRecognizer(configuration);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Ошибка распознавания: распознаватель не инициализирован! ");
            return;
        }
        log.info("Распознаватель инициализирован.");

// initialize Shipment object
        shipment = new Shipment();

// loading settings (fonts, box parameters, etc)
        String settingsFileName = "spa.properties";
        final Properties properties = new Properties();
        InputStream projectProperties;
        // initialize settings source
        try {
            String currentPath = Paths.get("").toAbsolutePath().toString();
            File settingsFile = new File(currentPath + "\\" + settingsFileName);
            projectProperties = new FileInputStream(settingsFile);
        } catch (FileNotFoundException e) {
            log.log(Level.WARNING, "Ошибка загрузки параметров: файл не найден. Загружены параметры умолчанию. ");
            projectProperties = this.getClass().getClassLoader().getResourceAsStream(settingsFileName);
        }
        try {
            assert projectProperties != null;
            properties.load(new InputStreamReader(projectProperties, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Ошибка загрузки параметров. Обратитесь к разработчику. ");
            return;
        }

        // loading parameters for UI
        Font tableHeaderFont = parseFont(properties, "table.header.font.name", "table.header.font.style", "table.header.font.size");
        Font tableFont = parseFont(properties, "table.font.name", "table.font.style", "table.font.size");
        Font infoLabelFont = parseFont(properties, "info.font.name", "info.font.style", "info.font.size");
        Font posFont = parseFont(properties, "pos.font.name", "pos.font.style", "pos.font.size");
        String packedColor = properties.getProperty("table.bgcolor.packed");
        currentSampleLabel.setFont(infoLabelFont);
        fromPosLabel.setFont(posFont);
        toPosLabel.setFont(posFont);
        samplesList.setFont(tableFont);
        mapTable.setFont(tableFont);
        mapTable.getTableHeader().setFont(tableHeaderFont);
        mapTable.setPackedColor(parseColorDef(packedColor, Color.green));

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
//        listScroll.setPreferredSize(new Dimension(parseNumDef(properties.getProperty("list.width"), 132), -1));
        String fCode = properties.getProperty("list.code");
        String fWeight = properties.getProperty("list.weight");
        String fStorage = properties.getProperty("list.storage");
        String fRack = properties.getProperty("list.rack");
        String fBox = properties.getProperty("list.box");
        String fRow = properties.getProperty("list.row");
        String fColumn = properties.getProperty("list.column");
        Sample fIDs = new Sample(fCode, fStorage, fRack, fBox, fRow, fColumn);
        fIDs.setWeight(fWeight);
        shipment.setIdentifiers(fIDs);

        // loading weight range
        double rangeLower = parseNumDef(properties.getProperty("range.lower"), 0.0);
        double rangeUpper = parseNumDef(properties.getProperty("range.upper"), 1.5);

        // loading parameters
        boolean autoSelect = properties.getProperty("autoselect").equals("true");


// initializing mapTable look&feel
        mapTable.setModel(shipment.getMapModel());

// initializing samplesList look&feel
        {
            samplesList.setModel(shipment.getListModel());

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
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    if (autoSelect) samplesList.setSelectedIndex(samplesList.locationToIndex(e.getPoint()));
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    super.mouseWheelMoved(e);
//                    listScroll
//                    samplesList.setSelectedIndex(samplesList.locationToIndex(e.getPoint()));
                }
            };
            samplesList.addMouseListener(listMouseAdapter);
            samplesList.addMouseMotionListener(listMouseAdapter);
            // TODO: scrolling&selecting on mouse wheel
//            samplesList.addMouseWheelListener(listMouseAdapter);

            // list selection listener
            samplesList.addListSelectionListener(e -> {
                if (samplesList.isSelectionEmpty()) return;
                int index = samplesList.getSelectedIndex();
                // shows hint with sample location
                Sample sample = samplesList.getModel().getElementAt(samplesList.getSelectedIndex());
                currentSampleLabel.setText(sample.getCode());
                fromPosLabel.setText(sample.getLocation());
                // TODO: сделать вычисление и отображение toPos
//                    toPosLabel.setText();
                // auto select map
                mapTable.changeSelection(shipment.translate(index).y, shipment.translate(index).x, false, false);
            });


            // drawing samplesList
            samplesList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Sample item = (Sample) value;
                    JCheckBox listItem = new JCheckBox(item.getCode() + " | " + item.getLocation());
                    listItem.setSelected(item.getPacked());
                    listItem.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                    return listItem;
                }
            });


            // TODO: popup menu or in-list drag&drop?
        }

// initializing shipmentNumberListener
        shipmentNumberField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                shipment.setNumber(shipmentNumberField.getText());
            }
        });

// initializing addSampleButton
        sampleAddButton.addActionListener(e -> {
            // TODO: UI to add sample
            shipment.addSample(new Sample("sample", "A", "B", "C", "D", "E"));
        });

// initializing delSampleButton
        sampleDeleteButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            samplesList.clearSelection();
            shipment.removeSample(index);
            if (samplesList.getModel().getSize() != 0) {
                if (index > 0) {
                    samplesList.setSelectedIndex(index - 1);
                }
                if (index == 0) {
                    samplesList.setSelectedIndex(index);
                }
            }
        });

// initializing moveUpButton
        sampleMoveUpButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            samplesList.clearSelection();
            if (shipment.moveSample(index, index - 1) == 0) {
                samplesList.setSelectedIndex(index - 1);
            } else {
                samplesList.setSelectedIndex(index);
            }
        });

// initializing moveDownButton
        sampleMoveDownButton.addActionListener(e -> {
            int index = samplesList.getSelectedIndex();
            samplesList.clearSelection();
            if (shipment.moveSample(index, index + 1) == 0) {
                samplesList.setSelectedIndex(index + 1);
            } else {
                samplesList.setSelectedIndex(index);
            }


        });

// initializing loadListButton
        loadListButton.addActionListener(e -> {
            // Reminder msg
            JOptionPane.showMessageDialog(this, "Проследите, чтобы случай не разбивался по разным коробкам!", "Уведомление", JOptionPane.INFORMATION_MESSAGE);
            shipment.importList();
            boxesCountLabel.setText("Кол-во коробок: " + shipment.getBoxesCount());
            samplesList.setSelectedIndex(0);
        });

// initializing saveMapButton
        saveMapButton.addActionListener(e -> shipment.saveMapToFile());

        startButton.addActionListener(e -> {
//            startWork();
        });

// initializing debugButton
        debugButton.addActionListener(e -> {
            // debug action
            debugAction();
        });
        log.info("Готов к работе.");
    }


    //======================================================================================================================
    private int parseNumDef(String source, int defaultValue) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseNumDef(String source, double defaultValue) {
        try {
            return Double.parseDouble(source);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Font parseFont(Properties p, String nameID, String styleID, String sizeID) {
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
                log.log(Level.WARNING, "Ошибка чтения параметров: недопустимый параметр цвета.");
                return defColor;
            }
        }
        return new Color(cl[0], cl[1], cl[2], cl[3]);
    }


    // select next/previous (unpacked) sample in the list and return true, if the list is end may return to beginning
    private boolean listSelect(boolean moveDown, boolean unpackedOnly, boolean continueWhenEnd) {
        if (samplesList.getModel().getSize() == 0) {
            return false;
        }

        int limit;
        int nextIndex;

        if (moveDown) {
            limit = continueWhenEnd ? samplesList.getModel().getSize() - 1 : samplesList.getModel().getSize() - samplesList.getSelectedIndex() - 1;
            nextIndex = samplesList.getSelectedIndex() + 1;
        } else {
            limit = continueWhenEnd ? samplesList.getModel().getSize() - 1 : samplesList.getSelectedIndex();
            nextIndex = samplesList.getSelectedIndex() - 1;
        }

        int iter = 0;
        while (iter++ != limit) {
            // reset index when END of list reached
            if (moveDown & (nextIndex > samplesList.getModel().getSize() - 1)) {
                nextIndex = 0;
            }
            // reset index when BEGIN of list reached
            if (!moveDown & (nextIndex < 0)) {
                nextIndex = samplesList.getModel().getSize() - 1;
            }
            if (!unpackedOnly || !samplesList.getModel().getElementAt(nextIndex).getPacked()) {
                samplesList.setSelectedIndex(nextIndex);
                return true;
            }
            nextIndex = moveDown ? ++nextIndex : --nextIndex;
        }
        return false;
    }

    private void startWork() {
/*        int samplesCount = samplesList.getModel().getSize();
        // check if list is empty
        if (samplesCount == 0) {
            addLog("Ошибка: список пуст!");
            return;
        }
        // check if list is already done
        for (int index = 0; index < samplesCount; index++) {
            if (samplesList.getModel().getElementAt(index).isPacked()) {
                if (index == samplesCount - 1) {
                    addLog("Ошибка: все образцы уже обработаны.");
                    return;
                }
                continue;
            }
            break;
        }

        // recognizer inner-class thread
        class RecThread extends Thread {
            private String result;

            @Override
            public void run() {
                final String COMMAND_END = "command_end";
                final String COMMAND_PREVIOUS = "command_back";
                final String COMMAND_NEXT = "command_next";

                startButton.setText("Остановить работу");
                startButton.setEnabled(false); // TODO: убрать после наладки остановки кнопкой;
                boolean endOfSamples = false;
                recognizer.startRecognition(true);
                while (!isInterrupted()) {
                    String utterance = recognizer.getResult().getHypothesis();
                    Sample sample = samplesList.getModel().getElementAt(samplesList.getSelectedIndex());
                    int index = samplesList.getSelectedIndex();

                    // if weight was recognized, setWeight and go next sample
                    if (interpretWeight(utterance)) {
                        sample.setWeight(result);
                        ((MapTableModel) mapTable.getModel()).setSample(sample.getCode() + " " + sample.getWeight(), index);
                        // output result in status-bar
                        addLog(sample.getCode() + " вес " + sample.getWeight());

                        if (!sample.isPacked()) listSwitchSampleCheck(samplesList.getSelectedIndex());
                        endOfSamples = !listSelect(true, true, true);
                        // selecting next cell in map TODO: something another way
                        mapTable.changeSelection(((MapTableModel) mapTable.getModel()).getSampleRow(index), ((MapTableModel) mapTable.getModel()).getSampleColumn(index), true, false);
                    } else {
                        // analyze commands
                        switch (result) {
                            case COMMAND_END: {
                                interrupt();
                                addLog("Команда: завершить работу");
                                break;
                            }
                            case COMMAND_NEXT: {
                                listSelect(true, false, true);
                                addLog("Команда: дальше");
                                break;
                            }
                            case COMMAND_PREVIOUS: {
                                listSelect(false, false, true);
                                addLog("Команда: назад");
                                break;
                            }
                            default: {
                                if (inDeveloping) {
                                    addLog(result);
                                } else {
                                    addLog("Не распознано. Повторите.");
                                }
                            }
                        }
                    }
                    if (endOfSamples) {
                        interrupt();
                    }
                }
                recognizer.stopRecognition();
                startButton.setText("Начать работу");
                startButton.setEnabled(true); // TODO: убрать после наладки остановки кнопкой;
            }

            private boolean interpretWeight(String source) {       // interpret the result and set it to 'result'. Returns TRUE if is a weight
                String s = source.replaceFirst(" ", ".");   // replace first 'space' to 'dot'
                String res = s.replace(" ", "");    // delete all spaces from string

                // analyze if the weight is correct
                try {
                    double weight = Double.parseDouble(res);
                    if ((weight > rangeLower) & (weight < rangeUpper)) {
                        result = String.valueOf(weight);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // if res have 'dot'-char, then it is not a command & not weight
                    if (res.indexOf('.') > -1) {
                        result = "-ERROR";
                        return false;
                    }
                }
                result = res;
                return false;
            }
        }

        // starting thread
        RecThread rec = new RecThread();
        rec.setName("SPA Recognizer thread");
        addLog("Запуск потока распознавания");
        rec.start();//*/
    }

    private void debugAction() {
        String msg = String.valueOf(listScroll.getWidth());
        log.info(msg);
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
        listScroll = new JScrollPane();
        listScroll.setVerticalScrollBarPolicy(22);
        mainPanel.add(listScroll, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, -1), null, 0, false));
        samplesList = new JList();
        listScroll.setViewportView(samplesList);
        final JSeparator separator1 = new JSeparator();
        mainPanel.add(separator1, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusBox = new JComboBox();
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
        saveMapButton = new JButton();
        saveMapButton.setIcon(new ImageIcon(getClass().getResource("/save16.png")));
        saveMapButton.setLabel("");
        saveMapButton.setText("");
        panel4.add(saveMapButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Начать работу");
        panel4.add(startButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        debugButton = new JButton();
        debugButton.setText("debug");
        panel4.add(debugButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


//======================================================================================================================

}
