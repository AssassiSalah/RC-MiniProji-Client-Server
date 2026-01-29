package controller;

import application.Load_Interfaces;
import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import protocol.Communication;

public class Top {

	@FXML
	private ImageView state;

	@FXML
	private Button connectionButton;

	@FXML
	private Button exitButton;

	@FXML
	private Tooltip state_text;

	private Image red;

	private Image green;

	@FXML
	public void initialize() {
		red = new Image(getClass().getResource("/image/red circle.png").toString());
		green = new Image(getClass().getResource("/image/green circle.png").toString());

		connected();
	}

	@FXML
	private void connect() {
		Main.communication_Manager = new Communication();
	}

	@FXML
	private void exit() {
		/*
		 * connectionManager.exit(waitForAuthontication);
		 */
		System.out.println("Exiting...\n");

		System.out.println("Client Disconnected Successfuly\n");

		// disconnected();
		
		Main.communication_Manager.disconnect();
		
		Load_Interfaces.displayLogIn();
		
		
	}

	private void connected() {
		exitButton.setVisible(true);
		connectionButton.setVisible(false);

		state.setImage(green);
		state_text.setText("State : Connected");
	}

	public void disconnected() {
		connectionButton.setVisible(true);
		exitButton.setVisible(false);

		state.setImage(red);
		state_text.setText("State : Not Connected");// TODO
	}
}
