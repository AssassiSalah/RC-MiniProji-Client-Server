package check_virus;

import java.io.File;

/**
 * Main class for checking if a file is safe by leveraging different scanning methods.
 * It prioritizes internet-based scanning with the VirusTotal API and falls back to 
 * Windows Defender on Windows systems if available.
 */
public class CheckVirus {

    /**
     * Determines if a file is safe by attempting to scan it using available methods.
     * 
     * @param file the file to be scanned
     * @return {@code true} if the file is determined to be safe, {@code false} otherwise
     */
    public static boolean isSafe(File file) {
        // Check if the file exists
        if (!file.exists()) {
            System.out.println("File Not Found.");
            return false;
        }

        try {
            // Check internet connectivity for VirusTotal API
            if (CheckInternet.isOnline()) {
                System.out.println("Check With API");
                return CheckWithAPI.isSafe(file); // Use VirusTotal API if internet is available
            } 
            // If no internet, fallback to Windows Defender if on Windows
            else if (isWindows() && CheckWithDefender.isWindowsDefenderAvailable()) {
                System.out.println("Check With Windows Defender");
                // Use Windows Defender to scan the file
                return CheckWithDefender.scanFileWithDefender(file.getAbsolutePath()); // TODO: FIX THIS
            }
        } catch (Exception e) {
            // Handle exceptions during scanning
            System.out.println("Error scanning file with Windows Defender: " + e.getMessage());
            System.out.println("Some files may be infected. Please review the logs.");
        }

        // If all checks fail, indicate inability to check the file
        System.out.println("Can't Check");
        return true;//TODO for the Current Time
    }

    /**
     * Checks if the operating system is Windows.
     * 
     * @return {@code true} if the operating system is Windows, {@code false} otherwise
     */
    public static boolean isWindows() {
        // Retrieve the OS name from system properties
        return System.getProperty("os.name").contains("Windows");
    }
}
