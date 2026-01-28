package check_virus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides functionality to scan files using Windows Defender and check if Windows Defender
 * is available on the system.
 */
public class CheckWithDefender {

    /**
     * Scans a file for threats using Windows Defender via PowerShell.
     *
     * @param filePath the path of the file to be scanned
     * @return {@code true} if the file is safe, {@code false} otherwise
     */
    protected static boolean scanFileWithDefender(String filePath) {
        // Command to invoke Windows Defender scan
        String[] command = { "powershell.exe", "-Command", "Start-MpScan", "-ScanPath", filePath };

        // Setup the process to execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Combine error and output streams

        Process process;
        try {
            // Start the process
            process = processBuilder.start();
        } catch (IOException e) {
            System.err.println("Error during the input/output operation while initiating Windows Defender scan.");
            return false;
        }

        int exitCode;
        try {
            // Wait for the process to complete and capture the exit code
            exitCode = process.waitFor(); // 0 = Virus detected, 1 = Safe
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for the Windows Defender scan to complete.");
            return false;
        }

        System.out.println("Exit code: " + exitCode);
        // Return true if the file is safe (exit code 1)
        return exitCode == 1;
    }

    /**
     * Checks if Windows Defender is available on the system by verifying the existence of the
     * `Start-MpScan` command using PowerShell.
     *
     * @return {@code true} if Windows Defender is available, {@code false} otherwise
     */
    protected static boolean isWindowsDefenderAvailable() {
        // Command to check if the Start-MpScan command exists
        String[] command = {
            "powershell.exe",
            "-Command",
            "Get-Command Start-MpScan -ErrorAction SilentlyContinue"
        };

        // Setup the process to execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error stream with output stream

        Process process;
        try {
            // Start the process
            process = processBuilder.start();
        } catch (IOException e) {
            System.err.println("Error during the input/output operation while checking Windows Defender availability.");
            return false;
        }

        int exitCode;
        try {
            // Wait for the process to complete and capture the exit code
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for the Windows Defender availability check to complete.");
            return false;
        }

        if (exitCode != 0) {
            System.err.println("Command failed with exit code: " + exitCode);
            return false; // Exit early if the process failed
        }

        // Read the output from the PowerShell command
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String outputString = output.toString();
            System.out.println("PowerShell Output: " + outputString);

            // Check if the output contains the expected command name
            return outputString.contains("Start-MpScan");
        } catch (IOException e) {
            System.err.println("Error during the input/output operation while reading the Windows Defender check results.");
            return false;
        }
    }
}
