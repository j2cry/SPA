/*
 * Created by JFormDesigner on Thu May 21 11:54:05 MSK 2020
 */

package ru.bioresourceslab;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import net.miginfocom.swing.MigLayout;

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

/**
 * @author fragarie
 */
public class AppUIv2 extends JFrame {

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
    boolean inDeveloping = false;

    public AppUIv2() {
        initComponents();

        // frame settings
//        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1062, 360));
        setSize(new Dimension(1100, 480));
        setLocationRelativeTo(null);

        // configure logger
        ComponentLogHandler logHandler = new ComponentLogHandler();
        logHandler.setComponent(statusBox);
        final Logger log = Logger.getLogger("SPA Logger");
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

        // loading fonts for UI
        Font tableHeaderFont = loadFont(properties, "table.header.font.name", "table.header.font.style", "table.header.font.size");
        Font tableFont = loadFont(properties, "table.font.name", "table.font.style", "table.font.size");
        Font infoLabelFont = loadFont(properties, "info.font.name", "info.font.style", "info.font.size");
        Font posFont = loadFont(properties, "pos.font.name", "pos.font.style", "pos.font.size");
        currentSampleLabel.setFont(infoLabelFont);
        fromPosLabel.setFont(posFont);
        toPosLabel.setFont(posFont);
        samplesList.setFont(tableFont);
        mapTable.setFont(tableFont);
        mapTable.getTableHeader().setFont(tableHeaderFont);

        // loading fonts and settings for exporting
        Font exportHeaderFont = loadFont(properties, "export.header.font.name", "export.header.font.style", "export.header.font.size");
        Font exportFont = loadFont(properties, "export.font.name", "export.font.style", "export.font.size");
        int cellWidth = parseNumDef(properties.getProperty("export.width"), 16);
        shipment.setExportParameters(exportHeaderFont, exportFont, cellWidth);

        // loading box parameters
        int separator = parseNumDef(properties.getProperty("separator"), 2);
        int columns = parseNumDef(properties.getProperty("box.columns"), 9);
        int rows = parseNumDef(properties.getProperty("box.rows"), 9);
        shipment.setBoxOptions(rows, columns, separator);

        // loading identifiers
        listScroll.setPreferredSize(new Dimension(parseNumDef(properties.getProperty("list.width"), 132), -1));
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
                currentSampleLabel.setText(sample.get(Sample.SAMPLE_CODE));
                fromPosLabel.setText(sample.get(Sample.SAMPLE_LOCATION));
                // TODO: сделать вычисление и отображение toPos
//                    toPosLabel.setText();
                // auto select map
                mapTable.changeSelection(shipment.translate(index).y, shipment.translate(index).x, false, false);
            });


            // drawing samplesList
            class samplesListCellRenderer extends DefaultListCellRenderer {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Sample item = (Sample) value;
                    JCheckBox listItem = new JCheckBox(item.get(Sample.SAMPLE_CODE&Sample.SAMPLE_LOCATION));
                    listItem.setSelected(item.getPacked());
                    listItem.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                    return listItem;
                }
            }
            samplesListCellRenderer listRenderer = new samplesListCellRenderer();
            samplesList.setCellRenderer(listRenderer);

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

    private Font loadFont(Properties p, String nameID, String styleID, String sizeID) {
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
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - fragarie
        label1 = new JLabel();
        shipmentNumberField = new JTextField();
        hSpacer2 = new JPanel(null);
        currentPosLabel = new JLabel();
        boxesCountLabel = new JLabel();
        panel1 = new JPanel();
        hSpacer1 = new JPanel(null);
        sampleAddButton = new JButton();
        sampleDeleteButton = new JButton();
        sampleEditButton = new JButton();
        sampleMoveUpButton = new JButton();
        sampleMoveDownButton = new JButton();
        scrollPane1 = new JScrollPane();
        mapTable = new MapTable();
        listScroll = new JScrollPane();
        samplesList = new JList<>();
        panel3 = new JPanel();
        hSpacer3 = new JPanel(null);
        currentSampleLabel = new JLabel();
        label3 = new JLabel();
        fromPosLabel = new JLabel();
        hSpacer4 = new JPanel(null);
        label4 = new JLabel();
        toPosLabel = new JLabel();
        panel2 = new JPanel();
        loadListButton = new JButton();
        saveMapButton = new JButton();
        startButton = new JButton();
        debugButton = new JButton();
        separator1 = new JSeparator();
        statusBox = new JComboBox<>();

        //======== this ========
        setIconImage(new ImageIcon(getClass().getResource("/title.png")).getImage());
        Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[fill]" +
            "[shrink 0,fill]" +
            "[42,grow,fill]" +
            "[41,fill]" +
            "[60,shrink 0,fill]" +
            "[172,fill]rel",
            // rows
            "[0]0" +
            "[20]0" +
            "[264,grow]0" +
            "[shrink 0]rel" +
            "[]rel" +
            "[12,bottom]"));

        //---- label1 ----
        label1.setText("\u041d\u043e\u043c\u0435\u0440 \u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0438:");
        contentPane.add(label1, "cell 0 1");

        //---- shipmentNumberField ----
        shipmentNumberField.setPreferredSize(new Dimension(40, 24));
        contentPane.add(shipmentNumberField, "cell 1 1,alignx left,growx 0,width 40");
        contentPane.add(hSpacer2, "cell 2 1,growx");

        //---- currentPosLabel ----
        currentPosLabel.setText("\u041f\u043e\u0437\u0438\u0446\u0438\u044f: ");
        contentPane.add(currentPosLabel, "cell 3 1,gapx null 25");

        //---- boxesCountLabel ----
        boxesCountLabel.setText("\u041a\u043e\u043b-\u0432\u043e \u043a\u043e\u0440\u043e\u0431\u043e\u043a:");
        boxesCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPane.add(boxesCountLabel, "cell 4 1");

        //======== panel1 ========
        {
            panel1.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax.
            swing.border.EmptyBorder(0,0,0,0), "JFor\u006dDesi\u0067ner \u0045valu\u0061tion",javax.swing.border
            .TitledBorder.CENTER,javax.swing.border.TitledBorder.BOTTOM,new java.awt.Font("Dia\u006cog"
            ,java.awt.Font.BOLD,12),java.awt.Color.red),panel1. getBorder
            ()));panel1. addPropertyChangeListener(new java.beans.PropertyChangeListener(){@Override public void propertyChange(java
            .beans.PropertyChangeEvent e){if("bord\u0065r".equals(e.getPropertyName()))throw new RuntimeException
            ();}});
            panel1.setLayout(new MigLayout(
                "fill,hidemode 3",
                // columns
                "[139,fill]rel" +
                "[right]rel" +
                "[right]rel" +
                "[right]rel" +
                "[right]0",
                // rows
                "0[fill]0" +
                "[fill]0"));
            panel1.add(hSpacer1, "cell 0 0");

            //---- sampleAddButton ----
            sampleAddButton.setIcon(new ImageIcon(getClass().getResource("/addSample16.png")));
            panel1.add(sampleAddButton, "cell 1 0 1 2,alignx right,grow 0 100,wmax 32");

            //---- sampleDeleteButton ----
            sampleDeleteButton.setIcon(new ImageIcon(getClass().getResource("/delSample16.png")));
            panel1.add(sampleDeleteButton, "cell 2 0 1 2,alignx right,grow 0 100,width :26:32");

            //---- sampleEditButton ----
            sampleEditButton.setIcon(new ImageIcon(getClass().getResource("/change16.png")));
            panel1.add(sampleEditButton, "cell 3 0 1 2,alignx trailing,grow 0 100,wmax 32");

            //---- sampleMoveUpButton ----
            sampleMoveUpButton.setIcon(new ImageIcon(getClass().getResource("/upSample10.png")));
            panel1.add(sampleMoveUpButton, "cell 4 0,align trailing center,grow 0 0,wmax 32,hmax 15");

            //---- sampleMoveDownButton ----
            sampleMoveDownButton.setIcon(new ImageIcon(getClass().getResource("/downSample10.png")));
            panel1.add(sampleMoveDownButton, "cell 4 1,align trailing center,grow 0 0,wmax 32,hmax 15");
        }
        contentPane.add(panel1, "cell 5 1,hmax 56");

        //======== scrollPane1 ========
        {
            scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane1.setViewportView(mapTable);
        }
        contentPane.add(scrollPane1, "cell 0 2 5 1,grow");

        //======== listScroll ========
        {
            listScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            listScroll.setViewportView(samplesList);
        }
        contentPane.add(listScroll, "cell 5 2,grow,wmin 140");

        //======== panel3 ========
        {
            panel3.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[87,fill]" +
                "[fill]" +
                "[fill]0" +
                "[fill]" +
                "[53,fill]",
                // rows
                "[]" +
                "[]"));
            panel3.add(hSpacer3, "cell 0 0,growx");

            //---- currentSampleLabel ----
            currentSampleLabel.setText("<\u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u043e\u0431\u0440\u0430\u0437\u0435\u0446>");
            panel3.add(currentSampleLabel, "cell 1 0 1 2,grow");

            //---- label3 ----
            label3.setText("\u043e\u0442:");
            panel3.add(label3, "cell 2 0,aligny center,growy 0");

            //---- fromPosLabel ----
            fromPosLabel.setText("<from>");
            panel3.add(fromPosLabel, "cell 3 0");
            panel3.add(hSpacer4, "cell 4 0,growx");

            //---- label4 ----
            label4.setText("\u0434\u043e:");
            panel3.add(label4, "cell 2 1,aligny center,growy 0");

            //---- toPosLabel ----
            toPosLabel.setText("<to>");
            panel3.add(toPosLabel, "cell 3 1");
        }
        contentPane.add(panel3, "cell 0 3 5 1,grow");

        //======== panel2 ========
        {
            panel2.setLayout(new MigLayout(
                "fill,hidemode 3",
                // columns
                "0[center]" +
                "[center]" +
                "[fill]0" +
                "[0,fill]",
                // rows
                "0[]" +
                "[]0"));

            //---- loadListButton ----
            loadListButton.setIcon(new ImageIcon(getClass().getResource("/import16.png")));
            panel2.add(loadListButton, "cell 0 0,alignx center,growx 0");

            //---- saveMapButton ----
            saveMapButton.setIcon(new ImageIcon(getClass().getResource("/save16.png")));
            panel2.add(saveMapButton, "cell 1 0,alignx center,growx 0");

            //---- startButton ----
            startButton.setText("\u041d\u0430\u0447\u0430\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443");
            panel2.add(startButton, "cell 2 0,alignx center,growx 0");

            //---- debugButton ----
            debugButton.setText("debug");
            panel2.add(debugButton, "cell 2 1,alignx right,growx 0");
        }
        contentPane.add(panel2, "cell 5 3,growx");
        contentPane.add(separator1, "cell 0 4 6 1,aligny center,grow 100 0");
        contentPane.add(statusBox, "cell 0 5 6 1,aligny bottom,grow 100 0");
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - fragarie
    private JLabel label1;
    private JTextField shipmentNumberField;
    private JPanel hSpacer2;
    private JLabel currentPosLabel;
    private JLabel boxesCountLabel;
    private JPanel panel1;
    private JPanel hSpacer1;
    private JButton sampleAddButton;
    private JButton sampleDeleteButton;
    private JButton sampleEditButton;
    private JButton sampleMoveUpButton;
    private JButton sampleMoveDownButton;
    private JScrollPane scrollPane1;
    private MapTable mapTable;
    private JScrollPane listScroll;
    private JList<Sample> samplesList;
    private JPanel panel3;
    private JPanel hSpacer3;
    private JLabel currentSampleLabel;
    private JLabel label3;
    private JLabel fromPosLabel;
    private JPanel hSpacer4;
    private JLabel label4;
    private JLabel toPosLabel;
    private JPanel panel2;
    private JButton loadListButton;
    private JButton saveMapButton;
    private JButton startButton;
    private JButton debugButton;
    private JSeparator separator1;
    private JComboBox<String> statusBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
