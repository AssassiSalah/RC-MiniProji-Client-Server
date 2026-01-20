package com.virus_check;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Full_code_check {

    private static final String SAVE_FILE_PATH = System.getProperty("user.home") + "/.last_opened_path"; // Hidden file in user home
    private static boolean useVirusTotalAPI = false;
    private static boolean allFilesSafe = true;

    public static void main(String[] args) {
        // Load the last opened path from the hidden file
        File lastOpenedFile = loadLastOpenedFile();

        // Show file/folder selection dialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // Preselect the last opened file/directory if it exists
        if (lastOpenedFile != null && lastOpenedFile.exists()) {
            fileChooser.setCurrentDirectory(lastOpenedFile.getParentFile());
            fileChooser.setSelectedFile(lastOpenedFile);
        }

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            // Get selected files and directories
            File[] files = fileChooser.getSelectedFiles();

            // Save the last opened file/directory path
            saveLastOpenedFile(files[0]);

            // Flatten all files if a folder is selected
            List<File> allFiles = new ArrayList<>();
            for (File file : files) {
                if (file.isDirectory()) {
                    allFiles.addAll(getFilesFromDirectory(file)); // Get files from the selected directory
                } else {
                    allFiles.add(file); // If file is not a directory, add it directly
                }
            }

            // Show progress bar while scanning files
            AtomicInteger totalFiles = new AtomicInteger(allFiles.size());

            try (ProgressBar progressBar = new ProgressBar("Scanning Files", totalFiles.get())) {
                for (int i = 0; i < allFiles.size(); i++) {
                    File file = allFiles.get(i);

                    // Check if the user is online and the API is enabled
                    if (isOnline() && useVirusTotalAPI) {
                        try {
                            checkFileWithVirusTotal(file.getAbsolutePath());
                        } catch (IOException e) {
                            System.out.println("Error checking file with VirusTotal: " + e.getMessage());
                            allFilesSafe = false;
                        }
                    } else {
                        try {
                            scanFileWithDefender(file.getAbsolutePath());
                        } catch (IOException | InterruptedException e) {
                            System.out.println("Error scanning file with Windows Defender: " + e.getMessage());
                            allFilesSafe = false;
                        }
                    }

                    // Update progress with file index
                    progressBar.updateProgress(i + 1);
                }
            }

            // Final validation message
            if (allFilesSafe) {
                System.out.println("All files are safe!");
            } else {
                System.out.println("Some files may be infected. Please review the logs.");
            }
        }
    }

    // Load the last opened file/directory from the hidden save file
    private static File loadLastOpenedFile() {
        File saveFile = new File(SAVE_FILE_PATH);
        if (saveFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
                String path = reader.readLine();
                return path != null ? new File(path) : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Save the last opened file/directory to the hidden save file
    private static void saveLastOpenedFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE_PATH))) {
            writer.write(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get all files from a directory, including files from subfolders
    public static List<File> getFilesFromDirectory(File directory) {
        List<File> files = new ArrayList<>();
        File[] filesInDir = directory.listFiles();
        if (filesInDir != null) {
            for (File file : filesInDir) {
                if (file.isDirectory()) {
                    files.addAll(getFilesFromDirectory(file)); // Recursively call for subdirectories
                } else {
                    files.add(file); // Add file to the list
                }
            }
        }
        return files;
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

    // Check a file with VirusTotal API
    public static void checkFileWithVirusTotal(String filePath) throws IOException {
        String apiKey = ApiKeys.VIRUS_TOTAL_API_KEY.getKey(); // Replace with your API key

        // Create the file to be uploaded
        File file = new File(filePath);

        // Check if file exists
        if (!file.exists()) {
            System.out.println("The file does not exist.");
            return;
        }

        // Construct the API URL for uploading the file
        String apiUrl = "https://www.virustotal.com/api/v3/files";

        // Prepare HTTP connection for file upload
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-apikey", apiKey);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

        // Create the boundary string for multipart data
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        // Create a progress bar for upload
        long fileSize = file.length();
        long bytesUploaded = 0;

        try (OutputStream outputStream = connection.getOutputStream()) {
            // Write the file as part of the multipart request
            String fileField = "file";
            String fileName = file.getName();

            // Write the start boundary
            outputStream.write((twoHyphens + boundary + lineEnd).getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName + "\"" + lineEnd).getBytes());
            outputStream.write(("Content-Type: application/octet-stream" + lineEnd).getBytes());
            outputStream.write(lineEnd.getBytes());

            // Write the file data
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesUploaded += bytesRead;

                    // Display progress
                    int progressPercentage = (int) ((bytesUploaded / (double) fileSize) * 100);
                    System.out.print("\rUploading file: " + progressPercentage + "% - " + bytesUploaded + "/" + fileSize + " bytes");
                }
            }

            outputStream.write(lineEnd.getBytes());
            outputStream.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        }

        // Get the response code
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Parse the response to get the file's scan id
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Parse the response to get the file's hash
                String responseStr = response.toString();
                String fileHash = extractFileHashFromResponse(responseStr);
                if (fileHash != null) {
                    // Call the endpoint to get the scan report using the file's hash
                    getScanResult(fileHash, apiKey);
                } else {
                    System.out.println("Failed to get the file hash from the response.");
                }
            }
        } else {
            System.out.println("Error: Failed to upload file to VirusTotal. Response code: " + responseCode);
        }
    }

    // Method to extract file hash from the API response
    private static String extractFileHashFromResponse(String response) {
        // Extract file hash from the response (the response should contain a field like "data": {"id": "file_hash"} or similar)
        String fileHash = null;
        int startIndex = response.indexOf("\"id\":\"");
        if (startIndex != -1) {
            int endIndex = response.indexOf("\"", startIndex + 6);
            if (endIndex != -1) {
                fileHash = response.substring(startIndex + 6, endIndex);
            }
        }
        return fileHash;
    }

    // Method to get the scan report using the file's hash
    private static void getScanResult(String fileHash, String apiKey) throws IOException {
        String apiUrl = "https://www.virustotal.com/api/v3/files/" + fileHash;
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-apikey", apiKey);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Print the result (the response contains scan results)
                System.out.println("VirusTotal Scan Result: " + response.toString());

                if (response.toString().contains("malicious")) {
                    allFilesSafe = false;
                    System.out.println("WARNING: Harmful file detected!");
                }
            }
        } else {
            System.out.println("Error: Failed to get scan result. Response code: " + responseCode);
        }
    }

    // Scan file with Windows Defender
    public static void scanFileWithDefender(String filePath) throws IOException, InterruptedException {
        String command = "powershell.exe Start-MpScan -ScanType QuickScan -File '" + filePath + "'";
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        System.out.println("Windows Defender scan completed for: " + filePath);
    }

    // Simple Progress Bar class
    public static class ProgressBar implements AutoCloseable {
        private final String taskName;
        private final int total;
        private int progress;

        public ProgressBar(String taskName, int total) {
            this.taskName = taskName;
            this.total = total;
            this.progress = 0;
        }

        public void updateProgress(int currentFileIndex) {
            progress = currentFileIndex;
            int percentage = (int) ((progress / (double) total) * 100);
            System.out.print("\r" + taskName + ": " + percentage + "% - " + progress + "/" + total + " files scanned");
            if (progress == total) {
                System.out.println();
            }
        }

        @Override
        public void close() {
            System.out.println("\nScanning completed.");
        }
    }
}
