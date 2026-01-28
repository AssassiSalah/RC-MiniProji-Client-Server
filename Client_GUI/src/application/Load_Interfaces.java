package application;

import java.io.IOException;
import java.util.logging.Logger;

import controller.Advance_Download;
import controller.Download;
import controller.Home;
import controller.Right;
import controller.Upload;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * The Load_Interfaces class is responsible for dynamically loading and managing
 * JavaFX interfaces in a BorderPane layout. It initializes the user interface
 * for a "Client File Sharing" application.
 */
public class Load_Interfaces {
	
	// 550 height and 200 + 400 + 200 width
	
	private final Logger LOGGER = Logger.getLogger(Load_Interfaces.class.getName());
	
    // Main container for the application's interface (Initialize the BorderPane)
	private static BorderPane interface_Pane = new BorderPane();
	
	private static Stage stage;
	
	private static Scene log_In_Scene = null;
	
	private static Scene main_Scene = null;
		
	private static ClassInfo log_In;
	
	private static ClassInfo home_Pane;
	
	private static ClassInfo right;
	
	private static ClassInfo download_Pane;
	
	private static ClassInfo upload_Pane;
	
	private static ClassInfo adv_Download_Pane;
	
	private static ClassInfo history;
			
	private static ClassInfo circleProgress;

	/**
     * Constructor initializes the UI by setting up the main layout and loading
     * various sections of the interface.
     * 
     * @param primaryStage the primary stage for this JavaFX application
     */
	Load_Interfaces(Stage primaryStage) {
	    
		System.out.println("Create Log_In");
		log_In = create_Log_In_Interface();
		
		// Load the home interface and assign it to the home pane
		System.out.println("Create Home");
		home_Pane = create_Home_Interface();
		
        // Add nodes to specific regions of the BorderPane
		interface_Pane.setCenter(home_Pane.getInterface());
		System.out.println("Create Left");
		interface_Pane.setLeft(create_Left_Interface().getInterface());
		System.out.println("Create Right");
		Load_Interfaces.right = create_Right_Interface();
	    interface_Pane.setRight(right.getInterface());
	    
	    // Set the top and bottom regions of the BorderPane to null (no content)
	    System.out.println("Create Top");
		interface_Pane.setTop(create_Top_Interface().getInterface());
	    interface_Pane.setBottom(null); //TODO
	    
	    System.out.println("Create Download");
	    Load_Interfaces.download_Pane = create_Download_Interface();
	    System.out.println("k");
	    
	    System.out.println("Create Upload");
	    Load_Interfaces.upload_Pane = create_Upload_Interface();
	    
	    System.out.println("Create Advance Download");
	    Load_Interfaces.adv_Download_Pane = create_Adv_Download_Interface();
	    
	    System.out.println("Create History");
	    Load_Interfaces.history = create_History_Interface();
	    
	    System.out.println("Create Circle Progress");
	    Load_Interfaces.circleProgress = create_Circle_Progress();
	    
	    //Initial The Scenes this is new addition
		interface_Pane.setCenter(circleProgress.getInterface());
		Load_Interfaces.main_Scene = new Scene(interface_Pane);
		Load_Interfaces.log_In_Scene = new Scene(log_In.getInterface());
		
        // Set up the scene and the primary stage
	    add_CSS(AppConst.Application_CSS_Name, interface_Pane);
	    primaryStage.setTitle("Wave Flow (Client File Sharing)");
	    primaryStage.getIcons().add(new Image(getClass().getResource("/image/icon blue.jpg").toString()));
	    primaryStage.setScene(log_In_Scene);   
	    primaryStage.show();
	    
	    Load_Interfaces.stage = primaryStage;
	}

	/**
     * Loads an FXML interface and applies a CSS stylesheet if provided.
     * 
     * @param FXML_Name the name of the FXML file to load (must not be empty)
     * @param CSS_Name  the name of the CSS file to apply (can be empty)
     * @return the loaded Parent node or null if the resource cannot be loaded
     */
	private ClassInfo load_Interface(String FXML_Name, String CSS_Name) {
		
		if(FXML_Name.isEmpty())
			return null;
		
		var fxmlUrl = getClass().getResource("/interfaces/" + FXML_Name);

	    if (fxmlUrl == null) {
	    	LOGGER.severe("FXML resource Not Found: " + "/interfaces/" + FXML_Name);
	        return null;
	    }
	    
		// Load the specific FXML
	    FXMLLoader loader = new FXMLLoader(fxmlUrl);
	    Parent node;
	    try {
	    	node = loader.load();
		} catch (IOException e) {
			LOGGER.severe("Cannot Load " + FXML_Name);
			e.printStackTrace();
			return null;
		}
	    
	    add_CSS(CSS_Name, node);
	    return new ClassInfo(node, loader.getController());
	}
	
	private void add_CSS(String CSS_Name, Parent node) {
		
		if(!CSS_Name.isEmpty()) {
	    	var cssUrl = getClass().getResource("/interfaces/" + CSS_Name);

	    	if (cssUrl == null)
	    		LOGGER.severe("CSS resource Not Found: " + "/interfaces/" + CSS_Name);
	    	else
	    		node.getStylesheets().add(cssUrl.toExternalForm());
	    } else
	    	System.out.println("CSS Not Exist " + CSS_Name);
	}
	
