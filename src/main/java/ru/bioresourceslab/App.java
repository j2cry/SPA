package ru.bioresourceslab;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                AppUI mainFrame = new AppUI();
                AppUIv2 mainFrame = new AppUIv2();
                mainFrame.setVisible(true);
            }
        });//*/
    }

}
