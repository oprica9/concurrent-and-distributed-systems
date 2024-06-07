package app.file_manager;

import app.model.ImageFileInfo;
import app.model.TextFileInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class FileOpener {

    public static void openImageFile(ImageFileInfo imageFileInfo) {
        BufferedImage image = imageFileInfo.getFileContent();
        if (image == null) {
            System.err.println("Could not open image file: " + imageFileInfo.getPath());
            return;
        }

        JLabel label = new JLabel(new ImageIcon(image));
        displayContentInFrame(imageFileInfo.getPath(), label);
    }

    public static void openTextFile(TextFileInfo textFileInfo) {
        String textContent = textFileInfo.getFileContent();
        if (textContent == null) {
            System.err.println("Could not open text file: " + textFileInfo.getPath());
            return;
        }

        JTextArea textArea = new JTextArea(textContent);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        displayContentInFrame(textFileInfo.getPath(), scrollPane);
    }

    private static void displayContentInFrame(String title, JComponent contentComponent) {
        JFrame frame = new JFrame(title);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });

        frame.getContentPane().add(contentComponent, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
