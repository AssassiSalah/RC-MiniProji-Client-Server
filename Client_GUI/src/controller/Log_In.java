package controller;

import application.AppConst;
import application.Load_Interfaces;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Log_In {

	@FXML
	private TextField usernameField;

	@FXML
	private TextField passwordField;
	
	private boolean handle_Username_Password(String command) {
		String username = usernameField.getText();
		String password = passwordField.getText();

		AppConst.communication_Manager.write(command);
		AppConst.communication_Manager.write(username);
		AppConst.communication_Manager.write(password);
		//communication_Manager.write(Hasher.hashPassword(password));
		
		return AppConst.communication_Manager.read().contains("Successful");
	}

	@FXML
	private void log_In() {
		if(!AppConst.communication_Manager.isConnect())
			AppConst.communication_Manager.connect();
		
		if (handle_Username_Password("LOG_IN")) {
			Load_Interfaces.informationAlert("Log In Successful", "Now You Are Authonticated");
			Load_Interfaces.displayMainApplication();
		} else {
			Load_Interfaces.informationAlert("Log In Failed", "Usernane or the Password Is Inncorect");
		}
	}

	@FXML
	private void register() {
		if(!AppConst.communication_Manager.isConnect())
			AppConst.communication_Manager.connect();
		
		if (handle_Username_Password("REGISTER")) {
			Load_Interfaces.informationAlert("Register Successful", "Now You Can Log In");
		} else {
			Load_Interfaces.informationAlert("Register Failed", "Usernane Already Exist");
		}
	}
}
