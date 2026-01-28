package check_virus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CheckWithDefender {

	// Scan file with Windows Defender
	protected static boolean scanFileWithDefender(String filePath) throws IOException, InterruptedException {
		String[] command = { "powershell.exe", "-Command", "Start-MpScan", "-ScanPath", filePath };
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();
		int exitCode = process.waitFor();// 0 == virus // 1 == safe
		System.out.println("Exit code: " + exitCode);
		return exitCode == 1;
	}

	protected static boolean isWindowsDefenderAvailable() throws IOException, InterruptedException {
	    String[] command = { 
	        "powershell.exe", 
	        "-Command", 
	        "Get-Command Start-MpScan -ErrorAction SilentlyContinue" 
	    };

	    ProcessBuilder processBuilder = new ProcessBuilder(command);
	    processBuilder.redirectErrorStream(true); // Merge error stream with output stream

	    Process process = processBuilder.start();
	    int exitCode = process.waitFor(); // Wait for the process to complete

	    if (exitCode != 0) {
	        System.err.println("Command failed with exit code: " + exitCode);
	        return false; // Exit early if the process failed
	    }

	    // Read the output stream
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	        StringBuilder output = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            output.append(line).append("\n");
	        }

	        String outputString = output.toString();
	        System.out.println("PowerShell Output: " + outputString);

	        // Check if the output mentions 'Start-MpScan'
	        return outputString.contains("Start-MpScan");
	    }
	}


}
