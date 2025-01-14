package ru.bioresourceslab;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static ru.bioresourceslab.Sample.DELIMITER;
import static ru.bioresourceslab.Sample.SPACER;

public class EditSampleUI extends JDialog {
    private final JTextComponent inputArea;
    private JComboBox<String> insertTypeBox;

    private boolean succeed;

//    final Logger log = Logger.getLogger("SPA Logger");


    public EditSampleUI(Frame owner, boolean modal, boolean changeMode) {
        super(owner, modal);
        JPanel addPanel = new JPanel();
        addPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));

        if (changeMode) {
            this.setTitle("Редактирование...");
            addPanel.setPreferredSize(new Dimension(200, 51));
            inputArea = new JTextField();
            addPanel.add(inputArea, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
            this.setResizable(false);
        } else {
            this.setTitle("Добавление в список...");
            addPanel.setPreferredSize(new Dimension(248, 120));
            String[] insertNames = {"Добавить в конец", "Вставить перед выделенным", "Вставить после выделенного"};
            insertTypeBox = new JComboBox<>(insertNames);
            insertTypeBox.setEditable(false);
            insertTypeBox.setSelectedIndex(2);
            addPanel.add(insertTypeBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
            final JScrollPane scrollPane1 = new JScrollPane();
            addPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
            inputArea = new JTextArea();
            scrollPane1.setViewportView(inputArea);
        }
        JButton okButton = new JButton();
        okButton.setText("ОК");
        addPanel.add(okButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JButton cancelButton = new JButton();
        cancelButton.setText("Отменить");
        addPanel.add(cancelButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        this.setContentPane(addPanel);
        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.pack();
        this.setMinimumSize(this.getSize());

        // loading icon
        Image titleIcon = Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("addSample16.png"));
        setIconImage(titleIcon);
        succeed = false;

        okButton.addActionListener(e -> {
            succeed = true;
            setVisible(false);
            dispose();
        });

        cancelButton.addActionListener(e -> {
            succeed = false;
            setVisible(false);
            dispose();
        });
        inputArea.requestFocus();
    }

    public boolean showModal() {
        this.setVisible(true);
        return !inputArea.getText().equals("") & succeed;
    }

    public ArrayList<Sample> getData() {
        String[] input;
        input = inputArea.getText().split("\n");

        ArrayList<Sample> dataList = new ArrayList<>();

        if (input.length == 0) return null;
        for (String row : input) {
            String[] loc = {"0", "0", "0", "0", "0"};
            if (row.equals("")) continue;
            // get code & location
            String[] sample = row.split(SPACER, 2);
            if (sample.length == 2) {       // do this if location is specified
                int i = 0;
                StringTokenizer sc = new StringTokenizer(sample[1], DELIMITER);
                while (sc.hasMoreTokens()) {
                    loc[i++] = sc.nextToken();
                }
            } // end if
            dataList.add(new Sample(sample[0], "", loc[0], loc[1], loc[2], loc[3], loc[4]));//*/
        } // end for

        return dataList;
    }

    public void setData(String value) {
        inputArea.setText(value);
    }

    // 0 = ADD_TO_END
    public boolean isAdding() {
        return insertTypeBox.getSelectedIndex() == 0;
    }

    // 1 = INSERT_BEFORE_SELECTION; 2 = INSERT_AFTER_SELECTION, but it doesn't matter
    public boolean insBeforeSelection() {
        return insertTypeBox.getSelectedIndex() == 1;
    }

}
