package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application {
	
	private static final String DEFAULT_PATH_DOWNLOAD = System.getProperty("user.home") + "/Downloads" +"/RC_miniproj";
	
    private static final String SERVER_ADDRESS = "localhost"; // "192.168.1.4"; //Error: Connection timed out: connect
    private static final int SERVER_PORT = 1234;
	
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;	
	private Socket socket;
	
	@FXML
	private VBox startingPane;
	
	@FXML
	private Label state;
	
	//@FXML
	//private Button connect_button;
	
	@FXML 
	private TextArea logArea;
	
	@FXML
	private VBox userInfoPane;
	
	@FXML
	private TextField usernameField;
	
	@FXML
	private PasswordField passwordField;
	
	@FXML
	private VBox commandPane;
	
	@FXML
	private Label progressText;
	
	@FXML
	private ProgressBar progressBar;
	
	@FXML 
	private ComboBox<String> choiceCommand;
	
	@FXML
	private TextArea listFileArea;
	
	@FXML
	private VBox progressPane;
	
	@FXML
	private Label username;
	
	@FXML
	private VBox fileNamePane;
	
	@FXML
	private VBox filePathPane;
	
	@FXML
	private HBox userPane;
	
	@FXML
	private Button executedButton;
	
	@FXML
	private TextField pathSelected;
	
	@FXML
	private Button importButton;
	
	@FXML
	private TextField fileName_TextField;

	/**
     * This method is automatically called after the FXML file is loaded.
     * It is where you can initialize the UI components.
     */
	 @FXML
	 public void initialize() {
		 startingPane.setVisible(true);
		 userInfoPane.setVisible(false);
		 commandPane.setVisible(false);
		 listFileArea.setVisible(false);
		 progressPane.setVisible(false);
		 fileNamePane.setVisible(false);
		 filePathPane.setVisible(false);
		 userPane.setVisible(false);
		 //pathname
		 
		 state.setTextFill(Color.BLACK);
		 
		// Initialize ComboBox with values
	    choiceCommand.getItems().addAll("UPLOAD", "LIST_FILES_USER", "DOWNLOAD", "EXIT");
	    
		// Add a listener to handle the user's selection
	    choiceCommand.setOnAction(e -> onChoiceSelected());
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientGUI.fxml")); // ClientGUI
			Parent root = loader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("Client");
			primaryStage.setScene(scene);
			primaryStage.setResizable(true);
			//primaryStage.getIcons().add(new Image("/img/Robot.png"));
			primaryStage.show();			
			// Handle key events in the scene
            //scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleKeyPress(event.getCode(), loader.getController(), root));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void connected() {
        try {
            // Initialize connection to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            // Update log area
            logArea.appendText("Connected to the server at " + SERVER_ADDRESS + ":" + SERVER_PORT + "\n");
            
            state.setText("Connected");
            state.setTextFill(Color.GREEN);
            userInfoPane.setVisible(true);
        } catch (IOException ex) {
            // Handle connection error
        	state.setTextFill(Color.RED);
        	state.setText("Not Connected");
            logArea.appendText("Failed to connect to the server: " + ex.getMessage() + "\n");
        }
    }
	
	@FXML
	private void authunticate() {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            dataOutputStream.writeUTF(username);
            dataOutputStream.writeUTF(password);

            String response = dataInputStream.readUTF();
            logArea.appendText(response + "\n");

            if (response.contains("Successful")) {
            	startingPane.setVisible(false);
        		commandPane.setVisible(true);
        		userPane.setVisible(true);
                logArea.appendText("You Can Now Execute Commands.\n");
                this.username.setText(username);
                //connect_exit_button.setText("Exit");
                //connect_exit_button.setOnAction(e -> exit());
            } else {
                logArea.appendText("You Lost The Connection, Please try again.\n");
                userInfoPane.setVisible(false);
                state.setText("Not Connected");
            }
        } catch (IOException e) {
            logArea.appendText("Error: " + e.getMessage() + "\n");
        }
    }

	private void exit() {
		logArea.appendText("Exiting...\n");
        try {
			dataOutputStream.writeUTF("EXIT");
			logArea.appendText("Client Disconnected Successfuly\n");
			
			startingPane.setVisible(true);
			userInfoPane.setVisible(false);
			commandPane.setVisible(false);
			
			state.setTextFill(Color.RED);
        	state.setText("Not Connected");
			executedButton.setOnAction(e -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
     * This method is called when the user selects an item from the ComboBox.
     */
    private void onChoiceSelected() {
        // Get the selected value from the ComboBox
        // Based on the selected value, you can perform the corresponding action
        switch (choiceCommand.getValue()) {
            case "UPLOAD":
                // Handle the UPLOAD command
                System.out.println("Prepare to upload file.");
                filePathPane.setVisible(true);
                listFileArea.setVisible(false);
                progressPane.setVisible(true);
                fileNamePane.setVisible(false);
                userPane.setVisible(false);
                executedButton.setOnAction(e -> {
                	try {
                		dataOutputStream.writeUTF("UPLOAD");
                		uploadFile(pathSelected.getText());
                	} catch (IOException e1) {
                		e1.printStackTrace();
                }});
                break;
            case "LIST_FILES_USER":
                // Handle the LIST_FILES_USER command
                System.out.println("Listing user files.");
                listFileArea.setVisible(true);
                progressPane.setVisible(false);
                fileNamePane.setVisible(false);
                filePathPane.setVisible(false);
                userPane.setVisible(true);
                executedButton.setOnAction(ex -> {
					try {
						dataOutputStream.writeUTF("LIST_FILES_USER");
						logArea.appendText("\n");
						List_Files_Server();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}); 
                break;
            case "DOWNLOAD":
                // Handle the DOWNLOAD command
                System.out.println("Prepare to download file.");
                fileNamePane.setVisible(true);
                listFileArea.setVisible(false);
                progressPane.setVisible(true);
                filePathPane.setVisible(false);
                userPane.setVisible(false);
                
                executedButton.setOnAction(e -> {File downloadDir = new File(DEFAULT_PATH_DOWNLOAD);
                if (!downloadDir.exists()) 
                	downloadDir.mkdirs();
                
                String fileName = fileName_TextField.getText();
                System.out.println("Name Of File : "+ fileName);
                try {
                	dataOutputStream.writeUTF("DOWNLOAD");
					downloadFile(fileName, DEFAULT_PATH_DOWNLOAD);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}}); //TODO
                break;
            case"EXIT":
                userPane.setVisible(true);
            	fileNamePane.setVisible(false);
                listFileArea.setVisible(false);
                progressPane.setVisible(false);
                filePathPane.setVisible(false);

            	executedButton.setOnAction(ex -> exit());
            	break;
            default:
                System.out.println("Unknown command selected.");
        }
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0); // Reset progress bar
    }
    
    @FXML
    private void importFile() {
        // Create a FileChooser instance
        FileChooser fileChooser = new FileChooser();
        
        // Set an optional title for the file chooser dialog
        fileChooser.setTitle("Select a File to Import");
        
        // (Optional) Set an initial directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // (Optional) Add extension filters to limit file types
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        // Show the open dialog and get the selected file
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            // Display the file path
        	String filePath = selectedFile.getAbsolutePath();
        	pathSelected.setText(filePath);
            System.out.println("File selected: " + filePath);
        } else {
            // Handle case when no file is selected
            System.out.println("No file selected.");
        }
    }

    private void uploadFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()) {
        	System.out.println("File Not Exist");
        	return;
        }
        
        dataOutputStream.writeUTF(file.getName());
        
        String serverResponse = dataInputStream.readUTF();
        System.out.println(serverResponse);
        if (!serverResponse.contains("Ready")) {
            //System.out.println("Server not ready to receive the file.");
        	System.out.println("File Already Exist");
            return;
        }
        
        long totalSize = file.length();
        dataOutputStream.writeUTF("" + totalSize);
        
        //COMPLITE before send file check if the is virus or not (another class)

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
            	executedButton.setDisable(true);
            	choiceCommand.setDisable(true);
            	importButton.setDisable(true);
            	pathSelected.setDisable(true);
            	
                long currentSize = 0;
                System.out.println("start to upload ");
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while(currentSize < totalSize) {
                    	bytesRead = fileInputStream.read(buffer);
                    	// Check if the end of the file is reached
                        if (bytesRead == -1) {
                            break; // Exit the loop
                        }
                        dataOutputStream.write(buffer, 0, bytesRead);
                        //System.out.println(currentSize + " " + totalSize);
                        System.out.println((double) currentSize / totalSize * 100 + "%");
                        System.out.println();
                        currentSize += bytesRead; 
                        
                        // Update progress
                        double progress = (double) currentSize / totalSize;
                        updateProgress(currentSize, totalSize);

                        // Log progress percentage
                        System.out.printf("%.2f%% uploaded%n", progress * 100);
                    }
                    System.out.println((double) currentSize / totalSize * 100 + "%");
                	System.out.println();
                    
                    dataOutputStream.flush();
                    
                    executedButton.setDisable(false);
                	choiceCommand.setDisable(false);
                	importButton.setDisable(false);
                	pathSelected.setDisable(false);
                    System.out.println("File uploaded successfully.");
                    logArea.appendText(dataInputStream.readUTF() + "\n");
                } catch(Exception e) {
                	throw e;
                }
            return null;
            }
        };
        
            // Bind the progress property to the ProgressBar
            progressBar.progressProperty().bind(uploadTask.progressProperty());

            // Run the task on a background thread
            new Thread(uploadTask).start();
}

    private void List_Files_Server() throws IOException { 	
        String response;
        while (!(response = dataInputStream.readUTF()).equals("END")) {
        	logArea.appendText(response + "\n");
        }
    }
    
    private void downloadFile(String fileName, String destination) throws IOException {
        dataOutputStream.writeUTF(fileName);

        String response = dataInputStream.readUTF();
        System.out.println(response);
        
        if (response.contains("Not Found")) {
            System.out.println("Error: " + response);
            return;
        }

        File downloadedFile = new File(destination + "/" + fileName);
        
        long totalSize = Long.parseLong(dataInputStream.readUTF());
        progressBar.setProgress(0); // Reset progress bar
        
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
            	
            	executedButton.setDisable(true);
            	choiceCommand.setDisable(true);
            	fileName_TextField.setDisable(true);
            	
            	long currentSize = 0;

            	//System.out.println("start to Download ");
                try (FileOutputStream fileOut = new FileOutputStream(downloadedFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    System.out.println("Start The Reading");
                    
                    // Read file data until the end of the stream
                    while(currentSize < totalSize) {
                    	bytesRead = dataInputStream.read(buffer);
                    	
                    	// Check if the end of the file is reached
                        if (bytesRead == -1) {
                            break; // Exit the loop
                        }
                        
                    	//System.out.println(bytesRead);
                        fileOut.write(buffer, 0, bytesRead);
                        
                        System.out.println((double) currentSize / totalSize * 100 + "%");
                        System.out.println();
                        currentSize += bytesRead; 
                        
                        // Update progress
                        double progress = (double) currentSize / totalSize;
                        updateProgress(currentSize, totalSize);

                        // Log progress percentage
                        System.out.printf("%.2f%% uploaded%n", progress * 100);
                    }
                    
                    System.out.println((double) currentSize / totalSize * 100 + "%");
                	System.out.println();
                	
                	executedButton.setDisable(false);
                	choiceCommand.setDisable(false);
                	fileName_TextField.setDisable(false);
                    System.out.println("File uploaded successfully.");
                }  catch(Exception e) {
                	throw e;
                }
            return null;
            }
        };
        
        	System.out.println("File downloaded successfully.");
        	System.out.println();
        	
        	// Bind the progress property to the ProgressBar
            progressBar.progressProperty().bind(downloadTask.progressProperty());

            // Run the task on a background thread
            new Thread(downloadTask).start();
            
            //logArea.appendText(dataInputStream.readUTF() + "\n");
    }

    public static void main(String[] args) {
		launch(args);
	}

}
