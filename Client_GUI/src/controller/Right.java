package controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import application.AppConst;
import application.Load_Interfaces;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;

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
        
        myFilesButton.setDisable(true);
        sharedFilesButton.setDisable(true);
        
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
                
        if(myFilesButton.isSelected())
        	AppConst.communication_Manager.write("LIST_FILES_USER");
        else
        	AppConst.communication_Manager.write("LIST_FILES_SHARED");
        
        String response;
        while (!(response = AppConst.communication_Manager.read()).equals("END")) {
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
    	Label selectedLabel = filesListView.getSelectionModel().getSelectedItem();
        if (selectedLabel != null) {
        	AppConst.communication_Manager.write("CHANGE_VISIBILITY");
        	
        	String nameFile = selectedLabel.getText();
        	
        	int index = nameFile.indexOf(" (Owner");
			if(index != -1) {
				nameFile = nameFile.substring(0, index);
			}
			
        	AppConst.communication_Manager.write(nameFile);
        	HistoryController.appendToFile(new HistoryController.Record("Change_Visibility", nameFile));
        } else {
            System.out.println("No File Selected");
            HistoryController.appendToFile(new HistoryController.Record("Change_Visibility", "null"));
        }
    	
        onRefreashClick();
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
        	AppConst.communication_Manager.removeFile(selectedLabel.getText());
        	HistoryController.appendToFile(new HistoryController.Record("Remove_File", selectedLabel.getText()));
        } else {
            System.out.println("No File Selected");
            HistoryController.appendToFile(new HistoryController.Record("Remove_File", "null"));
        }
        selectNothing();
        
        onRefreashClick();
    }
    
    @FXML
    public void selectMyFiles() {
    	myFilesButton.setSelected(true);
    	sharedFilesButton.setSelected(false);
    	onRefreashClick();
    }
    
    @FXML
    public void selectSharedFiles() {
    	sharedFilesButton.setSelected(true);
    	myFilesButton.setSelected(false);
    	onRefreashClick();
    }
    
    @FXML
	private void selectNothing() {
    	filesListView.getSelectionModel().clearSelection();
	}
}
