package model;

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

import application.AppConst;
import application.Load_Interfaces;
import application.Main;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;

public class FileManager {

	private final DataInputStream dataInputStream;
	private final DataOutputStream dataOutputStream;

	@SuppressWarnings("unused")
	private ProgressBar progressBar;
	
	private int BUFFER_SIZE =8192;


	FileManager(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
		this.dataInputStream = dataInputStream;
		this.dataOutputStream = dataOutputStream;

		this.progressBar = null;

		File downloadDir = new File(AppConst.DEFAULT_DOWNLOAD_PATH);
		if (!downloadDir.exists())
			downloadDir.mkdirs();
	}

	public void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
	}
	
	private String read() {
		return Main.communication_Manager.read();
    }
	
	private void write(String message) {
		Main.communication_Manager.write(message);
	}

	void downloadFile(String fileName) throws IOException {
		
		write("DOWNLOAD");

		write(fileName);

		String response = read();
		System.out.println(response);

		if (!response.contains("Ready")) {
			if(response.contains("Algo"))
				System.out.println("Algo Not Exist.");
			else
				System.out.println("File Already Exist");
			return;
		}

		File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

		long totalSize = dataInputStream.readLong();// Long.parseLong();
		// progressBar.setProgress(0); // Reset progress bar

		//TODO check this cause i paste abdou code without checking
		Task<Void> downloadTask = new Task<>() {
			@Override
			protected Void call() throws Exception {

				long currentSize = 0;

				// System.out.println("start to Download ");
				try (FileOutputStream fileOut = new FileOutputStream(downloadedFile)) {
					byte[] buffer = new byte[4096];
					int bytesRead;

					System.out.println("Start The Reading");

					// Read file data until the end of the stream
					for (; currentSize < totalSize; currentSize += bytesRead) {

						// Log progress percentage
						System.out.printf("%.2f%% Downloaded%n", ((double) currentSize / totalSize) * 100);

						// Update progress
						// ----updateProgress(currentSize, totalSize);

						bytesRead = dataInputStream.read(buffer);

						// Check if the end of the file is reached
						if (bytesRead == -1) {
							break; // Exit the loop
						}

						// System.out.println(bytesRead);
						fileOut.write(buffer, 0, bytesRead);
					}
					System.out.println("File Download Successfully.");
				} catch (Exception e) {
					throw e;
				}
				return null;
			}
		};

		//System.out.println("File downloaded successfully.");
		//System.out.println();

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

	            String calculatedHash = HashUtilAbdou.computeSHA256(packet);
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
		
		// Bind the progress property to the ProgressBar
		// progressBar.progressProperty().bind(downloadTask.progressProperty());

		// Run the task on a background thread
		//new Thread(downloadTask).start();

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
	
	void uploadFile(String filePath) throws IOException {

		write("UPLOAD");
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("File Not Exist");
			return;
		}

		write(file.getName());

		String serverResponse = read();
		System.out.println(serverResponse);
		if (!serverResponse.contains("Ready")) {
			if(serverResponse.contains("Algo"))
				System.out.println("Algo Not Exist.");
			else
				System.out.println("File Already Exist");
			return;
		}
		
        // Using SHA-256 for hash calculation
		MessageDigest messageDigest;
        try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("SHA-256 is not exsit.");
			System.err.println("Cause : not set up java probably");
			return;
		}

		long totalSize = file.length();
		dataOutputStream.writeLong(totalSize);
        
        // Use AtomicLong to ensure thread-safe updates
        AtomicLong currentSize = new AtomicLong(0);
        
		Task<Void> uploadTask = new Task<>() {
			@Override
			protected Void call() throws Exception {

				System.out.println("start to upload ");
				
				try (FileInputStream fileInputStream = new FileInputStream(file)) {
					byte[] buffer = new byte[4096];
					int bytesRead;
                    int sequenceNumber = 0; // Sequence number for each chunk

					while(currentSize.get() < totalSize) {
						
						// Log progress percentage
						//System.out.printf("%.2f%% Uploaded%n", ((double) currentSize.get() / totalSize) * 100);
						
						bytesRead = fileInputStream.read(buffer);
						// Check if the end of the file is reached
						if (bytesRead == -1) {
							break; // Exit the loop if end of file is reached
						}
						
						dataOutputStream.write(buffer, 0, bytesRead);
						messageDigest.update(buffer, 0, bytesRead); // Update hash with the current chunk
                        currentSize.addAndGet(bytesRead); // Safely update currentSize

						// Update progress
						//---updateProgress(currentSize, totalSize);	
                        
                        // Optionally display the upload progress for one out of every 10,000 packets
                        if (currentSize.get() % 10000 == 0) {
                            double progress = (double) currentSize.get() / totalSize;
                            System.out.printf("Chunk %d uploaded: %.2f%%\n", ++sequenceNumber, progress * 100);
                        }
					}
					
					dataOutputStream.flush();

                    byte[] calculatedHash = messageDigest.digest(); // Final file hash

                    // Convert hash to hex
                    StringBuilder hashBuilder = new StringBuilder();
                    for (byte b : calculatedHash) {
                        hashBuilder.append(String.format("%02x", b));
                    }

                    String fileHash = hashBuilder.toString();
                    System.out.println("Calculated Hash: " + fileHash);

                    // Send hash back to server
                    write("File uploaded. Hash: " + fileHash);
                    

					System.out.println("File uploaded successfully.");
					// Load_Interfaces.informationAlert("File uploaded successfully", dataInputStream.readUTF());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};

		// Bind the progress property to the ProgressBar
		//---progressBar.progressProperty().bind(uploadTask.progressProperty());

		// Run the task on a background thread
		new Thread(uploadTask).start();
	}

	public void advDownloadFile(String fileName) throws IOException {
		write("ADVANCE_DOWNLOAD");
		write(fileName);
		
		String response = read();
		
		if(response.equals("File Exist."))
			downloadFile(fileName);
	}
	
	/**
	 * Sends a request to the server to remove a file.
	 *
	 * @param fileName the name of the file to be removed from the server
	 * @throws IOException
	 */
	void removeFileFromServer(String fileName) {
		write("REMOVE");
		// Send the file name to be removed
		write(fileName);

		// Receive the server's response
		String serverResponse = read();

		// Handle the server's response
		if (serverResponse.contains("Success")) {
			Load_Interfaces.informationAlert("File removed successfully",
					"The File : " + fileName + ", Has Been Removed");
		} else if (serverResponse.contains("Not Found")) {
			Load_Interfaces.informationAlert("File Not Found", "The File : " + fileName + ", Not Exist in The Server");
		} else {
			Load_Interfaces.informationAlert("File Failed To Remove", "The File : " + fileName + ", Still Exist in The Server");
		}
	}
}
