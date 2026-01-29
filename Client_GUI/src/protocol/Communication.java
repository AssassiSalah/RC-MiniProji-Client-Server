package protocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import application.Load_Interfaces;

import javafx.application.Platform;

/**
 * The {@code Communication} class is responsible for managing network communication,
 * file transfer operations (upload and download), and interaction with the user interface 
 * for a client-server protocol. It manages socket communication, sends/receives messages 
 * to/from the server, and triggers file operations like download and upload.
 * It also handles the progress display for file transfers.
 * 
 * This class connects to a server, allows file download, upload, and removal, and
 * provides utilities to monitor and display the progress of file transfers.
 * 
 * @author [Your Name]
 * @version 1.0
 * @since [Date]
 */
public class Communication {

    private ConnectionManager connectionManager;
    private FileManagerClient fileManager;

    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Initializes a new instance of the {@code Communication} class, setting up the
     * connection manager for managing server communication.
     */
    public Communication() {
        connectionManager = new ConnectionManager();
    }

    /**
     * Reads a line of input from the server.
     * 
     * @return The message received from the server, or {@code null} if an error occurs.
     */
    public String read() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("I/O error occurred while reading a line.");
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends a message to the server.
     * 
     * @param message The message to be sent to the server.
     */
    public void write(String message) {
        writer.println(message);
    }

    /**
     * Initiates a download request to the server for a specified file.
     * 
     * @param fileName The name of the file to be downloaded.
     */
    public void download(String fileName) {
        write("DOWNLOAD");
        write(fileName);
        
        System.out.println("File Name: " + fileName);

        String response = read();

        if (!response.contains("Ready")) {
            Platform.runLater(() -> {
                if (response.contains("Algo")) {
                    System.out.println("Algorithm Not Exist.");
                    Load_Interfaces.informationAlert("Error", "Algorithm does not exist.");
                } else {
                    System.out.println("File Already Exists");
                    Load_Interfaces.informationAlert("Warning", "File already exists.");
                }
            });
            return;
        }

        Platform.runLater(() -> Load_Interfaces.displayCircleProgress()); // Safely update the UI
        try {
            fileManager.downloadFile(fileName); // This runs asynchronously using a Task
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            Platform.runLater(() -> Load_Interfaces.informationAlert("Error", "An error occurred during the download."));
        }
    }

    /**
     * Initiates an advanced download request to the server for a specified file.
     * 
     * @param fileName The name of the file to be downloaded.
     */
    public void advDownload(String fileName) {
        // Create a new thread to handle the advanced download
        new Thread(() -> {
            write("ADVANCE_DOWNLOAD");
            write(fileName);

            String response = read();

            if (response.contains("File Exist")) {
                response = read();
                if (!response.contains("Ready")) {
                    System.out.println("Algorithm Not Exist.");
                    return;
                }
                try {
                    // Display the progress circle (must run on the JavaFX thread)
                    Platform.runLater(() -> Load_Interfaces.displayCircleProgress());

                    // Start the download
                    fileManager.downloadFile(fileName);

                    // Stop progress display on success (must run on the JavaFX thread)
                    Platform.runLater(() -> {
                        System.out.println("Download completed successfully.");
                        Load_Interfaces.displayCircleProgress();
                    });

                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        System.err.println("An error occurred during download: " + e.getMessage());
                        Load_Interfaces.displayCircleProgress();
                    });
                }
            } else {
                System.out.println("File does not exist or cannot be downloaded.");
            }
        }).start();
    }


    /**
     * Uploads a file to the server.
     * 
     * @param filePath The path of the file to be uploaded.
     */
    public void upload(String filePath, String visibility) {
        write("UPLOAD");
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File Not Exist");
            return;
        }

        write(file.getName());
        write(visibility);

        String serverResponse = read();
        System.out.println(serverResponse);
        if (!serverResponse.contains("Ready")) {
            if (serverResponse.contains("Algo"))
                System.out.println("Algorithm Not Exist.");
            else
                System.out.println("File Already Exists");
            return;
        }

        try {
            Load_Interfaces.displayCircleProgress();
            fileManager.uploadFile(file, file.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a specified file from the server.
     * 
     * @param fileName The name of the file to be removed.
     */
    public void removeFile(String fileName) {
        fileManager.removeFileFromServer(fileName);
    }

    /**
     * Checks if the client is connected to the server.
     * 
     * @return {@code true} if connected, {@code false} otherwise.
     */
    public boolean isConnect() {
        return connectionManager.isConnect();
    }

    /**
     * Establishes a connection to the server by initializing input/output streams 
     * and setting up the file manager for file operations.
     */
    public void connect() {
        if (!connectionManager.connected()) {
            System.exit(1);
        }

        Socket clientSocket = connectionManager.getClientSoket();
        try {
            fileManager = new FileManagerClient(this, new DataInputStream(clientSocket.getInputStream()), 
                                          new DataOutputStream(clientSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("I/O error occurred while initializing streams.");
            System.exit(1);
        } catch (NullPointerException e) {
            System.err.println("Error: clientSocket is not initialized.");
            System.exit(1);
        } catch (SecurityException e) {
            System.err.println("Error: Access denied to the socket streams.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error occurred while initializing streams: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Disconnects from the server and terminates the connection.
     */
    public void disconnect() {
        connectionManager.exit();
    }

    /**
     * Retrieves the socket associated with the client connection.
     * 
     * @return The client socket.
     */
    public Socket getSocket() {
        return connectionManager.getClientSoket();
    }

    /**
     * Starts a progress timer for a file transfer, updating the UI with the total size.
     * 
     * @param totalSize The total size of the file to be transferred.
     */
    public void startProgressTimer(long totalSize) {
        Platform.runLater(() -> {
            Load_Interfaces.startCircleProgress(totalSize);
        });
    }

    /**
     * Updates the progress timer with the current size and time for each packet during transfer.
     * 
     * @param currentSize The current size of the file being transferred.
     * @param timeOnePacket The time taken to transfer one packet.
     */
    public void updateProgressTimer(long currentSize, long timeOnePacket) {
        Platform.runLater(() -> {
            Load_Interfaces.updateCircleProgress(currentSize, timeOnePacket);
        });
    }

	public void stopCircleProgressU() {
		Platform.runLater(() -> {
	        Load_Interfaces.displayUpload();
        });
	}

	public void stopCircleProgressD() {
		Platform.runLater(() -> {
	        Load_Interfaces.displayDownload();
        });
	}
	
	public void stopCircleProgressA() {
		Platform.runLater(() -> {
	        Load_Interfaces.displayAdvDownload();
        });
	}
}
