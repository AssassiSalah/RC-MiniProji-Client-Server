package utils;

import java.io.*;
import java.net.Socket;

public class FTPClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private DataInputStream dataInput;
    private DataOutputStream dataOutput;

    // Connect to the server
    public void connectToServer(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        dataInput = new DataInputStream(socket.getInputStream());
        dataOutput = new DataOutputStream(socket.getOutputStream());
    }

    // Disconnect from the server
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    // Send a command to the server
    public void sendCommand(String command) {
        writer.println(command);
    }

    // Receive a response from the server
    public String receiveResponse() throws IOException {
        return reader.readLine();
    }

    // Upload a file to a specific folder (USER or SHARED)
    public void uploadFile(File file, String folderType) throws IOException {
        sendCommand("UPLOAD " + folderType + "/" + file.getName());
        byte[] fileContent = FileUtils.readFile(file.getAbsolutePath());
        dataOutput.write(fileContent);
        dataOutput.flush();

        String response = receiveResponse();
        if (!"UPLOAD_SUCCESS".equals(response)) {
            throw new IOException("Upload failed: " + response);
        }
    }

    // Download a file from a specific folder (USER or SHARED)
    public void downloadFile(String fileName, File saveLocation, String folderType) throws IOException {
        sendCommand("DOWNLOAD " + folderType + "/" + fileName);

        String response = receiveResponse();
        if ("DOWNLOAD_START".equals(response)) {
            try (FileOutputStream fileOutput = new FileOutputStream(saveLocation)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = dataInput.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bytesRead);
                }
            }

            String finalResponse = receiveResponse();
            if (!"DOWNLOAD_SUCCESS".equals(finalResponse)) {
                throw new IOException("Download failed: " + finalResponse);
            }
        } else {
            throw new IOException("Server error: " + response);
        }
    }

    // Remove a file from a specific folder (USER or SHARED)
    public void removeFile(String fileName, String folderType) throws IOException {
        sendCommand("REMOVE " + folderType + "/" + fileName);
        String response = receiveResponse();
        if (!"REMOVE_SUCCESS".equals(response)) {
            throw new IOException("Remove failed: " + response);
        }
    }

    // List files in a specific directory
    public String[] listFiles(String directory) throws IOException {
        sendCommand("LIST " + directory);
        String response = receiveResponse();
        return response.split("\n");
    }
}

