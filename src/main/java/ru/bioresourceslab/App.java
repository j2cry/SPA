package ru.bioresourceslab;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppUI mainFrame = new AppUI();
            mainFrame.setVisible(true);
        });//*/
    }

}
