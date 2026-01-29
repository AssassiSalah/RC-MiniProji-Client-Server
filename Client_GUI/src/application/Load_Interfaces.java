package application;

import java.io.IOException;
import java.util.logging.Logger;
import controller.Advance_Download;
import controller.CircularProgressBarController;
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
 * The {@code Load_Interfaces} class is responsible for dynamically loading and
 * managing JavaFX interfaces in a {@link BorderPane} layout. It initializes the
 * user interface for a "Client File Sharing" application and provides methods
 * to transition between different screens.
 */
public class Load_Interfaces {

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
	 * Constructor for the {@code Load_Interfaces} class. Initializes various
	 * interface components and sets up the initial scenes.
	 *
	 * @param primaryStage The primary stage of the JavaFX application.
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
		interface_Pane.setBottom(null); // TODO

		System.out.println("Create Download");
		Load_Interfaces.download_Pane = create_Download_Interface();

		System.out.println("Create Upload");
		Load_Interfaces.upload_Pane = create_Upload_Interface();

		System.out.println("Create Advance Download");
		Load_Interfaces.adv_Download_Pane = create_Adv_Download_Interface();

		System.out.println("Create History");
		Load_Interfaces.history = create_History_Interface();

		System.out.println("Create Circle Progress");
		Load_Interfaces.circleProgress = create_Circle_Progress();

		// Initialize the Scenes
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
	 * Loads an FXML interface and applies the specified CSS stylesheet.
	 *
	 * @param FXML_Name The name of the FXML file.
	 * @param CSS_Name  The name of the CSS file.
	 * @return A {@link ClassInfo} object containing the loaded interface and
	 *         controller.
	 */
	private ClassInfo load_Interface(String FXML_Name, String CSS_Name) {
		if (FXML_Name.isEmpty())
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
			return null;
		}

		add_CSS(CSS_Name, node);
		return new ClassInfo(node, loader.getController());
	}

	/**
	 * Adds the specified CSS stylesheet to the given node.
	 *
	 * @param CSS_Name The name of the CSS file.
	 * @param node     The node to which the CSS will be applied.
	 */
	private void add_CSS(String CSS_Name, Parent node) {
		if (!CSS_Name.isEmpty()) {
			var cssUrl = getClass().getResource("/interfaces/" + CSS_Name);

			if (cssUrl == null)
				LOGGER.severe("CSS resource Not Found: " + "/interfaces/" + CSS_Name);
			else
				node.getStylesheets().add(cssUrl.toExternalForm());
		} else {
			System.out.println("CSS Not Exist " + CSS_Name);
		}
	}

	private ClassInfo create_Log_In_Interface() {
		return load_Interface(AppConst.LOG_IN_INTERFACE_NAME, AppConst.LOG_IN_CSS_NAME);
	}

	private ClassInfo create_Home_Interface() {
		return load_Interface(AppConst.HOME_INTERFACE_NAME, "");
	}

	private ClassInfo create_Top_Interface() {
		return load_Interface(AppConst.TOP_INTERFACE_NAME, AppConst.TOP_CSS_NAME);
	}

	private ClassInfo create_Left_Interface() {
		return load_Interface(AppConst.LEFT_INTERFACE_NAME, AppConst.LEFT_CSS_NAME);
	}

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

	/**
	 * Displays the main application screen.
	 */
	public static void displayMainApplication() {
		stage.close();
		stage.setScene(main_Scene);
		((Right) right.getController()).onRefreashClick();
		((Home) home_Pane.getController()).startWritingEffect();
		stage.show();
	}

	/**
	 * Displays the login screen.
	 */
	public static void displayLogIn() {
		stage.close();
		stage.setScene(log_In_Scene);
		stage.show();
	}

	/**
	 * Displays the home screen.
	 */
	public static void displayHome() {
		interface_Pane.setCenter(home_Pane.getInterface());
		clearAllFields();
	}

	/**
	 * Displays the download screen.
	 */
	public static void displayDownload() {
		interface_Pane.setCenter(download_Pane.getInterface());
		clearAllFields();
		((Right) right.getController()).selectMyFiles();
	}

	/**
	 * Displays the upload screen.
	 */
	public static void displayUpload() {
		interface_Pane.setCenter(upload_Pane.getInterface());
		clearAllFields();
	}

	/**
	 * Displays the advanced download screen.
	 */
	public static void displayAdvDownload() {
		interface_Pane.setCenter(adv_Download_Pane.getInterface());
		clearAllFields();
		((Right) right.getController()).selectSharedFiles();
	}

	/**
	 * Displays the history screen.
	 */
	public static void displayHistory() {
		interface_Pane.setCenter(history.getInterface());
		clearAllFields();
	}

	/**
	 * Displays the circle progress screen.
	 */
	public static void displayCircleProgress() {
		interface_Pane.setCenter(circleProgress.getInterface());
	}

	/**
	 * Starts the circular progress bar with the given total size.
	 *
	 * @param totalSize The total size for the progress.
	 */
	public static void startCircleProgress(double totalSize) {
		((CircularProgressBarController) circleProgress.getController()).initialize(totalSize);
	}

	/**
	 * Updates the circular progress bar with the current size and time between
	 * packets.
	 *
	 * @param currentSize       The current size of the download.
	 * @param timeBetweenPacket The time between packet downloads.
	 */
	public static void updateCircleProgress(double currentSize, double timeBetweenPacket) {
		((CircularProgressBarController) circleProgress.getController()).update(currentSize, timeBetweenPacket);
	}

	/**
	 * Clears all fields in the interface.
	 */
	public static void clearAllFields() {
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

	/**
	 * Updates the text in the relevant interface.
	 *
	 * @param text The text to be updated.
	 */
	public static void updateText(String text) {
		if (Download.class.isInstance(download_Pane.getController())) {
			((Download) download_Pane.getController()).setText(text);
		}

		if (Advance_Download.class.isInstance(adv_Download_Pane.getController())) {
			int index = text.indexOf(" (Owner");
			if(index != -1)
				((Advance_Download) adv_Download_Pane.getController()).setText(text.substring(0, index));
		}
	}

	/**
	 * Displays an information alert with the specified title and content text.
	 *
	 * @param title       The title of the alert.
	 * @param contentText The content of the alert.
	 */
	public static void informationAlert(String title, String contentText) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(contentText);
		alert.showAndWait(); // Shows the alert and waits for user interaction
	}
}

/**
 * The ClassInfo class is used to store the interface (FXML node) and its
 * controller for each part of the user interface. This class allows the easy
 * management and access to both the interface and controller of the FXML files
 * dynamically loaded into the application.
 */
class ClassInfo {

	private Parent _interface; // The FXML interface (UI) node
	private Object controller; // The controller associated with the FXML interface

	/**
	 * Constructs a new ClassInfo instance with the provided FXML interface and
	 * controller.
	 * 
	 * @param _interface The FXML interface (UI) node.
	 * @param controller The controller associated with the FXML interface.
	 */
	public ClassInfo(Parent _interface, Object controller) {
		this._interface = _interface;
		this.controller = controller;
	}

	/**
	 * Gets the FXML interface (UI) node.
	 * 
	 * @return The FXML interface (UI) node.
	 */
	public Parent getInterface() {
		return _interface;
	}

	/**
	 * Gets the controller associated with the FXML interface.
	 * 
	 * @return The controller object associated with the FXML interface.
	 */
	public Object getController() {
		return controller;
	}
}
