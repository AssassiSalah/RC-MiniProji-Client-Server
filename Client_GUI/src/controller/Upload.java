package controller;

import java.io.File;

import application.AppConst;
import application.Load_Interfaces;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for handling the file upload functionality.
 * This class provides the functionality to select files via drag-and-drop
 * or file chooser dialog, display the file path, and trigger the file upload.
 */
public class Upload {

    @FXML
    private VBox upload_Interface;

    @FXML
    private TextField pathFile;
    
    @FXML
    private ToggleButton visibility_Public;

    /**
     * Initializes the upload interface by setting up drag-and-drop listeners.
     * The method is called automatically after the FXML is loaded.
     */
    @FXML
    private void initialize() {
        // Set up drag-and-drop listeners for the upload interface
        upload_Interface.setOnDragOver(this::handleDragOver);
        upload_Interface.setOnDragDropped(this::handleDragDropped);
    }

    /**
     * Handles the drag-over event to accept files for drag-and-drop upload.
     * 
     * @param event The drag event containing drag data.
     */
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != upload_Interface && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Handles the drag-dropped event to set the file path from the dropped file.
     * 
     * @param event The drag event containing the dropped data.
     */
    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasFiles()) {
            // Get the first file from the dragboard and set its path in the text field
            pathFile.setText(dragboard.getFiles().get(0).getAbsolutePath());
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Opens a file chooser dialog for selecting a file to import.
     * The method returns the absolute path of the selected file, or an empty string
     * if no file is selected.
     * 
     * @return The absolute path of the selected file, or an empty string if no file was selected.
     */
    @FXML
    private String onImportClick() {
        // Create a FileChooser instance
        FileChooser fileChooser = new FileChooser();
        
        // Set an optional title for the file chooser dialog
        fileChooser.setTitle("Select a File to Import");
        
        // Set an initial directory (optional)
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // Add extension filters (optional)
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        
        // Show the open dialog and get the selected file
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            // Display the file path and set it in the text field
            String filePath = selectedFile.getAbsolutePath();
            System.out.println("File selected: " + filePath);
            pathFile.setText(filePath);
            return filePath;
        } else {
            // Handle case when no file is selected
            System.out.println("No file selected.");
            pathFile.setText("");
            return "";
        }
    }

    /**
     * Triggers the file upload by calling the upload method from the communication manager.
     * It first checks if a file has been selected. If not, it will prompt the user to select a file.
     * 
     * @throws NullPointerException If the pathFile is not set correctly.
     */
    @FXML
    private void onUploadClick() {
        // Check if the pathFile text field is empty, and prompt the user to select a file if necessary
        if (pathFile.getText().isEmpty()) {
            if (onImportClick().isEmpty()) {
                Load_Interfaces.informationAlert("Select File", "You Must Select a File.");
                return;
            }
        }

        System.out.println("Want To Upload This File: " + pathFile.getText());
        
        String visibility;
        if(!visibility_Public.isSelected())
        	visibility = "private";
        else
        	visibility = "public";
        
        // Call the upload method to upload the selected file
        AppConst.communication_Manager.upload(pathFile.getText(), visibility);
        
        // Log the upload attempt in the history
        HistoryController.appendToFile(new HistoryController.Record("Upload", new File(pathFile.getText()).getName(), visibility));
    }

    /**
     * Clears the file path field, resetting the text in the pathFile text field.
     */
    public void clearField() {
        pathFile.setText("");
    }

    /**
     * Sets the file path in the pathFile text field.
     * 
     * @param text The file path to be set in the text field.
     */
    public void setText(String text) {
        pathFile.setText(text);
    }
}
