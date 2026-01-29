package controller;


import application.Load_Interfaces;
import application.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Download {

	@FXML
	private TextField nameFile;
	
	@FXML
	private void onDownloadClick() {
	    if (nameFile.getText().isEmpty()) {
	        Load_Interfaces.informationAlert("Enter Path", "Cannot upload empty path.");
	        return;
	    }

	    String fileName = nameFile.getText();
	    System.out.println("Want To Download The File: " + fileName);

	    // Run the download in a new thread to avoid freezing the UI
	    new Thread(() -> {
	        try {
	            Main.communication_Manager.download(fileName);
	            // Append history entry on download success
	            Platform.runLater(() -> HistoryController.appendToFile(new HistoryController.Record("Download", fileName)));
	        } catch (Exception e) {
	            e.printStackTrace();
	            // Notify the user of an error
	            Platform.runLater(() -> Load_Interfaces.informationAlert("Error", "Download failed."));
	        }
	    }).start();
	}

	
	public void clearField() {
		nameFile.setText("");
	}
	
	public void setText(String text) {
		nameFile.setText(text);
	}
}
