package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import Hash.HashUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

public class FileManager_client {
	
	private final DataInputStream dataInputStream;
	private final DataOutputStream dataOutputStream;
	private TextArea logArea;
	private ProgressBar progressBar;
	private int BUFFER_SIZE =8192;
	public FileManager_client(DataInputStream dataInputStream, DataOutputStream dataOutputStream, TextArea logArea, ProgressBar progressBar) {
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
	
	public void downloadFile(String fileName, String destination) throws IOException, NoSuchAlgorithmException {
	    dataOutputStream.writeUTF("DOWNLOAD");
	    dataOutputStream.writeUTF(fileName);

	    String response = dataInputStream.readUTF();
	    if (response.contains("Not Found")) {
	        System.out.println("Error: " + response);
	        return;
	    }

	    long totalSize = dataInputStream.readLong(); // Read total file size
	    File downloadedFile = new File(destination, fileName);

	    try (RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw")) {
	        fileOut.setLength(totalSize); // Prepare file with correct size

	        long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
	        long currentPacket = 0;

	        while (currentPacket < packetCount) {
	            dataOutputStream.writeUTF("REQUEST_PACKET " + currentPacket); // Request current packet

	            byte[] buffer = new byte[BUFFER_SIZE];
	            int bytesRead = dataInputStream.read(buffer);
	            if (bytesRead == -1) break;

	            byte[] packet = Arrays.copyOf(buffer, bytesRead);
	            String receivedHash = dataInputStream.readUTF(); // Receive packet hash

	            String calculatedHash = HashUtil.computeSHA256(packet);
	            if (receivedHash.equals(calculatedHash)) {
	                fileOut.seek(currentPacket * BUFFER_SIZE);
	                fileOut.write(packet);
	                currentPacket++;
	                if(currentPacket%1000==0)
	                {
		                System.out.println("Packet " + currentPacket + " verified and written.");

	                }
	            } else {
	                System.err.println("Hash mismatch for packet " + currentPacket + ". Retrying...");
	            }
	        }

	        // Confirm transfer complete
	        dataOutputStream.writeUTF("TRANSFER_COMPLETE");
	        String finalHash = dataInputStream.readUTF();
	        System.out.println("Final file hash received: " + finalHash);

	        System.out.println("File downloaded successfully.");
	    } catch (IOException | NoSuchAlgorithmException e) {
	        System.err.println("Download failed: " + e.getMessage());
	        dataOutputStream.writeUTF("DOWNLOAD_FAILED");
	    }
	}

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return result;
    }

    // Helper method to convert byte array to hex string for readability
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
    public void uploadFile(String filePath) throws IOException, NoSuchAlgorithmException {
        dataOutputStream.writeUTF("UPLOAD");
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File Not Exist");
            return;
        }

        dataOutputStream.writeUTF(file.getName());

        String serverResponse = dataInputStream.readUTF();
        System.out.println(serverResponse);
        if (!serverResponse.contains("Ready")) {
            System.out.println("File Already Exists");
            return;
        }

        long totalSize = file.length();
        dataOutputStream.writeLong(totalSize);

        // Using SHA-256 for hash calculation
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        // Use AtomicLong to ensure thread-safe updates
        AtomicLong currentSize = new AtomicLong(0);

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    int sequenceNumber = 0; // Sequence number for each chunk

                    while (currentSize.get() < totalSize) {
                        bytesRead = fileInputStream.read(buffer);

                        if (bytesRead == -1) {
                            break; // Exit the loop if end of file is reached
                        }

                        dataOutputStream.write(buffer, 0, bytesRead); // Send the data chunk
                        messageDigest.update(buffer, 0, bytesRead); // Update hash with the current chunk
                        currentSize.addAndGet(bytesRead); // Safely update currentSize

                        // Optionally display the upload progress for one out of every 10,000 packets
                        if (currentSize.get() % 10000 == 0) {
                            double progress = (double) currentSize.get() / totalSize;
                            System.out.printf("Chunk %d uploaded: %.2f%%\n", ++sequenceNumber, progress * 100);
                        }
                    }

                    byte[] calculatedHash = messageDigest.digest(); // Final file hash

                    // Convert hash to hex
                    StringBuilder hashBuilder = new StringBuilder();
                    for (byte b : calculatedHash) {
                        hashBuilder.append(String.format("%02x", b));
                    }

                    String fileHash = hashBuilder.toString();
                    System.out.println("Calculated Hash: " + fileHash);

                    // Send hash back to server
                    dataOutputStream.writeUTF("File uploaded. Hash: " + fileHash);
                    dataOutputStream.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }
        };

        // Commented out progress bar binding line to disable GUI update
        // progressBar.progressProperty().bind(uploadTask.progressProperty());

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
