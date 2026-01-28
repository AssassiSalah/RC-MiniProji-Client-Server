package application;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import spicy.User;  // Import the User class
import spicy.UserManager;  // Assuming userManager is a class handling user operations
import spicy.User_Table;  // Import the User_Table class
import javafx.collections.ObservableList;

public class SampleController {

    @FXML
    private TableView<User> userTableView;  // Use TableView in the controller

    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> hashPasswordColumn;
    @FXML
    private TableColumn<User, String> publicKeyColumn;
    @FXML
    private TableColumn<User, String> fileColumn;  // Add the new column for "Files" button
   
    @FXML
    private TableColumn<User, String> viewStatsColumn; // Add the new column for the button
    @FXML
    private TableColumn<User, Integer> totalFilesSentColumn;
    @FXML
    private TableColumn<User, Integer> totalFilesReceivedColumn;
    @FXML
    private TableColumn<User, Integer> numberOfVirusSendsColumn;

    @FXML
    private TableColumn<User, String> lastTimeConnectedColumn;
    @FXML
    private TableColumn<User, String> lastIpUsedColumn;
    @FXML
    private TableColumn<User, Boolean> normalDisconnectColumn;
    @FXML
    private TableColumn<User, String> joinTimeColumn;
    @FXML
    private TableColumn<User, String> commandColumn;
    @FXML
    private TableColumn<User, String> visibilityColumn;
    @FXML
    private TableColumn<User, String> timeExecutionColumn;
    @FXML
    private TableColumn<User, Boolean> executedCorrectlyColumn;

    private UserManager userManager;  // Assuming userManager class exists

    // Constructor
    public SampleController() {
        this.userManager = new UserManager();  // Initialize userManager
    }

    // Method to initialize random users and populate the table
    public void initializeRandomUsersAndPopulateTable() {
        // Initialize 5 random users using userManager
        userManager.initializeRandomUsers(10);

        // Convert the List<User> to ObservableList<User>
        ObservableList<User> usersObservableList = FXCollections.observableArrayList(userManager.getUsers());

        // Populate the table with the list of users
        userTableView.setItems(usersObservableList);  // Set users in the table view
    }

    @FXML
    public void initialize() {
        // Initialize the columns in User_Table
        User_Table userTable = new User_Table();
        userTable.initializeTable(
            usernameColumn, 
            hashPasswordColumn, 
            publicKeyColumn, 
            totalFilesSentColumn, 
            totalFilesReceivedColumn, 
            numberOfVirusSendsColumn, 
            lastTimeConnectedColumn,
            lastIpUsedColumn, 
            normalDisconnectColumn, 
            joinTimeColumn, 
            commandColumn,
            visibilityColumn,
            timeExecutionColumn,
            executedCorrectlyColumn,
            viewStatsColumn,
            fileColumn);  // Initialize the new column for files
    

        // Initialize random users and populate the table
        initializeRandomUsersAndPopulateTable();  
    }
}
