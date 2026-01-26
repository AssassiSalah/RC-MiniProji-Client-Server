package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.FTPClient;

import java.io.File;

public class FileController {
	
	@FXML
	private Button listFilesButton;
    @FXML
    private Button uploadButton;
    @FXML
	private Button downloadButton;
    @FXML
    private Button removeButton;
    @FXML
    private ListView<String> userFileList;
    @FXML
    private ListView<String> sharedFileList;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleGroup folderToggleGroup;
    @FXML
    private RadioButton userFolderRadio;
    @FXML
    private RadioButton sharedFolderRadio;

    private FTPClient ftpClient;

    public void setFTPClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }
    
    @FXML
    private void initialize() {
        // Initialize and assign ToggleGroup to RadioButtons
        folderToggleGroup = new ToggleGroup();
        userFolderRadio.setToggleGroup(folderToggleGroup);
        sharedFolderRadio.setToggleGroup(folderToggleGroup);
    }


    // List files in both user and shared folders
    @FXML
    private void listFiles() {
        try {
            String[] userFiles = ftpClient.listFiles("USER");
            String[] sharedFiles = ftpClient.listFiles("SHARED");

            userFileList.setItems(FXCollections.observableArrayList(userFiles));
            sharedFileList.setItems(FXCollections.observableArrayList(sharedFiles));

            statusLabel.setText("File lists updated.");
        } catch (Exception e) {
            statusLabel.setText("Error listing files.");
            e.printStackTrace();
        }
    }

    // Upload a file to the selected folder (USER or SHARED)
    @FXML
    private void uploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            String folderType = getSelectedFolderType();
            if (folderType == null) {
                statusLabel.setText("Please select a target folder.");
                return;
            }

            try {
                ftpClient.uploadFile(file, folderType);
                statusLabel.setText("File uploaded successfully: " + file.getName());
                listFiles();
            } catch (Exception e) {
                statusLabel.setText("Error uploading file.");
                e.printStackTrace();
            }
        } else {
            statusLabel.setText("No file selected.");
        }
    }

    // Download a file from the selected folder (USER or SHARED)
    @FXML
    private void downloadFile() {
        String selectedFile = getSelectedFile();

        if (selectedFile == null) {
            statusLabel.setText("Please select a file to download.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File As");
        fileChooser.setInitialFileName(selectedFile);
        File saveLocation = fileChooser.showSaveDialog(new Stage());

        if (saveLocation != null) {
            String folderType = getSelectedFolderType();
            if (folderType == null) {
                statusLabel.setText("Please select a source folder.");
                return;
            }

            try {
                ftpClient.downloadFile(selectedFile, saveLocation, folderType);
                statusLabel.setText("File downloaded successfully: " + selectedFile);
            } catch (Exception e) {
                statusLabel.setText("Error downloading file.");
                e.printStackTrace();
            }
        } else {
            statusLabel.setText("Download location not selected.");
        }
    }

    // Remove a file from the selected folder (USER or SHARED)
    @FXML
    private void removeFile() {
        String selectedFile = getSelectedFile();

        if (selectedFile == null) {
            statusLabel.setText("Please select a file to remove.");
            return;
        }

        String folderType = getSelectedFolderType();
        if (folderType == null) {
            statusLabel.setText("Please select a source folder.");
            return;
        }

        try {
            ftpClient.removeFile(selectedFile, folderType);
            statusLabel.setText("File removed successfully: " + selectedFile);
            listFiles();
        } catch (Exception e) {
            statusLabel.setText("Error removing file.");
            e.printStackTrace();
        }
    }

    // Helper methods
    private String getSelectedFolderType() {
        RadioButton selectedRadio = (RadioButton) folderToggleGroup.getSelectedToggle();
        return selectedRadio != null ? selectedRadio.getText().toUpperCase() : null;
    }

    private String getSelectedFile() {
        String selectedFile = userFileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            selectedFile = sharedFileList.getSelectionModel().getSelectedItem();
        }
        return selectedFile;
    }

}
