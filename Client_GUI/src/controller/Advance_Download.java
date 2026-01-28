package controller;

import application.Main;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Advance_Download {
	
	@FXML
	private TextField nameFile;
	
	@FXML
	private void onDownloadClick() {
		System.out.println("Want To Search For The File : " + nameFile.getText());
		Main.communication_Manager.advDownload(nameFile.getText());
		HistoryController.appendToFile(new HistoryController.Record("Adv.Download", nameFile.getText()));
	}
	
	public void clearField() {
		nameFile.setText("");
	}
}