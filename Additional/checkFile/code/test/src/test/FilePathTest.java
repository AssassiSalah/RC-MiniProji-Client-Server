package test;

import java.io.File;

public class FilePathTest {
    public static void main(String[] args) {
    	//put your file for test if path is correct
    	//File file = new File("C:\\Users\\SMZ\\OneDrive\\Documents\\Desktop\\tpRc_ping\\tracePing.sh");
    	File file = new File("");
        if (file.exists()) {
            System.out.println("File found.");
        } else {
            System.out.println("File not found. Please check the path.");
        }
    }
}
