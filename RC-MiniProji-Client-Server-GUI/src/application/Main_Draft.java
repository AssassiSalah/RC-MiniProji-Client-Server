package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class Main_Draft extends Application { //GUI
	
	
    private TextField usernameField;
    private PasswordField passwordField;
    private TextArea logArea;
    private ComboBox<String> commandBox;
    private TextField filePathField;
    private TextField downloadFileNameField;
    private Button executeButton;
    private Button authenticateButton;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
	private Button connectButton;
	
	private Socket socket;

    private static final String SERVER_ADDRESS = "localhost"; // "192.168.1.4"; //Error: Connection timed out: connect
    private static final int SERVER_PORT = 1234;

    @Override
    public void start(Stage primaryStage) {
    	
    	connectButton = new Button("Connect");
    	connectButton.setOnAction(e -> connected());
    	
        // Authentication panel
        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();
        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();
        authenticateButton = new Button("Authenticate");
        authenticateButton.setOnAction(e -> authenticate());

        HBox authBox = new HBox(10, usernameLabel, usernameField, passwordLabel, passwordField, authenticateButton);
        authBox.setPadding(new Insets(10));

        // Command panel
        commandBox = new ComboBox<>();
        commandBox.getItems().addAll("UPLOAD", "LIST_FILES_USER", "DOWNLOAD", "EXIT");
        commandBox.setPromptText("Select Command");

        filePathField = new TextField();
        filePathField.setPromptText("File path for upload");
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseFile());

        downloadFileNameField = new TextField();
        downloadFileNameField.setPromptText("File name to download");

        executeButton = new Button("Execute");
        executeButton.setDisable(true);
        executeButton.setOnAction(e -> executeCommand());

        VBox commandBoxLayout = new VBox(10, commandBox, new HBox(10, filePathField, browseButton), downloadFileNameField, executeButton);
        commandBoxLayout.setPadding(new Insets(10));

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        VBox mainLayout = new VBox(10, connectButton, authBox, commandBoxLayout, new Label("Logs:"), logArea);
        mainLayout.setPadding(new Insets(10));


        Scene scene = new Scene(mainLayout, 600, 400);
        primaryStage.setTitle("Authenticated File Client");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        authenticateButton.setDisable(true);
        executeButton.setDisable(true);
    }
    
    private void connected() {
        try {
            // Initialize connection to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            // Update log area
            logArea.appendText("Connected to the server at " + SERVER_ADDRESS + ":" + SERVER_PORT + "\n");

            // Enable authentication and command buttons
            authenticateButton.setDisable(false);
            executeButton.setDisable(true);

            // Disable the connect button to prevent reconnecting
            connectButton.setDisable(true);
        } catch (IOException ex) {
            // Handle connection error
            logArea.appendText("Failed to connect to the server: " + ex.getMessage() + "\n");
        }
    }


    private void authenticate() {
        try {

            String username = usernameField.getText();
            String password = passwordField.getText();

            dataOutputStream.writeUTF(username);
            dataOutputStream.writeUTF(password);

            String response = dataInputStream.readUTF();
            logArea.appendText(response + "\n");

            if (response.contains("Successful")) {
                executeButton.setDisable(false);
                authenticateButton.setDisable(true);
                logArea.appendText("Authentication successful! You can now execute commands.\n");
            } else {
                logArea.appendText("Authentication failed. Please try again.\n");
            }
        } catch (IOException e) {
            logArea.appendText("Error: " + e.getMessage() + "\n");
        }
    }

    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void executeCommand() {
        try {
            String command = commandBox.getValue();
            if (command == null) {
                logArea.appendText("Please select a command.\n");
                return;
            }

            dataOutputStream.writeUTF(command);

            switch (command) {
                case "UPLOAD":
                    String filePath = filePathField.getText();
                    if (filePath.isEmpty()) {
                        logArea.appendText("Please specify a file to upload.\n");
                        return;
                    }
                    uploadFile(filePath);
                    break;

                case "LIST_FILES_USER":
                    listFilesFromServer();
                    break;

                case "DOWNLOAD":
                    String fileName = downloadFileNameField.getText();
                    if (fileName.isEmpty()) {
                        logArea.appendText("Please specify a file name to download.\n");
                        return;
                    }
                    downloadFile(fileName, "Downloads");
                    break;

                case "EXIT":
                    logArea.appendText("Exiting...\n");
                    dataOutputStream.writeUTF("EXIT");
                    System.exit(0);
                    break;

                default:
                    logArea.appendText("Unknown command.\n");
            }
        } catch (IOException e) {
            logArea.appendText("Error: " + e.getMessage() + "\n");
        }
    }

    private void uploadFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            logArea.appendText("File not found.\n");
            return;
        }

        dataOutputStream.writeUTF(file.getName());

        String response = dataInputStream.readUTF();
        logArea.appendText(response + "\n");
        if (!response.contains("Ready")) {
            logArea.appendText("Server is not ready to receive the file.\n");
            return;
        }

        long fileSize = file.length();
        dataOutputStream.writeUTF(String.valueOf(fileSize));

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
        }

        logArea.appendText("File uploaded successfully.\n");
    }

    private void downloadFile(String fileName, String destination) throws IOException {
        dataOutputStream.writeUTF(fileName);

        String response = dataInputStream.readUTF();
        logArea.appendText(response + "\n");

        if (response.contains("Not Found")) {
            logArea.appendText("File not found on the server.\n");
            return;
        }

        File outputDir = new File(destination);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File file = new File(outputDir, fileName);

        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
        }

        logArea.appendText("File downloaded successfully.\n");
    }

    private void listFilesFromServer() throws IOException {
        String response;
        while (!(response = dataInputStream.readUTF()).equals("END")) {
            logArea.appendText(response + "\n");
        }
    }

    public static void main(String[] args) {
    	System.out.println("Brother Wrong Class (This Class Is Trash) Go To Main");
        //launch(args);
    }
}