	private ClassInfo create_Log_In_Interface() {
	        return load_Interface(AppConst.LOG_IN_INTERFACE_NAME, AppConst.LOG_IN_CSS_NAME);
	    }
	
	/**
     * Loads the home interface.
     * 
     * @return the loaded Parent node or null if the resource cannot be loaded
     */
     private ClassInfo create_Home_Interface() {
        return load_Interface(AppConst.HOME_INTERFACE_NAME, "");
    }
     
     private ClassInfo create_Top_Interface() {
         return load_Interface(AppConst.TOP_INTERFACE_NAME, AppConst.TOP_CSS_NAME);
     }

    /**
     * Loads the left interface and applies the specified CSS stylesheet.
     * 
     * @return the loaded classInfo node or null if the resource cannot be loaded
     */
    private ClassInfo create_Left_Interface() {
        return load_Interface(AppConst.LEFT_INTERFACE_NAME, AppConst.LEFT_CSS_NAME);
    }

    /**
     * Loads the right interface without applying any CSS stylesheet.
     * 
     * @return the loaded Parent node or null if the resource cannot be loaded
     */
    private ClassInfo create_Right_Interface() {
        return load_Interface(AppConst.RIGHT_INTERFACE_NAME, AppConst.RIGHT_CSS_NAME);
    }

	private ClassInfo create_Download_Interface() {
		return load_Interface(AppConst.DOWNLOAD_INTERFACE_NAME, AppConst.DOWNLOAD_CSS_NAME);
	}
	
	private ClassInfo create_Upload_Interface() {
		return load_Interface(AppConst.UPLOAD_INTERFACE_NAME, AppConst.UPLOAD_CSS_NAME);
	}
	
	private ClassInfo create_Adv_Download_Interface() {
		return load_Interface(AppConst.ADVANCE_DOWNLOAD_INTERFACE_NAME, AppConst.ADVANCE_DOWNLOAD_CSS_NAME);
	}
	
	private ClassInfo create_History_Interface() {
		return load_Interface(AppConst.HISTORY_INTERFACE_NAME, AppConst.HISTORY_CSS_NAME);
	}
	
	private ClassInfo create_Circle_Progress() {
		return load_Interface(AppConst.CIRCLE_PROGRESS, AppConst.HISTORY_CSS_NAME);
	}
	
	public static void displayMainApplication() {		
		stage.close();
		stage.setScene(main_Scene);
		((Right) right.getController()).onRefreashClick();
		((Home) home_Pane.getController()).startWritingEffect();
		stage.show();
	}
	
	public static void displayLogIn() {
		stage.close();
		stage.setScene(log_In_Scene);
		stage.show();
	}
	
	public static void displayHome() {
		interface_Pane.setCenter(home_Pane.getInterface());
		clearAllFields();
	}
	
	public static void displayDownload() {
		interface_Pane.setCenter(download_Pane.getInterface());
		clearAllFields();
	}
	
	public static void displayUpload() {
		interface_Pane.setCenter(upload_Pane.getInterface());
		clearAllFields();
	}

	public static void displayAdvDownload() {
		interface_Pane.setCenter(adv_Download_Pane.getInterface());
		clearAllFields();
	}
	
	public static void displayHistory() {
		interface_Pane.setCenter(history.getInterface());
		clearAllFields();
	}
	
	public static void displayCircleProgress() {
		interface_Pane.setCenter(circleProgress.getInterface());
		//clearAllFields();
	}
	
	
	public static void startCircleProgress(double totalSize) {
		((CircularProgressBarController) circleProgress.getController()).start(totalSize);
	}
	
	public static void updateCircleProgress(double currentSize, double timeBetweenPacket) {
		((CircularProgressBarController) circleProgress.getController()).update(currentSize, timeBetweenPacket);
	}
	
	public static void clearAllFields() {
		//Test For Safety (The Correct instance)
	    if (Download.class.isInstance(download_Pane.getController())) {
	        ((Download) download_Pane.getController()).clearField();
	    }
	    
	    if (Advance_Download.class.isInstance(adv_Download_Pane.getController())) {
	        ((Advance_Download) adv_Download_Pane.getController()).clearField();
	    }
	    
	    if (Upload.class.isInstance(upload_Pane.getController())) {
	        ((Upload) upload_Pane.getController()).clearField();
	    }
	}
	
	public static void updateText(String text) {
		//Test For Safety (The Correct instance)
	    if (Download.class.isInstance(download_Pane.getController())) {
	        ((Download) download_Pane.getController()).setText(text);
	    }
	    
	    if (Advance_Download.class.isInstance(adv_Download_Pane.getController())) {
	        ((Advance_Download) adv_Download_Pane.getController()).setText(text);
	    }
	}
	
	public static void informationAlert(String title, String contentText) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait(); // Shows the alert and waits for user interaction
    }

}

class ClassInfo {
	
	private Parent _interface;
	private Object controller;
	
	public ClassInfo(Parent _interface, Object controller) {
		this._interface = _interface;
		this.controller = controller;
	}

	public Parent getInterface() {
		return _interface;
	}

	public Object getController() {
		return controller;
	}
	
}
