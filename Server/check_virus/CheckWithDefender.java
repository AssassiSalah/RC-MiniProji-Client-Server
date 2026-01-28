package check_virus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CheckWithDefender {
		
	// Scan file with Windows Defender
	protected static boolean scanFileWithDefender(String filePath) throws IOException, InterruptedException {

        // PowerShell command for scanning a specific path
		String[] command = {
			    "powershell.exe",
			    "-Command",
			    "Start-MpScan",
			    "-ScanPath",
			    filePath
		};
        //Process process = Runtime.getRuntime().exec(command);
        
        // Use ProcessBuilder for better control
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error stream with output stream

        System.out.println("Scanning file with Windows Defender: " + filePath);
        Process process = processBuilder.start();
     
        // Read the output of the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // Wait for the process to complete
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Windows Defender scan completed successfully for: " + filePath);
            return true;
        } else {
            System.err.println("Windows Defender scan failed with exit code: " + exitCode);
            return false;
        }
    }
	
	protected static boolean isWindowsDefenderAvailable() throws IOException, InterruptedException {
	    String[] command = {
	        "powershell.exe",
	        "-Command",
	        "Get-Command Start-MpScan -ErrorAction SilentlyContinue"
	    };

	    ProcessBuilder processBuilder = new ProcessBuilder(command);
	    processBuilder.redirectErrorStream(true);

	    Process process = processBuilder.start();
	    process.waitFor();

	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	        String line = reader.readLine();
	        return line != null && line.contains("Start-MpScan");
	    }
	}
	
}
