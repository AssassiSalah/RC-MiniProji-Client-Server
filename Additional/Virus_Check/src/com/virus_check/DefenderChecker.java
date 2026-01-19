package com.virus_check;

import java.io.*;

public class DefenderChecker {
    public static void scanWithDefender(String filePath) throws IOException, InterruptedException {
        String command = "powershell.exe Start-MpScan -ScanType QuickScan -File '" + filePath + "'";
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor(); // Wait for the scan to complete
        System.out.println("Windows Defender scan completed for: " + filePath);
    }
}
