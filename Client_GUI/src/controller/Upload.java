package controller;

import java.io.File;

import application.Load_Interfaces;
import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class Upload {
	
	@FXML
	private VBox upload_Interface;
    
	@FXML
	private TextField pathFile;

    @FXML
    private void initialize() {
        // Set up drag-and-drop listeners
    	upload_Interface.setOnDragOver(this::handleDragOver);
    	upload_Interface.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != upload_Interface && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasFiles()) {
            // Get the first file
            pathFile.setText(dragboard.getFiles().get(0).getAbsolutePath());
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }
    
	@FXML
	private String onImportClick() {
		// Create a FileChooser instance
        FileChooser fileChooser = new FileChooser();
        
        // Set an optional title for the file chooser dialog
        fileChooser.setTitle("Select a File to Import");
        
        // (Optional) Set an initial directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // (Optional) Add extension filters to limit file types
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        
        // Show the open dialog and get the selected file
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            // Display the file path
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
	
	@FXML
	private void onUploadClick() {
		if(pathFile.getText().isEmpty()) {
			if(onImportClick().isEmpty()) {
				Load_Interfaces.informationAlert("Select File", "You Must Select an File.");
				return;
			}
		}
		
		System.out.println("Want To Upload This File : " + pathFile.getText());
		Main.communication_Manager.upload(pathFile.getText());
		HistoryController.appendToFile(new HistoryController.Record("Upload", new File(pathFile.getText()).getName(), "false"));
	}
	
	public void clearField() {
		pathFile.setText("");
	}
	
	public void setText(String text) {
		pathFile.setText(text);
	}
}
