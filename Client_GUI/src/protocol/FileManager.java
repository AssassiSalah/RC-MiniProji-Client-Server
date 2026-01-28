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
import javafx.stage.FileChooser;

public class FileManager {

	private Communication communication_Manager;

	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;

	private int BUFFER_SIZE = 8192;

	public FileManager(Communication communication_Manager, DataInputStream dataInputStream,
			DataOutputStream dataOutputStream) {
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

	/**
	 * Opens a file chooser dialog and returns the selected file path.
	 * 
	 * @return the absolute path of the selected file, or empty string if no file
	 *         was selected
	 */
	public static String importFile() {
		// Create a FileChooser instance
		FileChooser fileChooser = new FileChooser();

		// Set an optional title for the file chooser dialog
		fileChooser.setTitle("Select a File to Import");

		// Set an initial directory
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

		// Add extension filters to limit file types
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("All Files", "*.*"),
				new FileChooser.ExtensionFilter("Text Files", "*.txt"),
				new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

		// Show the open dialog and get the selected file
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			String filePath = selectedFile.getAbsolutePath();
			System.out.println("File selected: " + filePath);
			return filePath;
		} else {
			System.out.println("No file selected.");
			return "";
		}
	}

	/**
	 * Downloads a file from the server.
	 * 
	 * @param fileName the name of the file to download
	 */
	public void downloadFile(String fileName) throws NoSuchAlgorithmException, IOException {

		File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

		long totalSize = dataInputStream.readLong();

		Load_Interfaces.startCircleProgress(totalSize);

		Task<Void> downloadTask = new Task<>() {
			@Override
			protected Void call() throws IOException, NoSuchAlgorithmException {

				long currentSize = 0;

				RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw");
				fileOut.setLength(totalSize); // Prepare file with correct size

				long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);

				System.out.println("Start The Reading");

				for (long currentPacket = 0; currentPacket < packetCount; currentPacket++) {
					dataOutputStream.writeUTF("REQUEST_PACKET " + currentPacket); // Request current packet

					byte[] buffer = new byte[BUFFER_SIZE];
					int bytesRead = dataInputStream.read(buffer);
					if (bytesRead == -1)
						break;

					// Log progress percentage
					System.out.printf("%.2f%% Downloaded%n", ((double) currentSize / totalSize) * 100);

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

		// Run the task on a background thread
		new Thread(downloadTask).start();
	}

	/**
	 * Uploads a file to the server.
	 * 
	 * @param file      the file to upload
	 * @param totalSize the total size of the file in bytes
	 */
	public void uploadFile(File file, long totalSize) throws IOException {

		// Using SHA-256 for hash calculation
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("SHA-256 is not exist.");
			System.err.println("Cause: Java not set up properly");
			return;
		}

		dataOutputStream.writeLong(totalSize);

		// Use AtomicLong to ensure thread-safe updates
		AtomicLong currentSize = new AtomicLong(0);

		Task<Void> uploadTask = new Task<>() {
			@Override
			protected Void call() throws Exception {

				System.out.println("Starting upload...");

				try (FileInputStream fileInputStream = new FileInputStream(file)) {
					byte[] buffer = new byte[4096];
					int bytesRead;
					int sequenceNumber = 0;
					long startMillis = System.currentTimeMillis();
					long endMillis = 0;
					communication_Manager.startProgressTimer(totalSize);

					while (currentSize.get() < totalSize) {

						bytesRead = fileInputStream.read(buffer);
						// Check if the end of the file is reached
						if (bytesRead == -1) {
							break;
						}

						dataOutputStream.write(buffer, 0, bytesRead);
						messageDigest.update(buffer, 0, bytesRead);
						currentSize.addAndGet(bytesRead);

						endMillis = System.currentTimeMillis();

						// Display upload progress every 10,000 bytes
						if (currentSize.get() % 10000 == 0) {
							communication_Manager.updateProgressTimer(currentSize.get() / 1024,
									(endMillis - startMillis));

							double progress = (double) currentSize.get() / totalSize;
							System.out.printf("Chunk %d uploaded: %.2f%%\n", ++sequenceNumber, progress * 100);
						}
					}

					dataOutputStream.flush();

					byte[] calculatedHash = messageDigest.digest();

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
				} catch (Exception e) {
					e.printStackTrace();
				}

				Load_Interfaces.displayUpload();
				return null;
			}
		};

		// Run the task on a background thread
		new Thread(uploadTask).start();
	}

	/**
	 * Sends a request to the server to remove a file.
	 * 
	 * @param fileName the name of the file to be removed from the server
	 */
	public void removeFileFromServer(String fileName) {
		write("REMOVE");
		// Send the file name to be removed
		write(fileName);

		// Receive the server's response
		String serverResponse = read();

		// Handle the server's response
		if (serverResponse.contains("Success")) {
			Load_Interfaces.informationAlert("File removed successfully",
					"The File: " + fileName + ", Has Been Removed");
		} else if (serverResponse.contains("Not Found")) {
			Load_Interfaces.informationAlert("File Not Found", "The File: " + fileName + ", Not Exist in The Server");
		} else {
			Load_Interfaces.informationAlert("File Failed To Remove",
					"The File: " + fileName + ", Still Exist in The Server");
		}
	}

	// Helper method to convert hex string to byte array
	private byte[] hexToBytes(String hex) {
		int len = hex.length();
		byte[] result = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return result;
	}

	// Helper method to convert byte array to hex string
	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			hexString.append(String.format("%02x", b));
		}
		return hexString.toString();
	}
}
