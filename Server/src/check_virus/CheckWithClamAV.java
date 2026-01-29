package check_virus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CheckWithClamAV {

    // Scan file with ClamAV
    protected static boolean scanFileWithClamAV(String filePath) throws IOException, InterruptedException {
        // Command to scan a specific file
        String[] command = {"clamscan", filePath};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error stream with output stream

        System.out.println("Scanning file with ClamAV: " + filePath);
        Process process = processBuilder.start();

        // Read the output of the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("FOUND")) {
                    System.out.println("Infected file detected: " + filePath);
                    return false;
                }
            }
        }

        // Wait for the process to complete
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("ClamAV scan completed successfully for: " + filePath);
            return true;
        } else {
            System.err.println("ClamAV scan failed with exit code: " + exitCode);
            return false;
        }
    }

    // Check if ClamAV is installed
    protected static boolean isClamAVAvailable() throws IOException, InterruptedException {
        String[] command = {"which", "clamscan"};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            return line != null && !line.isEmpty();
        }
    }
}
