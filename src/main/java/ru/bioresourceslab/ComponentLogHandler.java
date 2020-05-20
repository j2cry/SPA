package ru.bioresourceslab;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class ComponentLogHandler extends StreamHandler  {
    private JComboBox<String> comboBox;
    public Calendar calendar = Calendar.getInstance();
    public SimpleDateFormat now = new SimpleDateFormat("HH:mm:ss");

    public void setComponent(@NotNull JComboBox<String> comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
        if (comboBox != null) {
            comboBox.addItem("<" + now.format(calendar.getTime()) + "> [" + record.getLevel().getName() + "] " + record.getMessage());
        }
    }
}