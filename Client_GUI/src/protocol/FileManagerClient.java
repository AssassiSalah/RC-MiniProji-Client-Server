/**
 * The `FileManagerClient` class is responsible for managing file upload and download operations
 * between the client and server using a `Communication` interface.
 */
package protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import application.AppConst;
import application.Load_Interfaces;
import javafx.concurrent.Task;

public class FileManagerClient {

    private Communication communication_Manager;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private SecretKey sessionAESKey; // AES Key for the session
    private IvParameterSpec sessionIV;
    private int BUFFER_SIZE = 16384; //8192;

    /**
     * Constructs a new `FileManagerClient` object.
     *
     * @param communication_Manager the `Communication` instance to handle client-server communication.
     * @param dataInputStream       the input stream for reading data from the server.
     * @param dataOutputStream      the output stream for sending data to the server.
     */
    public FileManagerClient(Communication communication_Manager, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        this.communication_Manager = communication_Manager;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;

        File downloadDir = new File(AppConst.DEFAULT_DOWNLOAD_PATH);
        if (!downloadDir.exists())
            downloadDir.mkdirs();
    }

    public void setSessionAESKey(SecretKey sessionAESKey) {
		this.sessionAESKey = sessionAESKey;
	}

	public void setSessionIV(IvParameterSpec sessionIV) {
		this.sessionIV = sessionIV;
	}

	/**
     * Reads a message from the server using the communication manager.
     *
     * @return the message received from the server.
     */
    private String read() {
        return communication_Manager.read();
    }

    /**
     * Writes a message to the server using the communication manager.
     *
     * @param message the message to send to the server.
     */
    private void write(String message) {
        communication_Manager.write(message);
    }
 
    /**
     * Downloads a file from the server asynchronously, decrypts it, and verifies its integrity using SHA-256.
     *
     * @param fileName the name of the file to be downloaded.
     * @throws IOException              if an I/O error occurs during file download.
     * @throws NoSuchAlgorithmException if SHA-256 is not supported.
     */
    public void downloadFile(String fileName) throws IOException, NoSuchAlgorithmException {
        // Create a File object for the downloaded file at the default download path
        File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

        // Read the size of the encrypted file from the server
        long encryptedSize = dataInputStream.readLong();
        System.out.println("Encrypted size... " + encryptedSize);
        if (encryptedSize <= 0) {
            throw new IOException("Invalid encrypted size received.");
        }

        // Initialize SHA-256 for hashing
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        AtomicLong currentSize = new AtomicLong(0); // Tracks the number of bytes received

        // Asynchronous task for downloading and decrypting the file
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Starting download...");

                try (RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw")) {
                    byte[] buffer = new byte[BUFFER_SIZE]; // Buffer for reading encrypted data
                    int bytesRead;
                    long startMillis = System.currentTimeMillis(); // Track start time for progress updates

                    // Initialize AES cipher in decryption mode
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, sessionAESKey, sessionIV);

                    // Start the progress timer for the UI
                    communication_Manager.startProgressTimer(encryptedSize);

                    while (currentSize.get() < encryptedSize) {
                        // Read the next chunk of encrypted data
                        int remainingBytes = (int) Math.min(buffer.length, encryptedSize - currentSize.get());
                        bytesRead = dataInputStream.read(buffer, 0, remainingBytes);

                        // Handle unexpected end of stream
                        if (bytesRead == -1) {
                            throw new IOException("Unexpected end of stream before file completion.");
                        }

                        // Decrypt the chunk and write it to the file
                        byte[] decryptedChunk = cipher.update(buffer, 0, bytesRead);
                        if (decryptedChunk != null) {
                            fileOut.write(decryptedChunk);
                            messageDigest.update(decryptedChunk); // Update the hash with the decrypted data
                        }

                        // Update the size of encrypted data received
                        currentSize.addAndGet(bytesRead);

                        // Update progress every 10KB
                        if (currentSize.get() % 10_000 == 0) {
                            long elapsedMillis = System.currentTimeMillis() - startMillis;
                            communication_Manager.updateProgressTimer(currentSize.get() / 1024, elapsedMillis);
                            System.out.printf("Progress: %.2f%%%n", (double) currentSize.get() / encryptedSize * 100);
                        }
                    }

                    // Finalize decryption for any remaining bytes
                    byte[] finalDecryptedChunk = cipher.doFinal();
                    if (finalDecryptedChunk != null) {
                        fileOut.write(finalDecryptedChunk);
                        messageDigest.update(finalDecryptedChunk);
                    }

                    // Validate the file hash against the server's hash
                    String finalHash = dataInputStream.readUTF();
                    String calculatedHash = Hasher.bytesToHex(messageDigest.digest());
                    if (!finalHash.equals(calculatedHash)) {
                        throw new IOException("File hash mismatch! Expected: " + finalHash + ", Calculated: " + calculatedHash);
                    }

                    System.out.println("Download completed successfully. Hash verified.");
                } catch (IOException e) {
                    System.err.println("Download failed: " + e.getMessage());
                    throw e;
                } finally {
                    // Stop the progress timer
                    communication_Manager.stopCircleProgressD();
                }

