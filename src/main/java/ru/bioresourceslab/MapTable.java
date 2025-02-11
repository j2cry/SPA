package ru.bioresourceslab;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import java.awt.*;
import java.util.Arrays;

public class MapTable extends JTable {

    //    private final Logger log = Logger.getLogger("SPA Logger");
    private final WordWrapCellRenderer cellRenderer;
    private int[] lines;

    private Color bgPacked = new Color(0, 255, 0, 150);
    private Color bgNotPacked = new Color(255, 0, 0, 150);

    public MapTable(){
        this.getTableHeader().setReorderingAllowed(false);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setCellSelectionEnabled(true);
        this.setFillsViewportHeight(true);

        // set table header
        this.getTableHeader().setResizingAllowed(true);
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) this.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < getColumnCount(); i++) {
            this.getTableHeader().getColumnModel().getColumn(i).setCellRenderer(headerRenderer);
        }

        this.setDoubleBuffered(true);
        cellRenderer = new WordWrapCellRenderer();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        this.setRowHeight(this.getFontMetrics(font).getHeight());
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

//    @Override
//    public TableCellRenderer getCellRenderer(int row, int column) {
//        return (cellRenderer == null) ? super.getCellRenderer(row, column) : cellRenderer;
//    }

    @Override
    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        return (cellRenderer == null) ? super.getDefaultRenderer(columnClass) : cellRenderer;
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        this.getModel().addTableModelListener(e -> lines = new int[getColumnCount()]);
        if (!this.isVisible()) this.setVisible(true);
    }

    public void setPackedBackground(Color bgPacked) {
        this.bgPacked = bgPacked;
    }

    public void setNotPackedBackground(Color bgNotPacked) {
        this.bgNotPacked = bgNotPacked;
    }


    class WordWrapCellRenderer extends JTextPane implements TableCellRenderer {

        public WordWrapCellRenderer() {
            StyledDocument doc = this.getStyledDocument();
            SimpleAttributeSet center = new SimpleAttributeSet();
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
            doc.setParagraphAttributes(0, doc.getLength(), center, false);

        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Sample sample = (value instanceof Sample) ? (Sample) value : null;
            if (sample == null) return null;

            this.setFont(table.getFont());
            this.setText(sample.getPacked() ? sample.get(Sample.SAMPLE_CODE | Sample.SAMPLE_WEIGHT) : sample.get(Sample.SAMPLE_CODE));
            // drawing selection
            setBackground(isSelected ? table.getSelectionBackground() : sample.getPacked() ? bgPacked : bgNotPacked);

            // auto height
            if (lines.length == 0) return null;
            // calculation of the required number of lines
            View view = this.getUI().getRootView(this).getView(0);
            int preferredHeight = (int) view.getPreferredSpan(View.Y_AXIS);
            int lineHeight = this.getFontMetrics(this.getFont()).getHeight();
            lines[column] = Math.max((preferredHeight / lineHeight), 1);

            // finding the greatest value and set it as row height
            int maxLines = Arrays.stream(lines).max().orElse(0);
            table.setRowHeight(row, table.getRowHeight() * maxLines);
//            table.setRowHeight(row, this.getFontMetrics(getFont()).getHeight() * maxLines);

            return this;
        }
    }
}
