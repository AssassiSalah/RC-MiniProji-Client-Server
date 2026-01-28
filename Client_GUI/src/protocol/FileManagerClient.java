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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import application.AppConst;
import application.Load_Interfaces;

import javafx.concurrent.Task;

public class FileManagerClient {

    private Communication communication_Manager;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private int BUFFER_SIZE = 8192;

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
     * @throws NoSuchAlgorithmException if SHA-256 is not supported.
     * @throws IOException              if an I/O error occurs during file download.
     */
    public void downloadFile(String fileName) throws NoSuchAlgorithmException, IOException {
        File downloadedFile = new File(AppConst.DEFAULT_DOWNLOAD_PATH, fileName);

        long totalSize = dataInputStream.readLong();

        // Using a background thread to download the file asynchronously
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws IOException, NoSuchAlgorithmException {
                System.out.println("start download");
                try (RandomAccessFile fileOut = new RandomAccessFile(downloadedFile, "rw")) {
                    fileOut.setLength(totalSize);

                    long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
                    long currentPacket = 0;

                    long startMillis = System.currentTimeMillis();
                    long endMillis = 0;
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

                            if (currentPacket % 100 == 0) {
                                communication_Manager.updateProgressTimer(currentPacket /1024, endMillis - startMillis);
                                System.out.println("Packet " + currentPacket + " verified and written.");
                            }
                        } else {
                            System.err.println("Hash mismatch for packet " + currentPacket + ". Retrying...");
                        }
                    }

                    dataOutputStream.writeUTF("TRANSFER_COMPLETE");
                    String finalHash = dataInputStream.readUTF();
                    System.out.println("Final file hash received: " + finalHash);

                    System.out.println("File downloaded successfully.");
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Download failed: " + e.getMessage());
                    dataOutputStream.writeUTF("DOWNLOAD_FAILED");
                }
                return null;
            }
        };

        new Thread(downloadTask).start();

        downloadTask.setOnSucceeded(event -> {
            System.out.println("Download Task completed successfully.");
        });

        downloadTask.setOnFailed(event -> {
            System.err.println("Download Task failed.");
        });
    }

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

        AtomicLong currentSize = new AtomicLong(0);

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("start to upload ");

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    int sequenceNumber = 0;

                    long startMillis = System.currentTimeMillis();
                    long endMillis = 0;
                    communication_Manager.startProgressTimer(totalSize);
                    while (currentSize.get() < totalSize) {
                        bytesRead = fileInputStream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }

                        dataOutputStream.write(buffer, 0, bytesRead);
                        messageDigest.update(buffer, 0, bytesRead);
                        currentSize.addAndGet(bytesRead);

                        endMillis = System.currentTimeMillis();

                        if (currentSize.get() % 10000 == 0) {
                            communication_Manager.updateProgressTimer(currentSize.get() / 1024, endMillis - startMillis);

                            double progress = (double) currentSize.get() / totalSize;
                            System.out.printf("Chunk %d uploaded: %.2f%%%n", ++sequenceNumber, progress * 100);
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

                    write("File uploaded. Hash: " + fileHash);

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
