package com.virus_check;

public class main {

    public static void main(String[] args) {
        // Instantiate the VirusFileChecker class
        VirusFileChecker virusFileChecker = new VirusFileChecker();
        
        // Run the file check
        virusFileChecker.runFileCheck();
        System.out.println(virusFileChecker.isAllFilesSafe());
    }
}
