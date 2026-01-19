package test;

import java.io.File;

public class Main {

	public static void main(String[] args) {
		// Replace with the actual path to your file
		File file = new File("C:\\Users\\SMZ\\OneDrive\\Documents\\Desktop\\tpRc_ping\\tracePing.sh");
	VirusCheck check=new VirusCheck();
	check.testFile(file);
	

	}

}
