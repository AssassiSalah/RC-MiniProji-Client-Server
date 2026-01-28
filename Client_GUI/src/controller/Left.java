package controller;

import application.Load_Interfaces;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class Left {

    @FXML
    private Button homeButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button uploadButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button downloadAdvanceButton;

    @FXML
    private Button helpButton;
    
	public void initialize() {
		changeColor(homeButton);
		
		helpButton.setId("normal");
    }

    @FXML
    private void onHomeButtonClick() {
        System.out.println("Home button clicked!");
        changeColor(homeButton);
        Load_Interfaces.displayHome();
    }

	@FXML
    private void onDownloadButtonClick() {
        System.out.println("Download button clicked!");
        changeColor(downloadButton);
        Load_Interfaces.displayDownload();
    }

    @FXML
    private void onUploadButtonClick() {
        System.out.println("Upload button clicked!");
        changeColor(uploadButton);
        Load_Interfaces.displayUpload();
    }

    @FXML
    private void onHistoryButtonClick() {
        System.out.println("History button clicked!");
        changeColor(historyButton);
        Load_Interfaces.displayHistory();
    }

    @FXML
    private void onDownloadAdvanceButtonClick() {
        System.out.println("Download Advance button clicked!");
        changeColor(downloadAdvanceButton);
        Load_Interfaces.displayAdvDownload();
    }

    @FXML
    private void onHelpButtonClick() {
        System.out.println("help button clicked!");
        Help.openLocalHtml();
    }
    
    public void changeColor(Button selectedButton) {
        String normalId = "normal";
        String selectedId = "selected";

        // Reset all buttons to normal
        homeButton.setId(normalId);
        downloadButton.setId(normalId);
        uploadButton.setId(normalId);
        historyButton.setId(normalId);
        downloadAdvanceButton.setId(normalId);

        // Set the selected button's ID
        selectedButton.setId(selectedId);
    }
}
