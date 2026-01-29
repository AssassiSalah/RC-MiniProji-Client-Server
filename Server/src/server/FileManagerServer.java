package server;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

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

    private static final int BUFFER_SIZE = 4096; // Buffer size for file transfer 4KB 4096
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
     * Receives an encrypted file from a client, decrypts it using AES, verifies its integrity using SHA-256, 
     * and stores it on the server. This method also performs an asynchronous virus scan on the received file.
     *
     * @param dir          the directory where the received file will be saved
     * @param fileName     the name of the file being uploaded
     * @param username     the name of the user uploading the file
     * @param dbHandler    a handler to interact with the database for logging and updates
     * @param visibility   the visibility of the file (e.g., "public" or "private")
     * @param sessionAESKey the AES key used for decryption
     * @param sessionIV    the initialization vector (IV) used for AES decryption
     * @throws IOException if an I/O error occurs during file reception
     * @throws GeneralSecurityException if an error occurs during decryption or hash computation
     */
    public void receiveFile(File dir, String fileName, String username, SQLiteDatabaseHandler dbHandler, String visibility, SecretKey sessionAESKey, IvParameterSpec sessionIV) throws IOException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        // Define the file path and prepare the SHA-256 message digest
        File newFile = new File(dir, fileName);
        MessageDigest messageDigest;

        // Initialize the SHA-256 algorithm
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available.");
            write("Algorithm of hash not available.");
            return;
        }

        // Check if the file already exists
        if (newFile.exists()) {
            write("File already exists.");
            return;
        } else {
            write("Ready to receive.");
        }

        // Read the total size of the encrypted file
        long encryptedSize = dataInputStream.readLong();
        if (encryptedSize <= 0) {
            write("Invalid encrypted file size.");
            return;
        }

        long receivedSize = 0; // Track how much data has been received

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(newFile, "rw")) {
            byte[] buffer = new byte[BUFFER_SIZE]; // Buffer for reading data
            int bytesRead;

            // Initialize the AES cipher in decryption mode
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sessionAESKey, sessionIV);

            // Read and decrypt data in chunks
            while (receivedSize < encryptedSize) {
                bytesRead = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, encryptedSize - receivedSize));
                if (bytesRead == -1) {
                    break;
                }

                receivedSize += bytesRead;

                // Decrypt the chunk and write it to the file
                byte[] decryptedChunk = cipher.update(buffer, 0, bytesRead);
                if (decryptedChunk != null) {
                    randomAccessFile.write(decryptedChunk);
                    messageDigest.update(decryptedChunk); // Update the hash with the decrypted data
                }
            }

            // Finalize decryption to handle any remaining bytes
            byte[] finalDecryptedChunk = cipher.doFinal();
            if (finalDecryptedChunk != null) {
                randomAccessFile.write(finalDecryptedChunk);
                messageDigest.update(finalDecryptedChunk);
            }

            // Receive the expected hash from the client
            String receivedHash = dataInputStream.readUTF();
            // Compute the hash of the received file
            String calculatedHash = Hasher.bytesToHex(messageDigest.digest());

            // Compare the received hash with the calculated hash
            if (!receivedHash.equals(calculatedHash)) {
                throw new IOException("File hash mismatch! Expected: " + receivedHash + ", Calculated: " + calculatedHash);
            }

            System.out.println("File received successfully. Hash verified: " + calculatedHash);

            // Update the database with the file transfer stats
            dbHandler.updateFileTransferStats(username, true, 1);
            if (visibility.equals("public")) {
                dbHandler.logCommand(username, "UPLOAD", fileName, true, true);
                dbHandler.addSharedFile(fileName, username);
            } else {
                dbHandler.logCommand(username, "UPLOAD", fileName, false, true);
            }

            // Perform an asynchronous virus scan
            new Thread(() -> {
                if (CheckVirus.isSafe(newFile)) {
                    System.out.println("File is safe.");
                } else {
                    System.err.println("File is infected!");
                    newFile.delete(); // Delete the file if it is infected
                    dbHandler.updateSentVirusesCount(username, 1);
                }
            }).start();

        } catch (IOException e) {
            write("Error during file reception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    




    /**
     * Sends a file to the client, encrypting it using AES and providing hash-based integrity verification.
     *
     * @param dir           the directory containing the file to send
     * @param fileName      the name of the file to be sent
     * @param sessionAESKey the AES key used for encryption
     * @param sessionIV     the initialization vector (IV) used for AES encryption
     * @return true if the file is sent successfully, false otherwise
     * @throws IOException if an error occurs during file transmission
     */
    public boolean sendFile(File dir, String fileName, SecretKey sessionAESKey, IvParameterSpec sessionIV) throws IOException {
        // Locate the file to send
        File file = new File(dir, fileName);
        if (!file.exists()) {
            write("File Not Found.");
            return false;
        }

        write("Ready"); // Notify the client that the server is ready to send the file

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // Prepare SHA-256 for hashing
            byte[] buffer = new byte[BUFFER_SIZE]; // Buffer for reading file data
            int bytesRead;

            // Initialize AES cipher in encryption mode
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sessionAESKey, sessionIV);

            // Stream to hold encrypted data
            ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();

            // Read the file in chunks, encrypt, and update the hash
            while ((bytesRead = randomAccessFile.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead); // Update the hash with original file data

                // Encrypt the chunk and add it to the stream
                byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
                if (encryptedChunk != null) {
                    encryptedStream.write(encryptedChunk);
                }
            }

            // Finalize encryption to handle any remaining bytes
            byte[] finalEncryptedChunk = cipher.doFinal();
            if (finalEncryptedChunk != null) {
                encryptedStream.write(finalEncryptedChunk);
            }

            // Convert encrypted stream to a byte array and send its size
            byte[] encryptedData = encryptedStream.toByteArray();
            long encryptedSize = encryptedData.length;
            dataOutputStream.writeLong(encryptedSize);

            // Send the encrypted file data to the client
            dataOutputStream.write(encryptedData);

            // Compute and send the hash of the original file
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
