package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import network.FTPServer;

import java.io.IOException;
import java.sql.SQLException;

public class ServerController {

    @FXML
	private Button startServerButton;
    @FXML
    private Button stopServerButton;
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea fileTransferLog;

    private FTPServer ftpServer;
    private Thread serverThread;

    // Start the server
    @FXML
    private void startServer() {
        try {
            // Initialize the server
            ftpServer = new FTPServer(8080, "ftp_database.db", this);

            // Start the server in a new thread
            serverThread = new Thread(() -> ftpServer.start());
            serverThread.setDaemon(true);
            serverThread.start();

            // Update UI
            startServerButton.setDisable(true);
            stopServerButton.setDisable(false);
            logMessage("Server started on port 8080.");
        } catch (IOException | SQLException e) {
            logMessage("Failed to start the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Stop the server
    @FXML
    private void stopServer() {
        try {
            if (ftpServer != null) {
                ftpServer.stop();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }

            // Update UI
            startServerButton.setDisable(false);
            stopServerButton.setDisable(true);
            logMessage("Server stopped.");
        } catch (IOException e) {
            logMessage("Failed to stop the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a client to the list
    public void addClient(String clientInfo) {
        Platform.runLater(() -> clientList.getItems().add(clientInfo));
    }

    // Remove a client from the list
    public void removeClient(String clientInfo) {
        Platform.runLater(() -> clientList.getItems().remove(clientInfo));
    }

    // Log a file transfer event
    public void logFileTransfer(String message) {
        Platform.runLater(() -> fileTransferLog.appendText(message + "\n"));
    }

    // General log message
    private void logMessage(String message) {
        Platform.runLater(() -> fileTransferLog.appendText("[LOG] " + message + "\n"));
    }
}
