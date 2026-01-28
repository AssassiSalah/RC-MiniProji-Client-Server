package controller;

import application.Load_Interfaces;
import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import model.Communication;
import model.Hasher;

public class Log_In {

	@FXML
	private TextField usernameField;

	@FXML
	private TextField passwordField;

	
	public Communication communication_Manager;

	@FXML
	public void initialize() {
		communication_Manager = Main.communication_Manager;
	}
	
	private boolean handle_Username_Password(String command) {
		String username = usernameField.getText();
		String password = passwordField.getText();

		communication_Manager.write(command);
		communication_Manager.write(username);
		communication_Manager.write(Hasher.hashPassword(password));
		
		return communication_Manager.read().contains("Successful");
	}

	@FXML
	private void log_In() {
		
		if (handle_Username_Password("LOG_IN")) {
			Load_Interfaces.informationAlert("Log In Successful", "Now You Are Authonticated");
			Load_Interfaces.displayMainApplication();
		} else {
			Load_Interfaces.informationAlert("Log In Failed", "Usernane or the Password Is Inncorect");
		}
	}

	@FXML
	private void register() {
		if (handle_Username_Password("REGISTER")) {
			Load_Interfaces.informationAlert("Register Successful", "Now You Can Log In");
		} else {
			Load_Interfaces.informationAlert("Register Failed", "Usernane Already Exist");
		}
	}
}
