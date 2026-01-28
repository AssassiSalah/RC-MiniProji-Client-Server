package application;

import javafx.application.Application;
import javafx.stage.Stage;
import model.Communication;

public class Main extends Application {

	public static Communication communication_Manager;

	@Override
	public void start(Stage primaryStage) {
		communication_Manager = new Communication();

		new Load_Interfaces(primaryStage);
	}

	public static void main(String[] args) {
		launch(args);
	}


}
