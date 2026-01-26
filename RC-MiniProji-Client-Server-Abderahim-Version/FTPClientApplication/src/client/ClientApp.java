package client;

import controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.FTPClient;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());

            // Initialize FTP Client and set it in the controller
            FTPClient ftpClient = new FTPClient();
            ftpClient.connectToServer("localhost", 8080);

            LoginController controller = loader.getController();
            controller.setFTPClient(ftpClient);

            // Set stage
            primaryStage.setScene(scene);
            primaryStage.setTitle("FTP Client");
            primaryStage.show();

            // Ensure the connection is closed on exit
            primaryStage.setOnCloseRequest(e -> {
                try {
                    ftpClient.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
