package controller;

import application.Load_Interfaces;
import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Download {

	@FXML
	private TextField nameFile;
	
	@FXML
	private void onDownloadClick() {
		if(nameFile.getText().isEmpty()) {
			Load_Interfaces.informationAlert("Enter Path", "cannot upload empty path.");
			return;
		}
		
		System.out.println("Want To Download The File : " + nameFile.getText());
		Main.communication_Manager.download(nameFile.getText());
		HistoryController.appendToFile(new HistoryController.Record("Download", nameFile.getText()));
	}
	
	public void clearField() {
		nameFile.setText("");
	}
	
	public void setText(String text) {
		nameFile.setText(text);
	}
}
