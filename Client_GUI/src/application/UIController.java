package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class UIController {
	
	private FileManager_client fileManager;
	
	@FXML
	private VBox startingPane;
	
	@FXML
	private ImageView state;
	
	@FXML
	private Button connectionButton;
	
	@FXML
	private Button exitButton;
	
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
	private Button executedButton;
	
	@FXML
	private TextField pathSelected;
	
	@FXML
	private Button importButton;
	
	@FXML
	private TextField fileName_TextField;
	
	@FXML 
	private Tooltip state_text;

	
	private Image red;
	
	private Image green;

	private boolean waitForAuthontication;

	private ConnectionManager connectionManager;
	/**
     * This method is automatically called after the FXML file is loaded.
     * It is where you can initialize the UI components.
     */
	 @FXML
	 public void initialize() {
		 red = new Image(getClass().getResource("/image/red circle.png").toString());
		 green = new Image(getClass().getResource("/image/green circle.png").toString());
		 
		 startingPane.setVisible(true);
		 userInfoPane.setVisible(false);
		 commandPane.setVisible(false);
		 listFileArea.setVisible(false);
		 progressPane.setVisible(false);
		 fileNamePane.setVisible(false);
		 filePathPane.setVisible(false);
		 connectionButton.setVisible(true);
		 exitButton.setVisible(false);
		 
		 //pathname
		 
		 state.setImage(red);
		 state_text.setText("State : Not Connected");
		 
		 username.setText("");
		 
		 waitForAuthontication = false;
		 
		// Initialize ComboBox with values
	    choiceCommand.getItems().addAll("UPLOAD", "LIST_FILES_USER", "DOWNLOAD", "REMOVE", "EXIT");
	    
		// Add a listener to handle the user's selection
	    choiceCommand.setOnAction(e -> onChoiceSelected());
	    
	    connectionManager = new ConnectionManager();
	}

	 @FXML
	private void connect() {
		String error = connectionManager.connected();
    	if(error.isEmpty()) { //Good
    		// Update log area
            logArea.appendText("Connected to the server at " + AppConstants.SERVER_ADDRESS + ":" + AppConstants.SERVER_PORT + "\n");
            
            state.setImage(green);
            state_text.setText("State : Connected");
            userInfoPane.setVisible(true);
            
            connectionButton.setVisible(false);
    		exitButton.setVisible(true);
    		waitForAuthontication = true;
    	} else {
    		// Handle connection error
        	state.setImage(red);
        	state_text.setText("State : Not Connected");
            logArea.appendText("Failed to connect to the server: " + error + "\n");
    	}
    	fileManager = new FileManager_client(connectionManager.getInputStream(), connectionManager.getOutputStream(), logArea, progressBar);
	}	
	
	@FXML
	private void authunticate() {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            DataOutputStream dataOutputStream = connectionManager.getOutputStream();
			dataOutputStream .writeUTF(username);
            dataOutputStream.writeUTF(password);

            String response = connectionManager.getInputStream().readUTF();
            logArea.appendText(response + "\n");

            if (response.contains("Successful")) {
            	startingPane.setVisible(false);
        		commandPane.setVisible(true);
                logArea.appendText("You Can Now Execute Commands.\n");
                this.username.setText(username);
                //connect_exit_button.setText("Exit");
                //connect_exit_button.setOnAction(e -> exit());
            } else {
                logArea.appendText("You Lost The Connection, Please try again.\n");
                userInfoPane.setVisible(false);
                state.setImage(red);
                state_text.setText("State : Not Connected");
            }
        } catch (IOException e) {
            logArea.appendText("Error: " + e.getMessage() + "\n");
        }
        
        waitForAuthontication = false;
    }

	@FXML
	private void exit() throws IOException {
		connectionManager.exit(waitForAuthontication);
		
		logArea.appendText("Exiting...\n");
		
        logArea.appendText("Client Disconnected Successfuly\n");
		
		startingPane.setVisible(true);
		userInfoPane.setVisible(false);
		commandPane.setVisible(false);
		
		connectionButton.setVisible(true);
		exitButton.setVisible(false);
		
		state.setImage(red);
		state_text.setText("State : Not Connected");//TODO 
		executedButton.setOnAction(e -> {}); //TODO Change This
	}
	
	public TextArea getLoArea() {
		return logArea;
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
                progressPane.setVisible(false);
                fileNamePane.setVisible(false);
                executedButton.setOnAction(e -> {
                	progressPane.setVisible(true);
                	filePathPane.setVisible(false);
                	try {
                		disableMovement(true);
                		try {
							fileManager.uploadFile(pathSelected.getText());
						} catch (NoSuchAlgorithmException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                		unableMovement(true);
                	} catch (IOException e1) {
                		e1.printStackTrace();
                }
                progressPane.setVisible(false);
                filePathPane.setVisible(true);});
                break;
            case "LIST_FILES_USER":
                // Handle the LIST_FILES_USER command
                System.out.println("Listing user files.");
                listFileArea.setVisible(true);
                progressPane.setVisible(false);
                fileNamePane.setVisible(false);
                filePathPane.setVisible(false);
                executedButton.setOnAction(ex -> {
					try {
						logArea.appendText("\n");
						List_Files_Server();
						logArea.appendText("\n");
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
                progressPane.setVisible(false);
                filePathPane.setVisible(false);
                
                executedButton.setOnAction(e -> {
                	progressPane.setVisible(true);
                    fileNamePane.setVisible(false);
                	File downloadDir = new File(AppConstants.DEFAULT_DOWNLOAD_PATH);
                if (!downloadDir.exists()) 
                	downloadDir.mkdirs();
                
                String fileName = fileName_TextField.getText();
                System.out.println("Name Of File : "+ fileName);
                try {
                	disableMovement(false);
                	fileManager.downloadFile(fileName, AppConstants.DEFAULT_DOWNLOAD_PATH);
                	unableMovement(false);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                	progressPane.setVisible(false);
                	fileNamePane.setVisible(true);
                }); //TODO
                break;
            case "REMOVE":
                // Handle the DOWNLOAD command
                System.out.println("Prepare to download file.");
                fileNamePane.setVisible(true);
                listFileArea.setVisible(false);
                progressPane.setVisible(false);
                filePathPane.setVisible(false);
                
                executedButton.setOnAction(e -> {
                	try {
                        // Send the REMOVE command to the server
                		 String fileName = fileName_TextField.getText();
                		 fileManager.removeFileFromServer(fileName);
                	} catch (IOException e1) {
                		e1.printStackTrace();
                	}
                }); //TODO
                break;
            case"EXIT":
            	fileNamePane.setVisible(false);
                listFileArea.setVisible(false);
                progressPane.setVisible(false);
                filePathPane.setVisible(false);

            	executedButton.setOnAction(ex -> {
					try {
						exit();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
            	break;
            default:
                System.out.println("Unknown command selected.");
        }
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0); // Reset progress bar
    }
        
	@FXML
    private void importFile() {
		String filePath = FileManager_client.importFile();
		pathSelected.setText(filePath);
	}

    private void List_Files_Server() throws IOException {
    	listFileArea.setText("");
    	
		connectionManager.getOutputStream().writeUTF("LIST_FILES_USER");

    	DataInputStream dataInputStream = connectionManager.getInputStream();
        String response;
        while (!(response = dataInputStream.readUTF()).equals("END")) {
        	listFileArea.appendText(response + "\n");
        }
    }
    
    private void disableMovement(boolean isUpload) {
    	executedButton.setDisable(true);
    	choiceCommand.setDisable(true);
    	
    	fileName_TextField.setDisable(true);
    	
    	importButton.setDisable(true);
    	pathSelected.setDisable(true);
    }
    
    private void unableMovement(boolean isUpload) {
    	executedButton.setDisable(false);
    	choiceCommand.setDisable(false);
    	
    	fileName_TextField.setDisable(false);
    	
    	importButton.setDisable(false);
    	pathSelected.setDisable(false);
    }
    
}
