package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import check_virus.CheckVirus;
import util.Hasher;

/**
 * Manages file operations for the server, including file upload, download, 
 * removal, and collaboration. Handles file hashing for integrity verification 
 * and optional virus checking.
 */
public class FileManagerServer {
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private BufferedReader reader;
    private PrintWriter writer;

    private static final int BUFFER_SIZE = 65536; // Buffer size for file transfer 4KB 4096
    											  // 8 KB 8192;
    											  // 16 KB 16384;
    											  // 64 KB 65536;
    											  // 128 KB 131072;

    /**
     * Constructs a FileManager object with the necessary I/O streams.
     * 
     * @param dataInputStream  input stream for reading binary data
     * @param dataOutputStream output stream for sending binary data
     * @param reader           buffered reader for text input
     * @param writer           print writer for sending text output
     */
    public FileManagerServer(DataInputStream dataInputStream, DataOutputStream dataOutputStream, BufferedReader reader, PrintWriter writer) {
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Mimics a database by returning a map of hardcoded user credentials.
     * Hashes passwords and creates user directories if not already present.
     * 
     * @return a map of usernames to hashed passwords
     */
    public static Map<String, String> getUsers() {
        Map<String, String> users = new HashMap<>();
        // Sample user credentials
        users.put("admin", Hasher.hashPassword("admin"));
        createIfNotExist("admin");
        users.put("1", Hasher.hashPassword("1"));
        createIfNotExist("1");
        users.put("user1", Hasher.hashPassword("pass1"));
        createIfNotExist("user1");
        users.put("user2", Hasher.hashPassword("pass2"));
        createIfNotExist("user2");
        return users;
    }

    /**
     * Ensures that a directory exists, creating it if necessary.
     * 
     * @param pathDir the directory path to check or create
     */
    public static void createIfNotExist(String pathDir) {
        File folderDir = new File(pathDir);
        if (!folderDir.exists())
            folderDir.mkdirs();
    }

    /**
     * Reads a line of text using the BufferedReader.
     * 
     * @return the line read, or null if an error occurs
     */
    public String read() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("I/O error occurred while reading a line.");
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
        }
        return null;
    }

    /**
     * Sends a message to the client using the PrintWriter.
     * 
     * @param message the message to send
     */
    public void write(String message) {
        writer.println(message);
    }

    /**
     * while (currentSize < totalSize && (bytesRead = dataInputStream.read(buffer, 0, Math.min(buffer.length, (int)(totalSize - currentSize)))) != -1)
     * Handles the upload of a file from the client, verifying its integrity using SHA-256.
     *
     * @param dir      the directory to save the file in
     * @param fileName the name of the file being uploaded
     * @throws IOException if an error occurs during file transfer
     */
    public void receiveFile(File dir, String fileName, String username, SQLiteDatabaseHandler dbHandler, String visibility) throws IOException {
        File newFile = new File(dir, fileName);
        MessageDigest messageDigest;

        // Initialize the MessageDigest for SHA-256 hashing
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available.");
            write("Algo Of Hash Not Exist.");
            return;
        }

        if (newFile.exists()) {
            write("File Already Exist.");
            return;
        } else {
            write("Ready To Receive.");
        }

        long totalSize = dataInputStream.readLong(); // Read file size
        if (totalSize <= 0) {
            write("Invalid file size.");
            return;
        }

        long currentSize = 0;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(newFile, "rw")) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Loop to read file data in chunks
            while (currentSize < totalSize && (bytesRead = dataInputStream.read(buffer, 0, Math.min(buffer.length, (int)(totalSize - currentSize)))) != -1) {
                randomAccessFile.write(buffer, 0, bytesRead);
                messageDigest.update(buffer, 0, bytesRead); // Update the hash
                currentSize += bytesRead;

                // Optionally log progress
                if (currentSize % 10000 == 0) {
                    double progress = (double) currentSize / totalSize * 100;
                    System.out.printf("Received: %.2f%%%n", progress);
                }
            }
            
            
            // Compute the final hash
            String calculatedHash = Hasher.bytesToHex(messageDigest.digest());
            //write("File received. Hash: " + calculatedHash);
            System.out.println("File received. Hash: " + calculatedHash);
            
            dbHandler.updateFileTransferStats(username, true, 1); // Update sent files count
            if(visibility.equals("public")) {
            	dbHandler.logCommand(username, "UPLOAD", fileName, true, true);
            	dbHandler.addSharedFile(fileName, username);
            } else {
            	dbHandler.logCommand(username, "UPLOAD", fileName, false, true);
            }
            

            // Start asynchronous virus check
            new Thread(() -> {
                if (CheckVirus.isSafe(newFile)) {
                    System.out.println("File is safe.");
                } else {
                    System.err.println("File is infected!");
                    // Uncomment to delete infected files
                    // newFile.delete();
                    dbHandler.updateSentVirusesCount(username, 1);
                }
            }).start();

        } catch (IOException e) {
            write("Error during file reception.");
        }
    }


    /**
     * Sends a file to the client in chunks with hash-based integrity verification.
     *
     * @param dir      the directory containing the file
     * @param fileName the name of the file to send
     * @throws IOException if an error occurs during file transfer
     */
    public boolean sendFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        if (!file.exists()) {
            write("File Not Found.");
            return false;
        }

        write("Ready");
        long totalSize = file.length();
        dataOutputStream.writeLong(totalSize);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = randomAccessFile.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
                messageDigest.update(buffer, 0, bytesRead); // Update hash with chunk data
            }

            // Send the final file hash
            String finalHash = Hasher.bytesToHex(messageDigest.digest());
            dataOutputStream.writeUTF(finalHash);

            System.out.println("File sent successfully. Hash: " + finalHash);
            return true;
        } catch (Exception e) {
            System.err.println("Error during file transfer: " + e.getMessage());
            dataOutputStream.writeUTF("TRANSFER_FAILED " + e.getMessage());
            return false;
        }
    }
    
    
    /**
     * Sends a file to the client in packets, computing hashes for integrity checks.
     * 
     * @param dir               the directory containing the file
     * @param requestedFileName the name of the file to send
     * @return true if the file was sent successfully, false otherwise
     * @throws IOException if an error occurs during file transfer
     */
    /*public boolean sendFile(File dir, String requestedFileName) throws IOException {
        File file = new File(dir, requestedFileName);
        if (!file.exists()) {
            System.out.println("File Not Found");
            write("File Not Found.");
            return false;
        }

        write("Ready");
        System.out.println("File Found");
        long totalSize = file.length();
        dataOutputStream.writeLong(totalSize);
        System.out.println("size = " + totalSize);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];

            // Respond to client requests for packets
            while (true) {
                String clientRequest = dataInputStream.readUTF();

                if (clientRequest.startsWith("REQUEST_PACKET")) {
                    int packetIndex = Integer.parseInt(clientRequest.split(" ")[1]);
                    long offset = (long) packetIndex * BUFFER_SIZE;

                    if (offset < totalSize) {
                        int packetSize = (int) Math.min(BUFFER_SIZE, totalSize - offset);
                        randomAccessFile.seek(offset);
                        randomAccessFile.readFully(buffer, 0, packetSize);

                        // Compute hash for the packet
                        messageDigest.update(buffer, 0, packetSize);
                        String packetHash = Hasher.computeSHA256(buffer, 0, packetSize);

                        // Send packet and hash
                        dataOutputStream.write(buffer, 0, packetSize);
                        dataOutputStream.writeUTF(packetHash);
                    } else {
                        dataOutputStream.writeUTF("INVALID_PACKET_INDEX");
                    }
                } else if (clientRequest.equals("TRANSFER_COMPLETE")) {
                    byte[] fileHash = messageDigest.digest();
                    dataOutputStream.writeUTF(Hasher.bytesToHex(fileHash)); //Final HASH
                    break;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error during file transfer: " + e.getMessage());
            dataOutputStream.writeUTF("TRANSFER_FAILED " + e.getMessage());
            return false;
        }
    }*/


    /**
     * Removes a file from the specified folder if it exists.
     * 
     * @param folder the folder where the file is located
     * @param fileName the name of the file to remove
     * @return true if the file was successfully removed, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public boolean removeFile(File folder, String fileName) throws IOException {
        File file = new File(folder, fileName);
        if (file.exists()) {
            if (file.delete()) {
                write("Success");
                System.out.println("File removed successfully");
                return true; // Successfully removed the file
            } else {
                write("Error");
                System.out.println("Failed to remove the file");
                return false; // Failed to delete the file
            }
        } else {
            write("Not Found");
            System.out.println("File does not exist: " + file.getAbsolutePath());
            return false; // File does not exist
        }
    }
    
    /**
     * Searches for the presence of a file by name in collaboration files (.txt).
     * The search will go through all `.txt` files in the server directory to check if 
     * the given file name is listed in any of them.
     * 
     * @param fileName the name of the file to search for
     * @return the name of the file containing the file entry if found, or an empty string if not found
     */
    public String searchInCollaboration(String fileName) {
        File folder = new File(AppConst.PATH_SERVER);

        // Get all .txt files in the folder
        File[] textFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        System.out.println("Text files is null? " + (textFiles == null));
        
        if (textFiles != null) {
            System.out.println("Text files size: " + textFiles.length);
            for (File textFile : textFiles) {
                // Read each .txt file line by line
                System.out.println("Checking text file: " + textFile);
                
                try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().equals(fileName)) {
                            String whoHaveFile = textFile.getName();
                            
                            if (whoHaveFile.endsWith(".txt")) {
                                whoHaveFile = whoHaveFile.substring(0, whoHaveFile.lastIndexOf("."));
                            }
                            write("File Exists.");
                            System.out.println("File found in: " + whoHaveFile);
                            return whoHaveFile;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + textFile.getName());
                    // Handle or log the exception appropriately
                }
            }
        }

        // If the file name was not found in any .txt file
        write("File Doesn't Exist.");
        System.out.println("File Doesn't Exist.");
        return "";
    }

}
