package controller;

import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Download {

	@FXML
	private TextField nameFile;
	
	@FXML
	private void onDownloadClick() {
		System.out.println("Want To Download The File : " + nameFile.getText());
		Main.communication_Manager.download(nameFile.getText());
		HistoryController.appendToFile(new HistoryController.Record("Download", nameFile.getText()));
	}
	
	public void clearField() {
		nameFile.setText("");
	}
}
