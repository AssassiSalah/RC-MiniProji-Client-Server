package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

public class HistoryController {

    @FXML
    private TableView<Record> tableView;

    @FXML
    private TableColumn<Record, String> commandColumn;

    @FXML
    private TableColumn<Record, String> fileNameColumn;

    @FXML
    private TableColumn<Record, String> visibilityColumn;

    @FXML
    private TableColumn<Record, String> timeColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> filterChoiceBox;

    private final ObservableList<Record> data = FXCollections.observableArrayList();
    private final FilteredList<Record> filteredData = new FilteredList<>(data);
    
    private static String nameFile = "History_Client.txt";

    @FXML
    public void initialize() {
        // Link table columns with the Record class properties
        commandColumn.setCellValueFactory(new PropertyValueFactory<>("command"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        visibilityColumn.setCellValueFactory(new PropertyValueFactory<>("visibility"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        // Bind the FilteredList to the TableView
        tableView.setItems(filteredData);

        // Initialize ChoiceBox with filtering options
        filterChoiceBox.setItems(FXCollections.observableArrayList("All", "Command", "File Name", "Visibility"));
        filterChoiceBox.setValue("All"); // Default selection

        // Add listeners for search bar and choice box
        setupSearchAndFilter();
        
        // Load initial data
        readFromFile();
        System.out.println("History Is Ready");
    }

    private void setupSearchAndFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        filterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String filterType = filterChoiceBox.getValue();

        filteredData.setPredicate(createFilterPredicate(searchText, filterType));
    }

    private Predicate<Record> createFilterPredicate(String searchText, String filterType) {
        if (searchText == null || searchText.isEmpty() || "All".equals(filterType)) {
            return record -> true; // No filtering
        }

        return record -> {
            switch (filterType) {
                case "Command":
                    return record.getCommand().toLowerCase().contains(searchText);
                case "File Name":
                    return record.getFileName().toLowerCase().contains(searchText);
                case "Visibility":
                    return record.getVisibility().toLowerCase().contains(searchText);
                default:
                    return false;
            }
        };
    }

    /**
     * Reads data from TODO and refreshes the TableView.
     */
    public void readFromFile() {
        data.clear();
        File file = new File(nameFile);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 4) {
                        data.add(new Record(parts[0], parts[1], parts[2], parts[3]));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Refresh the TableView
        applyFilters();
    }

    /**
     * Writes data to ___ TODO from the TableView.
     */
    public void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nameFile))) {
            for (Record record : data) {
                writer.write(String.join("|", record.getCommand(), record.getFileName(),
                        record.getVisibility(), record.getTime()));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void appendToFile(Record record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nameFile, true))) { // true for append mode
            writer.write(String.join("|", record.getCommand(), record.getFileName(),
                    record.getVisibility(), record.getTime()));
            writer.newLine(); // Add a new line after writing the record
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class representing each record in the TableView
    public static class Record {
        private final String command;
        private final String fileName;
        private final String visibility;
        private final String time;

        public Record(String command, String fileName, String visibility, String time) {
            this.command = command;
            this.fileName = fileName;
            this.visibility = visibility;
            this.time = time;
        }
        
        public Record(String command, String fileName, String visibility) {
        	this.command = command;
            this.fileName = fileName;
            this.visibility = visibility;
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")).toString();
        }

        public Record(String command, String fileName) {
        	this.command = command;
            this.fileName = fileName;
            this.visibility = "//";
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")).toString();
		}

		public Record(String command) {
			this.command = command;
            this.fileName = "//";
            this.visibility = "//";
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")).toString();
		}

		public String getCommand() {
            return command;
        }

        public String getFileName() {
            return fileName;
        }

        public String getVisibility() {
            return visibility;
        }

        public String getTime() {
            return time;
        }
    }
}
