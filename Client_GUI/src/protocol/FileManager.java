package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import application.AppConst;
import application.Load_Interfaces;

import javafx.concurrent.Task;

public class FileManager {
	
	private Communication communication_Manager;

	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;

	private int BUFFER_SIZE = 8192;

	FileManager(Communication communication_Manager, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
		this.communication_Manager = communication_Manager;
		this.dataInputStream = dataInputStream;
		this.dataOutputStream = dataOutputStream;

		File downloadDir = new File(AppConst.DEFAULT_DOWNLOAD_PATH);
		if (!downloadDir.exists())
			downloadDir.mkdirs();
	}

	private String read() {
		return communication_Manager.read();
	}

	private void write(String message) {
		communication_Manager.write(message);
	}

	void downloadFile(String fileName) throws NoSuchAlgorithmException, IOException {

		File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

		long totalSize = dataInputStream.readLong();
		
		Load_Interfaces.startCircleProgress(totalSize);

		Task<Void> downloadTask = new Task<>() {
			@Override
			protected Void call() throws IOException, NoSuchAlgorithmException  { // FileNotFoundException, IOException

				long currentSize = 0;

				RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw");
				fileOut.setLength(totalSize); // Prepare file with correct size

				long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);

				System.out.println("Start The Reading");
				
				for(long currentPacket = 0; currentPacket < packetCount; currentPacket++) {
					dataOutputStream.writeUTF("REQUEST_PACKET " + currentPacket); // Request current packet

					byte[] buffer = new byte[BUFFER_SIZE];
					int bytesRead = dataInputStream.read(buffer);
					if (bytesRead == -1)
						break;

					// Log progress percentage
					System.out.printf("%.2f%% Downloaded%n", ((double) currentSize / totalSize) * 100);
					
					// Update progress
					// ----updateProgress(currentSize, totalSize);
					
					byte[] packet = Arrays.copyOf(buffer, bytesRead);
					String receivedHash = dataInputStream.readUTF(); // Receive packet hash

					String calculatedHash = Hasher.computeSHA256(packet);
					if (receivedHash.equals(calculatedHash)) {
						fileOut.seek(currentPacket * BUFFER_SIZE);
						fileOut.write(packet);
						
						if (currentPacket % 1000 == 0) {
							System.out.println("Packet " + currentPacket + " verified and written.");

						}
					} else {
						System.err.println("Hash mismatch for packet " + currentPacket + ". Retrying...");
					}
				}

				fileOut.close();
				return null;
			}
		};

		// Confirm transfer complete
		write("TRANSFER_COMPLETE");
		String finalHash = dataInputStream.readUTF();
		System.out.println("Final file hash received: " + finalHash);

		System.out.println("File downloaded successfully.");

		// Bind the progress property to the ProgressBar
		// progressBar.progressProperty().bind(downloadTask.progressProperty());

		// Run the task on a background thread
		new Thread(downloadTask).start();
	}

	void uploadFile(File file, long totalSize) throws IOException {

		// Using SHA-256 for hash calculation
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("SHA-256 is not exsit.");
			System.err.println("Cause : not set up java probably");
			return;
		}

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

					communication_Manager.startProgressTimer(totalSize);
					while (currentSize.get() < totalSize) {
				        long startMillis = System.currentTimeMillis();

						bytesRead = fileInputStream.read(buffer);
						// Check if the end of the file is reached
						if (bytesRead == -1) {
							break; // Exit the loop if end of file is reached
						}

						dataOutputStream.write(buffer, 0, bytesRead);
						messageDigest.update(buffer, 0, bytesRead); // Update hash with the current chunk
						currentSize.addAndGet(bytesRead); // Safely update currentSize

				        long endMillis = System.currentTimeMillis();
				        communication_Manager.updateProgressTimer(currentSize.get(), endMillis - startMillis);

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
					// Load_Interfaces.informationAlert("File uploaded successfully",
					// dataInputStream.readUTF());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Load_Interfaces.displayUpload();
				return null;
			}
		};

		// Bind the progress property to the ProgressBar
		// ---progressBar.progressProperty().bind(uploadTask.progressProperty());

		// Run the task on a background thread
		new Thread(uploadTask).start();
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
			Load_Interfaces.informationAlert("File Failed To Remove",
					"The File : " + fileName + ", Still Exist in The Server");
		}
	}
}
