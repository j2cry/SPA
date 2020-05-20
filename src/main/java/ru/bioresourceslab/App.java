package ru.bioresourceslab;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                AppUI mainFrame = new AppUI();
                mainFrame.setVisible(true);
            }
        });//*/
    }

}
