/**
 * The `FileManagerClient` class is responsible for managing file upload and download operations
 * between the client and server using a `Communication` interface.
 */
package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import application.AppConst;
import application.Load_Interfaces;
import javafx.concurrent.Task;

public class FileManagerClient {

    private Communication communication_Manager;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
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
     * Downloads a file from the server asynchronously.
     *
     * @param fileName the name of the file to be downloaded.
     * @throws IOException              if an I/O error occurs during file download.
     * @throws NoSuchAlgorithmException if SHA-256 is not supported.
     */
    public void downloadFile(String fileName, boolean advance) throws IOException, NoSuchAlgorithmException {
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

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long currentSize = 0;
                    long startMillis = System.currentTimeMillis();

                    communication_Manager.startProgressTimer(totalSize);

                    while (currentSize < totalSize) {
                        // Adjust buffer size for the last chunk
                        int remainingBytes = (int) Math.min(buffer.length, totalSize - currentSize);
                        bytesRead = dataInputStream.read(buffer, 0, remainingBytes);

                        if (bytesRead == -1) {
                            throw new IOException("Unexpected end of stream.");
                        }

                        // Write to file and update hash
                        fileOut.write(buffer, 0, bytesRead);
                        messageDigest.update(buffer, 0, bytesRead);
                        currentSize += bytesRead;

                        // Log progress
                        if (currentSize % 10_000 == 0) {
                            double progress = (double) currentSize / totalSize;
                            long elapsedMillis = System.currentTimeMillis() - startMillis;
                            communication_Manager.updateProgressTimer(currentSize / 1024, elapsedMillis);

                            System.out.printf("Downloaded %.2f%%%n", progress * 100);
                        }
                    }

                    // Validate file hash
                    String finalHash = dataInputStream.readUTF();
                    String calculatedHash = Hasher.bytesToHex(messageDigest.digest());

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
     * Downloads a file from the server asynchronously.
     *
     * @param fileName the name of the file to be downloaded.
     * @throws NoSuchAlgorithmException if SHA-256 is not supported.
     * @throws IOException              if an I/O error occurs during file download.
     */
    /*public void downloadFile(String fileName) throws NoSuchAlgorithmException, IOException {
        File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

        long totalSize = dataInputStream.readLong();

        // Task for downloading the file asynchronously
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws IOException, NoSuchAlgorithmException {
                System.out.println("Starting download...");

                MessageDigest messageDigest;
                try {
                    messageDigest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("SHA-256 is not available.");
                    throw e;
                }

                AtomicLong currentSize = new AtomicLong(0);

                try (RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw")) {
                    fileOut.setLength(totalSize);

                    long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
                    long currentPacket = 0;

                    long startMillis = System.currentTimeMillis();
                    long endMillis;

                    communication_Manager.startProgressTimer(totalSize);

                    while (currentPacket < packetCount) {
                        dataOutputStream.writeUTF("REQUEST_PACKET " + currentPacket);

                        byte[] buffer = new byte[BUFFER_SIZE];
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
                            currentSize.addAndGet(bytesRead);
                            messageDigest.update(buffer, 0, bytesRead);

                            endMillis = System.currentTimeMillis();

                            // Log progress every 10 KB
                            if (currentSize.get() % 10_000 == 0) {
                                double progress = (double) currentSize.get() / totalSize;
                                communication_Manager.updateProgressTimer(currentSize.get() / 1024, endMillis - startMillis);

                                System.out.printf("Packet %d downloaded: %.2f%%%n", currentPacket, progress * 100);
                            }
                        } else {
                            System.err.println("Hash mismatch for packet " + currentPacket + ". Retrying...");
                        }
                    }

                    dataOutputStream.writeUTF("TRANSFER_COMPLETE");
                    String finalHash = dataInputStream.readUTF();
                    System.out.println("Final file hash received: " + finalHash);

                    // Verify final hash
                    byte[] calculatedHash = messageDigest.digest();
                    String calculatedFileHash = Hasher.bytesToHex(calculatedHash);

                    if (!calculatedFileHash.equals(finalHash)) {
                        System.err.println("File hash mismatch! Expected: " + finalHash + ", Calculated: " + calculatedFileHash);
                    }

                    System.out.println("File downloaded successfully.");
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Download failed: " + e.getMessage());
                    dataOutputStream.writeUTF("DOWNLOAD_FAILED");
                    throw e;
                }
                return null;
            }
        };

        Platform.runLater(() -> {
            downloadTask.setOnSucceeded(event -> {
                System.out.println("Download Task completed successfully.");
                communication_Manager.stopCircleProgressD();
            });

            downloadTask.setOnFailed(event -> {
                System.err.println("Download Task failed.");
                communication_Manager.stopCircleProgressD();
            });

            new Thread(downloadTask).start();
        });
    }*/
    


    /**
     * Uploads a file to the server asynchronously.
     *
     * @param file      the file to be uploaded.
     * @param totalSize the total size of the file in bytes.
     * @throws IOException if an I/O error occurs during file upload.
     */
    public void uploadFile(File file, long totalSize) throws IOException {
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
