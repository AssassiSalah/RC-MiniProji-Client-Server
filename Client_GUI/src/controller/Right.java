package controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import application.Load_Interfaces;
import application.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import protocol.Communication;

public class Right {

    @FXML
    private Label timeLabel;

    @FXML
    private ListView<Label> filesListView;

    @FXML
    private Button onRefreashClick;

    @FXML
    private Button onVisibleClick;

    @FXML
    private Button onDeleteClick;
    
    @FXML
    private ToggleButton myFilesButton;
    
    @FXML
    private ToggleButton sharedFilesButton;
    
    private DateTimeFormatter time_Formatter;

    /**
     * Initializes the controller. This method is automatically called
     * after the FXML file has been loaded.
     */
    @FXML
    private void initialize() {        
        time_Formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        
        myFilesButton.setSelected(true);
        
        // Add a listener to detect item selection
        filesListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Label>() {
            @Override
            public void changed(ObservableValue<? extends Label> observable, Label oldValue, Label newValue) {
                if (newValue != null) {
                	Load_Interfaces.updateText(newValue.getText()); //send the text to the right label
                }
            }
        });
    }
    
    /**
     * Handles the action when the "Refresh" button is clicked.
     */
    @FXML
    public void onRefreashClick() {
    	System.out.println("Refreshing content...");
        filesListView.getItems().clear();
        
        Communication connectionManager = Main.communication_Manager;
        
        if(myFilesButton.isSelected())
        	connectionManager.write("LIST_FILES_USER");
        else
        	connectionManager.write("LIST_FILES_SHARED");
        
        String response;
        while (!(response = connectionManager.read()).equals("END")) {
        	filesListView.getItems().add(new Label(response));
        	//listFileArea.appendText(response + "\n");
        }
        
		HistoryController.appendToFile(new HistoryController.Record("Show_Files"));
        refreashTime();
    }
        
    /**
     * Gets the current time in "hh:mm:ss a" format.
     *
     * @return the formatted current time
     */
    private void refreashTime() {
        timeLabel.setText(LocalTime.now().format(time_Formatter));
    }

    /**
     * Handles the action when the "Visible" button is clicked.
     */
    @FXML
    private void onVisibleClick() {
    	refreashTime();
        // Example: Toggle visibility of all labels in the VBox
        boolean isVisible = filesListView.isVisible();
        filesListView.setVisible(!isVisible);
        System.out.println("Toggling visibility: " + !isVisible);
    }

    /**
     * Removes an item from the given ListView by its index.
     *
     * @param listView the ListView to remove the item from
     * @param index the index of the item to be removed
     */
    @FXML
    private void onDeleteClick() {
    	
    	Label selectedLabel = filesListView.getSelectionModel().getSelectedItem();
        if (selectedLabel != null) {
        	Main.communication_Manager.removeFile(selectedLabel.getText());
        	HistoryController.appendToFile(new HistoryController.Record("Remove_File", selectedLabel.getText()));
        } else {
            System.out.println("No File Selected");
            HistoryController.appendToFile(new HistoryController.Record("Remove_File", "null"));
        }
        selectNothing();
        
        onRefreashClick();
    }
    
    @FXML
    private void selectMyFiles() {
    	sharedFilesButton.setSelected(false);
    	onRefreashClick();
    }
    
    @FXML
    private void selectSharedFiles() {
    	myFilesButton.setSelected(false);
    	onRefreashClick();
    }
    
    @FXML
	private void selectNothing() {
    	filesListView.getSelectionModel().clearSelection();
	}
}
