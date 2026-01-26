package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.FTPClient;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    private FTPClient ftpClient;

    // Initialize the controller with the FTP client instance
    public void setFTPClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    // Handle login action
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            ftpClient.sendCommand("LOGIN " + username + " " + password);
            String response = ftpClient.receiveResponse();

            if (response.equals("SUCCESS")) {
                messageLabel.setText("Login successful!");
                // Navigate to the File Operations GUI
                // Load the File Operations GUI
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/file.fxml"));
                Parent root = loader.load();

                // Pass the FTPClient instance to the FileController
                FileController controller = loader.getController();
                controller.setFTPClient(ftpClient);

                // Switch to the File Operations scene
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(root));
            } else {
                messageLabel.setText("Login failed: " + response);
            }
        } catch (Exception e) {
            messageLabel.setText("Error: Unable to connect to the server.");
            e.printStackTrace();
        }
    }

    // Handle registration action
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            ftpClient.sendCommand("REGISTER " + username + " " + password);
            String response = ftpClient.receiveResponse();

            if (response.equals("SUCCESS")) {
                messageLabel.setText("Registration successful! You can now log in.");
            } else {
                messageLabel.setText("Registration failed: " + response);
            }
        } catch (Exception e) {
            messageLabel.setText("Error: Unable to connect to the server.");
            e.printStackTrace();
        }
    }
}
