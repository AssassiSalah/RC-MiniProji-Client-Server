package application;

import javafx.application.Application;
import javafx.stage.Stage;

import protocol.Communication;

public class Main extends Application {

	@Override
    public void start(Stage primaryStage) {
		AppConst.communication_Manager = new Communication();
		
		new Load_Interfaces(primaryStage);
    }

	public static void main(String[] args) {
		launch(args);
	}


}