                return null;
            }
        };

        // Run the download task in a separate thread
        new Thread(downloadTask).start();
    }
    
    /**
     * Uploads a file to the server asynchronously, encrypts it, and sends its hash for integrity verification.
     *
     * @param file      the file to be uploaded.
     * @param totalSize the total size of the file in bytes.
     * @throws IOException if an I/O error occurs during file upload.
     */
    public void uploadFile(File file, long totalSize) throws IOException {
        // Initialize SHA-256 for hashing
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 is not available.");
            return;
        }

        AtomicLong currentSize = new AtomicLong(0); // Tracks the number of bytes uploaded

        // Asynchronous task for uploading and encrypting the file
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Starting upload...");

                try (RandomAccessFile fileInput = new RandomAccessFile(file, "r");
                     ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[BUFFER_SIZE]; // Buffer for reading file data
                    int bytesRead;

                    // Initialize AES cipher in encryption mode
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, sessionAESKey, sessionIV);

                    // Start the progress timer for the UI
                    communication_Manager.startProgressTimer(totalSize);

                    while (currentSize.get() < totalSize) {
                        // Read the next chunk of file data
                        bytesRead = fileInput.read(buffer);
                        if (bytesRead == -1) {
                            break; // End of file
                        }

                        // Encrypt the chunk and write it to the encrypted stream
                        byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
                        if (encryptedChunk != null) {
                            encryptedStream.write(encryptedChunk);
                        }

                        // Update the hash with the original chunk
                        messageDigest.update(buffer, 0, bytesRead);

                        // Update the size of file data processed
                        currentSize.addAndGet(bytesRead);

                        // Update progress every 10KB
                        long elapsedMillis = System.currentTimeMillis();
                        if (currentSize.get() % 10_000 == 0) {
                            communication_Manager.updateProgressTimer(currentSize.get() / 1024, elapsedMillis);

                            double progress = (double) currentSize.get() / totalSize;
                            System.out.printf("Progress: %.2f%%%n", progress * 100);
                        }
                    }

                    // Finalize encryption for any remaining bytes
                    byte[] finalEncryptedChunk = cipher.doFinal();
                    if (finalEncryptedChunk != null) {
                        encryptedStream.write(finalEncryptedChunk);
                    }

                    // Send the size of the encrypted data to the server
                    byte[] encryptedData = encryptedStream.toByteArray();
                    long encryptedSize = encryptedData.length;
                    dataOutputStream.writeLong(encryptedSize);

                    // Send the encrypted data
                    dataOutputStream.write(encryptedData);

                    // Send the calculated hash to the server
                    String fileHash = Hasher.bytesToHex(messageDigest.digest());
                    dataOutputStream.writeUTF(fileHash);

                    System.out.println("File uploaded successfully. Hash: " + fileHash);
                } catch (Exception e) {
                    System.err.println("Upload failed: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Stop the progress timer
                    communication_Manager.stopCircleProgressU();
                }

                return null;
            }
        };

        // Run the upload task in a separate thread
        new Thread(uploadTask).start();
    }
    
    //// ANOTHER VERSION Done Before the main one OF Download/Upload methods without the Encription (confidentiality + AES for crypt algorithm)
    //// USING THE HASH AS A CHECK FOR ALL THE PACKETS if they are received correctly, by: Bekkari Abderahman & Assassi Salah Eddine
    import java.io.FileInputStream;
    import java.util.Arrays;

    /**
     * Downloads a file from the server asynchronously.
     *
     * @param fileName the name of the file to be downloaded.
     * @throws IOException              if an I/O error occurs during file download.
     * @throws NoSuchAlgorithmException if SHA-256 is not supported.
     */
    public void downloadFile_ParallelVersion(String fileName, boolean advance) throws IOException, NoSuchAlgorithmException {
        File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);
        
        write("Ready");

        // Read total file size from the server
        long totalSize = dataInputStream.readLong();
        System.out.println("Size... " + totalSize);
        if (totalSize <= 0) {
            throw new IOException("Invalid file size received.");
        }

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        // Asynchronous download task
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Starting download...");

                try (RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw")) {
                    fileOut.setLength(totalSize); // Preallocate space for the file

                    long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long currentSize = 0;
                    long startMillis = System.currentTimeMillis();
                    long currentPacket = 0;

                    communication_Manager.startProgressTimer(totalSize);

                    while (currentPacket < packetCount) {
                        dataOutputStream.writeUTF("REQUEST_PACKET " + currentPacket);

                        int bytesRead = dataInputStream.read(buffer);
                        if (bytesRead == -1) break;

                        byte[] packet = Arrays.copyOf(buffer, bytesRead);
                        String receivedHash = dataInputStream.readUTF();

                        String calculatedHash = Hasher.computeSHA256(packet);
                        if (receivedHash.equals(calculatedHash)) {
                            fileOut.seek(currentPacket * BUFFER_SIZE);
                            fileOut.write(packet);
                            currentPacket++;

                            // Update current size and progress
                            currentSize += bytesRead;
                            messageDigest.update(buffer, 0, bytesRead);


                            // Log progress every 10 KB
                            if (currentSize % 10_000 == 0) {
                                double progress = (double) currentSize / totalSize;
                                communication_Manager.updateProgressTimer(currentSize / 1024, System.currentTimeMillis() - startMillis);

                                System.out.printf("Packet %d downloaded: %.2f%%%n", currentPacket, progress * 100);
                            }
                        } else {
                            System.err.println("Hash mismatch for packet " + currentPacket + ". Retrying...");
                        }
                    }
                    dataOutputStream.writeUTF("TRANSFER_COMPLETE");

                    System.out.print("l");
                    // Validate file hash
                    String finalHash = dataInputStream.readUTF();
                    System.out.print("l");

                    String calculatedHash = Hasher.bytesToHex(messageDigest.digest());
                    System.out.print("l");


                    if (!finalHash.equals(calculatedHash)) {
                        throw new IOException("File hash mismatch! Expected: " + finalHash + ", Calculated: " + calculatedHash);
                    }

                    System.out.println("Download completed successfully. Hash verified.");
                } catch (IOException e) {
                    System.err.println("Download failed: " + e.getMessage());
                    throw e;
                }

                communication_Manager.stopCircleProgress(advance);
                return null;
            }
        };

        // Start download task in a new thread
        new Thread(downloadTask).start();
    }
    
    /**
     * Uploads a file to the server asynchronously.
     *
     * @param file      the file to be uploaded.
     * @param totalSize the total size of the file in bytes.
     * @throws IOException if an I/O error occurs during file upload.
     */
    public void uploadFile_ParallelVersion(File file, long totalSize) throws IOException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 is not available.");
            return;
        }

        dataOutputStream.writeLong(totalSize);

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("start to upload ");

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long currentSize = 0;
                    int currentPacket = 0; 
                    
                    long startMillis = System.currentTimeMillis();

                    communication_Manager.startProgressTimer(totalSize);
                    while (currentSize < totalSize) {
                        bytesRead = fileInputStream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }

                        dataOutputStream.write(buffer, 0, bytesRead);
                        messageDigest.update(buffer, 0, bytesRead);
                        currentSize += bytesRead;

                        if (++currentPacket % 500 == 0) {
                            communication_Manager.updateProgressTimer(currentSize / 1024, System.currentTimeMillis() - startMillis);

                            double progress = (double) currentSize / totalSize;
                            System.out.printf("Chunk %d uploaded: %.2f%%%n", currentPacket, progress * 100);
                        }
                    }

                    dataOutputStream.flush();

                    byte[] calculatedHash = messageDigest.digest();
                    StringBuilder hashBuilder = new StringBuilder();
                    for (byte b : calculatedHash) {
                        hashBuilder.append(String.format("%02x", b));
                    }

                    String fileHash = hashBuilder.toString();
                    System.out.println("Calculated Hash: " + fileHash);

                    //write("File uploaded. Hash: " + fileHash);
                    System.out.println("File uploaded. Hash: " + fileHash);

                    System.out.println("File uploaded successfully.");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                communication_Manager.stopCircleProgressU();
                return null;
            }
        };

        new Thread(uploadTask).start();
    }

    /**
     * Requests the server to remove a file.
     *
     * @param fileName the name of the file to be removed.
     */
    public void removeFileFromServer(String fileName) {
        write("REMOVE");
        write(fileName);

        String serverResponse = read();

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
