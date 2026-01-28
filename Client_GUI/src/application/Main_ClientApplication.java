package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main_ClientApplication extends Application {
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			System.out.println(getClass().getResource("ClientGUI.fxml"));

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/ClientGUI.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("Client File Sharing");
			primaryStage.setScene(scene);
			primaryStage.setResizable(true);
			primaryStage.getIcons().add(new Image(getClass().getResource("/image/client icon2.png").toString()));
			// primaryStage.setOnCloseRequest(e -> System.out.println("exit"));
			primaryStage.show();			
			// Handle key events in the scene
            //scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleKeyPress(event.getCode(), loader.getController(), root));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public static void main(String[] args) {
		launch(args);
	}
}
