package spicy;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class User_Table {

	private TableView<User> userTable;
	private ObservableList<User> userList;

	public User_Table() {
		this.userList = FXCollections.observableArrayList();
	}

	public void initializeTable(TableColumn<User, String> usernameColumn, TableColumn<User, String> hashPasswordColumn,
			TableColumn<User, String> publicKeyColumn, TableColumn<User, Integer> totalFilesSentColumn,
			TableColumn<User, Integer> totalFilesReceivedColumn, TableColumn<User, Integer> numberOfVirusSendsColumn,
			TableColumn<User, String> lastTimeConnectedColumn, TableColumn<User, String> lastIpUsedColumn,
			TableColumn<User, Boolean> normalDisconnectColumn, TableColumn<User, String> joinTimeColumn,
			TableColumn<User, String> commandColumn, TableColumn<User, String> visibilityColumn,
			TableColumn<User, String> timeExecutionColumn, TableColumn<User, Boolean> executedCorrectlyColumn,
			TableColumn<User, String> viewStatsColumn, TableColumn<User, String> fileColumn) { // New file column
		this.userTable = new TableView<>();
		this.userTable.setItems(userList);

		// Existing column initializations
		usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
		hashPasswordColumn.setCellValueFactory(new PropertyValueFactory<>("hashPassword"));
		publicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("publicKey"));
		totalFilesSentColumn.setCellValueFactory(new PropertyValueFactory<>("totalFilesSent"));
		totalFilesReceivedColumn.setCellValueFactory(new PropertyValueFactory<>("totalFilesReceived"));
		numberOfVirusSendsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfVirusSends"));
		lastTimeConnectedColumn.setCellValueFactory(new PropertyValueFactory<>("lastTimeConnected"));
		lastIpUsedColumn.setCellValueFactory(new PropertyValueFactory<>("lastIpUsed"));
		normalDisconnectColumn.setCellValueFactory(new PropertyValueFactory<>("normalDisconnect"));

		// Format LocalDateTime fields as strings for table display
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		joinTimeColumn.setCellValueFactory(
				param -> new SimpleStringProperty(param.getValue().getJoinTime().format(formatter)));

		timeExecutionColumn.setCellValueFactory(
				param -> new SimpleStringProperty(param.getValue().getTimeExecution().format(formatter)));

		visibilityColumn.setCellValueFactory(new PropertyValueFactory<>("visibility"));
		executedCorrectlyColumn.setCellValueFactory(new PropertyValueFactory<>("executedCorrectly"));

		// Set the viewStatsColumn to have a button in each cell
		viewStatsColumn.setCellValueFactory(param -> null); // No default value
		viewStatsColumn.setCellFactory(createViewStatsButtonCellFactory());

		// Set the commandColumn to have a button in each cell for showing commands
		commandColumn.setCellValueFactory(param -> null); // No default value
		commandColumn.setCellFactory(createCommandButtonCellFactory());
		// Add the new column for "Files" button
		fileColumn.setCellValueFactory(param -> null); // No default value
		fileColumn.setCellFactory(createFileButtonCellFactory()); // Button cell factory

	}

	// Factory method to create a TableCell containing a Button for the "Files"
	private Callback<TableColumn<User, String>, TableCell<User, String>> createFileButtonCellFactory() {
	    return param -> new TableCell<>() {
	        @Override
	        protected void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty);
	            if (empty || getTableRow().getItem() == null) {
	                setGraphic(null);
	            } else {
	                Button button = new Button("View Files");
	                button.setOnAction(event -> showUserFiles(getTableRow().getItem())); // Open files window
	                setGraphic(button);
	            }
	        }
	    };
	}

	private void showUserFiles(User user) {
	    Stage filesStage = new Stage();
	    filesStage.setTitle("Files for " + user.getUsername());

	    TableView<User.FileWithVisibility> filesTable = new TableView<>();

	    TableColumn<User.FileWithVisibility, String> columnFileName = new TableColumn<>("File Name");
	    columnFileName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));

	    TableColumn<User.FileWithVisibility, String> columnVisibility = new TableColumn<>("Visibility");
	    columnVisibility.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getVisibility()));

	    filesTable.getColumns().add(columnFileName);
	    filesTable.getColumns().add(columnVisibility);

	    // Retrieve categorized files
	    ObservableList<User.FileWithVisibility> filesList = FXCollections.observableArrayList(
	        user.getFilesWithVisibility()
	    );
	    filesTable.setItems(filesList);

	    Scene scene = new Scene(filesTable, 400, 300);
	    filesStage.setScene(scene);
	    filesStage.show();
	}

	// Factory method to create a TableCell containing a Button for the "View
	private Callback<TableColumn<User, String>, TableCell<User, String>> createCommandButtonCellFactory() {
	    return param -> new TableCell<User, String>() {
	        @Override
	        protected void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty);
	            if (empty || getTableRow().getItem() == null) {
	                setGraphic(null);
	            } else {
	                Button button = new Button("View Commands");
	                button.setOnAction(event -> showUserCommands(getTableRow().getItem())); // Show commands on button click
	                setGraphic(button);
	            }
	        }
	    };
	}

	private void showUserCommands(User user) {
	    // Create a new window (Stage)
	    Stage commandsStage = new Stage();
	    commandsStage.setTitle("Commands for " + user.getUsername());

	    // Create a new TableView for displaying commands
	    TableView<CommandExecutionState> commandsTable = new TableView<>();

	    // Define columns for the commands table
	    TableColumn<CommandExecutionState, String> columnCommand = new TableColumn<>("Command");
	    TableColumn<CommandExecutionState, String> columnExecutedCorrectly = new TableColumn<>("Executed Correctly");

	    columnCommand.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCommand()));
	    columnExecutedCorrectly.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().isExecutedCorrectly() ? "Yes" : "No"));

	    // Add the columns to the table
	    commandsTable.getColumns().add(columnCommand);
	    commandsTable.getColumns().add(columnExecutedCorrectly);

	    // Create a list of commands for the user
	    ObservableList<CommandExecutionState> commandsList = FXCollections.observableArrayList(user.getCommands());

	    // Set the data for the commands table
	    commandsTable.setItems(commandsList);

	    // Create the scene and add it to the stage
	    Scene scene = new Scene(commandsTable, 400, 300);
	    commandsStage.setScene(scene);

	    // Show the new stage
	    commandsStage.show();
	}

	// Method to populate the table with a list of users
	public void populateUserTable(List<User> users) {
		userList.setAll(users); // Replace current data with new data
	}

	// Factory method to create a TableCell containing a Button for the "View Stats"
	// button
	private Callback<TableColumn<User, String>, TableCell<User, String>> createViewStatsButtonCellFactory() {
		return param -> new TableCell<User, String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					Button button = new Button("View Stats");
					button.setOnAction(event -> showUserStats(getTableRow().getItem())); // Show stats on button click
					setGraphic(button);
				}
			}
		};
	}

	private void showUserStats(User user) {
		// Create a new window (Stage)
		Stage statsStage = new Stage();
		statsStage.setTitle("User Stats for " + user.getUsername());

		// Create a new TableView for displaying stats
		TableView<UserStats> statsTable = new TableView<>();

		// Define columns for the stats table
		TableColumn<UserStats, String> columnName = new TableColumn<>("Stat");
		TableColumn<UserStats, Integer> columnValue = new TableColumn<>("Value");

		columnName.setCellValueFactory(new PropertyValueFactory<>("statName"));
		columnValue.setCellValueFactory(new PropertyValueFactory<>("statValue"));

		// Add the columns to the table
		statsTable.getColumns().add(columnName);
		statsTable.getColumns().add(columnValue);

		// Create a list of stats for the user
		ObservableList<UserStats> statsList = FXCollections.observableArrayList(
				new UserStats("Total Files Sent", user.getTotalFilesSent()),
				new UserStats("Total Files Received", user.getTotalFilesReceived()),
				new UserStats("Number of Virus Sends", user.getNumberOfVirusSends()));

		// Set the data for the stats table
		statsTable.setItems(statsList);

		// Create the scene and add it to the stage
		Scene scene = new Scene(statsTable, 400, 300);
		statsStage.setScene(scene);

		// Show the new stage
		statsStage.show();
	}

	// Helper class for User Stats
	public static class UserStats {
		private String statName;
		private Integer statValue;

		public UserStats(String statName, Integer statValue) {
			this.statName = statName;
			this.statValue = statValue;
		}

		public String getStatName() {
			return statName;
		}

		public Integer getStatValue() {
			return statValue;
		}
	}
}
