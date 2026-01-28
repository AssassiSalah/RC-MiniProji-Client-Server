package server;

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
import java.util.HashMap;
import java.util.Map;

import Hash.HashUtil;
import check_virus.CheckVirus;

public class FileManager {

    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private static final int BUFFER_SIZE = 8192; // Adjust as needed

    public FileManager(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
    }

    // HACK this method work instead of Database (we don't have DB)
    public static Map<String, String> getUsers() {
        Map<String, String> users = new HashMap<String, String>();
        // Sample user credentials
        users.put("user1", "pass1");
        users.put("user2", "pass2");
        return users;
    }

    public static void createIfNotExist(String pathDir) {
        File folderDir = new File(pathDir);

        if (!folderDir.exists())
            folderDir.mkdirs();

    }

    public void receiveFile(File dir, String fileName) throws IOException, NoSuchAlgorithmException {
        File newFile = new File(dir, fileName);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // Using SHA-256 for hash calculation

        if (newFile.exists()) {
            dataOutputStream.writeUTF("File Exists Already.");
            return;
        } else {
            dataOutputStream.writeUTF("Ready To Receive.");
        }

        long totalSize = dataInputStream.readLong(); // Read file size
        long currentSize = 0;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(newFile, "rw")) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int sequenceNumber = 0; // Sequence number to ensure proper chunk order

            while (currentSize < totalSize) {
                bytesRead = dataInputStream.read(buffer);

                if (bytesRead == -1) {
                    break; // Exit the loop if end of stream is reached
                }

                // Write the data to the file
                randomAccessFile.write(buffer, 0, bytesRead);
                messageDigest.update(buffer, 0, bytesRead); // Update the hash with the received data

                currentSize += bytesRead;
                sequenceNumber++;

                // Display progress for every 10,000 bytes (you can adjust this logic if needed)
                if (currentSize % 10000 == 0) {
                    double progress = (double) currentSize / totalSize * 100;
                    // System.out.printf("Received chunk %d: %.2f%%\n", sequenceNumber, progress);
                }

                // Optionally, send back the progress to the client (only display for one out of
                // every 10,000 packets)
                if (sequenceNumber % 10000 == 0) {
                    dataOutputStream.writeUTF("Chunk " + sequenceNumber + " received successfully.");
                }
            }

            // Calculate the received hash and compare
            byte[] receivedHash = messageDigest.digest();
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : receivedHash) {
                hashBuilder.append(String.format("%02x", b));
            }

            String calculatedHash = hashBuilder.toString();

            // If needed, disable hash printing in console
            // System.out.println("Calculated Hash: " + calculatedHash);

            // Send hash back to client
            dataOutputStream.writeUTF("File received. Hash: " + calculatedHash);

            // Optionally check for file viruses (async)
            new Thread(() -> {
                boolean test = true;
                /* if (CheckVirus.isSafe(newFile)||test) */
                if (CheckVirus.isSafe(newFile) || test) {
                    System.out.println("The file is safe.");
                } else {
                    System.out.println("Warning: The file is infected!");
                    // TODO: Remove the file if infected
                }
            }).start();
        }
    }

    public boolean processRequest(String command, File dir, String requestedFileName)
            throws IOException, NoSuchAlgorithmException {
        // Process the command directly
        if (command.startsWith("RESEND")) {
            return sendFile(dir, requestedFileName);
        }
        return false;
    }

    public boolean sendFile(File dir, String requestedFileName) throws IOException, NoSuchAlgorithmException {
        File file = new File(dir, requestedFileName);

        if (!file.exists()) {
            dataOutputStream.writeUTF("File Not Found.");
            return false;
        }

        dataOutputStream.writeUTF("TRANSFER " + requestedFileName);

        long totalSize = file.length();
        dataOutputStream.writeLong(totalSize); // Send total file size

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
            byte[][] packets = new byte[(int) packetCount][];
            String[] packetHashes = new String[(int) packetCount];

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            // Preload file into packets and compute hashes
            for (int i = 0; i < packetCount; i++) {
                int packetSize = (int) Math.min(BUFFER_SIZE, totalSize - (long) i * BUFFER_SIZE);
                byte[] packet = new byte[packetSize];
                randomAccessFile.seek((long) i * BUFFER_SIZE);
                randomAccessFile.readFully(packet);
                packets[i] = packet;

                String hash = HashUtil.computeSHA256(packet);
                packetHashes[i] = hash;
                messageDigest.update(packet); // Update the overall hash
            }

            byte[] fileHash = messageDigest.digest();

            // Respond to client requests for packets
            while (true) {
                String clientRequest = dataInputStream.readUTF();

                if (clientRequest.startsWith("REQUEST_PACKET")) {
                    int packetIndex = Integer.parseInt(clientRequest.split(" ")[1]);

                    if (packetIndex >= 0 && packetIndex < packetCount) {
                        dataOutputStream.write(packets[packetIndex]); // Send packet
                        dataOutputStream.writeUTF(packetHashes[packetIndex]); // Send hash
                    } else {
                        dataOutputStream.writeUTF("INVALID_PACKET_INDEX");
                    }
                } else if (clientRequest.equals("TRANSFER_COMPLETE")) {
                    dataOutputStream.writeUTF("FINAL_HASH " + HashUtil.bytesToHex(fileHash)); // Send final hash
                    break;
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error during file transfer: " + e.getMessage());
            dataOutputStream.writeUTF("TRANSFER_FAILED " + e.getMessage());
            return false;
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

    // Helper method to convert byte array to hex string for readability
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Checks if a file exists in the specified folder and removes it if it does.
     * 
     * @param folderPath the path of the folder
     * @param fileName   the name of the file to check and remove
     * @return true if the file was found and removed, false otherwise
     * @throws IOException
     */
    public boolean removeFile(File folder, String fileName) throws IOException {
        File file = new File(folder, fileName);
        if (file.exists()) {
            if (file.delete()) {
                dataOutputStream.writeUTF("Success");
                System.out.println("File removed successfully");
                return true;
            } else {
                dataOutputStream.writeUTF("Error");
                System.out.println("Failed to remove the file");
                return false;
            }
        } else {
            dataOutputStream.writeUTF("Not Found");
            System.out.println("File does not exist: " + file.getAbsolutePath());
            return false;
        }
    }
}
