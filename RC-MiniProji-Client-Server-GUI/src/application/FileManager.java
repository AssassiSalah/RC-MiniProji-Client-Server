package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

public class FileManager {
	
	private final DataInputStream dataInputStream;
	private final DataOutputStream dataOutputStream;
	private TextArea logArea;
	private ProgressBar progressBar;

	public FileManager(DataInputStream dataInputStream, DataOutputStream dataOutputStream, TextArea logArea, ProgressBar progressBar) {
	     this.dataInputStream = dataInputStream;
	     this.dataOutputStream = dataOutputStream;
	     this.logArea = logArea;
	     this.progressBar = progressBar;
	}  
	
	@FXML
    static String importFile() {
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
            System.out.println("File selected: " + filePath);
            return filePath;
        } else {
            // Handle case when no file is selected
            System.out.println("No file selected.");
            return "";
        }
    }
	
	void downloadFile(String fileName, String destination) throws IOException {
		
    	dataOutputStream.writeUTF("DOWNLOAD");
    	
        dataOutputStream.writeUTF(fileName);

        String response = dataInputStream.readUTF();
        System.out.println(response);
        
        if (response.contains("Not Found")) {
            System.out.println("Error: " + response);
            return;
        }

        File downloadedFile = new File(destination + "/" + fileName);
        
        long totalSize = dataInputStream.readLong();//Long.parseLong();
        progressBar.setProgress(0); // Reset progress bar
        
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
            	            	
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
	
    void uploadFile(String filePath) throws IOException {
    	
		dataOutputStream.writeUTF("UPLOAD");
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
        dataOutputStream.writeLong(totalSize);
        
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
            	
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
	
    /**
     * Sends a request to the server to remove a file.
     *
     * @param fileName the name of the file to be removed from the server
	 * @throws IOException 
     */
    void removeFileFromServer(String fileName) throws IOException {
    	dataOutputStream.writeUTF("REMOVE");
    	System.out.println("Name Of File : "+ fileName);
    	
        try {
            // Send the file name to be removed
            dataOutputStream.writeUTF(fileName);

            // Receive the server's response
            String serverResponse = dataInputStream.readUTF();

            // Log the server's response
            logArea.appendText("Server response: " + serverResponse + "\n");

            // Handle the server's response
            if (serverResponse.contains("Success")) {
                logArea.appendText("File removed successfully: " + fileName + "\n");
            } else if (serverResponse.contains("Not Found")) {
                logArea.appendText("File not found on server: " + fileName + "\n");
            } else {
                logArea.appendText("Failed to remove file: " + fileName + "\n");
            }
        } catch (IOException e) {
            // Handle errors during communication
            logArea.appendText("Error removing file from server: " + e.getMessage() + "\n");
        }
    }

}
