package com.virus_check;

import javax.swing.*; // Import for JFileChooser
import java.io.*;
import java.net.*;
import java.util.List;

public class FileVirusChecker {

    public static void main(String[] args) {
        // Show file/folder selection dialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            // Get selected files
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                // Check if the user is online
                if (isOnline()) {
                    try {
                        VirusTotalChecker.checkFile(file.getAbsolutePath());
                    } catch (IOException e) {
                        System.out.println("Error checking file with VirusTotal.");
                    }
                } else {
                    try {
                        DefenderChecker.scanWithDefender(file.getAbsolutePath());
                    } catch (IOException | InterruptedException e) {
                        System.out.println("Error scanning file with Windows Defender.");
                    }
                }
            }
        }
    }
    
    // Check if the user is online by pinging Google
    public static boolean isOnline() {
        try {
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
