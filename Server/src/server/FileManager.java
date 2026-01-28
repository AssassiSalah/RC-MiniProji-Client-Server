package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import check_virus.CheckVirus;

public class FileManager {
	
	private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private BufferedReader reader;
    private PrintWriter writer ;
    
    private static final int BUFFER_SIZE = 8192; // Adjust as needed
	
    public FileManager(DataInputStream dataInputStream, DataOutputStream dataOutputStream, BufferedReader reader, PrintWriter writer) {
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.reader = reader;
        this.writer = writer;
    }
	
    // HACK this method work instead of Database (we don't have DB)
    public static Map<String, String> getUsers() {
    	Map<String, String> users = new HashMap<String, String>();
        // Sample user credentials
    	users.put("admin", util.Hasher.hashPassword("admin"));
    	createIfNotExist("admin");
    	users.put("1", util.Hasher.hashPassword("1"));
    	createIfNotExist("1");
        users.put("user1", util.Hasher.hashPassword("pass1"));
        createIfNotExist("user1");
        users.put("user2", util.Hasher.hashPassword("pass2"));
        createIfNotExist("user2");
        return users;
    }

	public static void createIfNotExist(String pathDir) {
		 File folderDir= new File(pathDir);
	    	
	    if(!folderDir.exists())
	    	folderDir.mkdirs();
	    
	 }
	
    public String read(BufferedReader reader) {
        try {
			return reader.readLine();
		} catch (IOException e) {
		    System.err.println("I/O error occurred while reading a line.");
		    System.err.println("Cause: General input/output issues, like network failure or stream interruption.");
		} catch (Exception e) {
		    System.err.println("Unexpected error occurred: " + e.getMessage());
		    e.printStackTrace(); // Log the stack trace for debugging.
		}
		return null;
    }
    
    public void write(String message) {
    	writer.println(message);
    }
	
	//UPLOAD Command
    public void receiveFile(File dir, String fileName) throws IOException {
    	File newFile = new File(dir, fileName);
	    MessageDigest messageDigest;
		try {
			// Using SHA-256 for hash calculation
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("SHA-256 is not exsit.");
			System.err.println("Cause : not set up java probably");
			write("Algo Of Hash Not Exist.");
            return;
		} 

        if (newFile.exists()) {
            write("File Already Exist.");
            return;
        } else
        	write("Ready To Receive.");

        long totalSize = dataInputStream.readLong(); // Read file size
        
        if (totalSize == 0) {
	        dataOutputStream.writeUTF("Invalid file size.");
	        return;
	    }
        
        long currentSize = 0;

	    try (RandomAccessFile randomAccessFile = new RandomAccessFile(newFile, "rw")) {
            byte[] buffer = new byte[4096];
            int bytesRead;
	        int sequenceNumber = 0; // Sequence number to ensure proper chunk order

			for (; currentSize < totalSize; currentSize += bytesRead) {
				// Log progress percentage
				//System.out.printf("%.2f%% Recieve File%n", ((double) currentSize / totalSize) * 100);
				
            	bytesRead = dataInputStream.read(buffer);
            	// Check if the end of the file is reached
                if (bytesRead == -1) { //EOF
                    break; // Exit the loop
                }
                
                // Write the data to the file
	            randomAccessFile.write(buffer, 0, bytesRead);
	            messageDigest.update(buffer, 0, bytesRead); // Update the hash with the received data
	            
	            sequenceNumber++;
	            
	            // Display progress for every 10,000 bytes (you can adjust this logic if needed)
	            if (currentSize % 10000 == 0) {
	                double progress = (double) currentSize / totalSize * 100;
	               System.out.printf("Received chunk %d: %.2f%%\n", sequenceNumber, progress);
	            }

	            // Optionally, send back the progress to the client (only display for one out of every 10,000 packets)
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
	        
	        ///TODO Test The Hash
			
            write("File " + fileName + " Transfer Successful.");
            
            write("File received. Hash: " + calculatedHash);
	        System.out.println("100%");
        } catch (IOException e) {
	        System.out.println("Error during file reception: " + e.getMessage());
	        write("Error during file reception.");
	        write("no Hash.");
	        // e.printStackTrace();
	    }
        
     // Async virus scan
        new Thread(() -> {
            if (CheckVirus.isSafe(newFile)) { // check this
                System.out.println("The file is safe.");
            } else {
                System.out.println("Warning: The file is infected!");
                // TODO if check Virus Validate Then Remove Comments
                // newFile.delete(); // Remove the file if infected
            }
        }).start();
    }

	//DOWNLOAD Command
    public boolean sendFile(File dir, String requestedFileName) throws IOException {
        
        File file = new File(dir, requestedFileName);

        if (!file.exists()) {
            write("File Not Found.");
            return false;
        }
        
        write("Starting file transfer");
        
        long totalSize = file.length();
        System.out.println("Total Size : " + totalSize);
                
        dataOutputStream.writeLong(totalSize); // Send file size

        System.out.println("Starting file transfer\n");
        
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) { // FileInputStream
	        long packetCount = (long) Math.ceil((double) totalSize / BUFFER_SIZE);
	        byte[][] packets = new byte[(int) packetCount][];
	        String[] packetHashes = new String[(int) packetCount];

	        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

	        // Preload file into packets and compute hashes
	        for (int i = 0; i < packetCount; i++) {
	            int packetSize = (int) Math.min(BUFFER_SIZE, totalSize - (long) i * BUFFER_SIZE);
	            byte[] packet = new byte[packetSize];
	            randomAccessFile.seek((long) i * BUFFER_SIZE); // send the i packet 
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
            	write("Success");
                System.out.println("File removed successfully");
                return true;
            } else {
            	write("Error");
                System.out.println("Failed to remove the file");
                return false;
            }
        } else {
        	write("Not Found");
            System.out.println("File does not exist: " + file.getAbsolutePath());
            return false;
        }
    }   

    public String searchInCollaboration(String fileName) {
        File folder = new File(AppConst.PATH_SERVER);

        // Get all .txt files in the folder
        File[] textFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        System.out.println("text files is null? " + (textFiles == null));
        if (textFiles != null) {
            System.out.println("text files size : " + textFiles.length);
            for (File textFile : textFiles) {
                // Read each .txt file line by line
                System.out.println("Check textFile : " + textFile);
                try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().equals(fileName)) {
                        	String whoHaveFile = textFile.getName();
                        	
                        	if (whoHaveFile.endsWith(".txt")) {
                        		whoHaveFile = whoHaveFile.substring(0, whoHaveFile.lastIndexOf("."));
                        	}
                        	write("File Exist.");
                        	System.out.println("File Exist With : " + whoHaveFile);
                        	return whoHaveFile;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + textFile.getName());
                    // e.printStackTrace();
                }
            }
        }

        // If the file name was not found in any .txt file
        write("File Dosn't Exist.");
        System.out.println("File Dosn't Exist.");
        return "";
    }
    
    /*
    private static String searchInFolder(File folder, String fileName) { // search in all server
            
        // Ensure the folder exists and is a directory
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("The File Not Exist In Another Clints");;
        }
        
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursive call for subdirectories
                    String foundPath = searchInFolder(file, fileName);
                    if (foundPath != null) {
                        return foundPath;
                    }
                } else if (file.getName().equals(fileName)) {
                    // File found
                    return file.getAbsolutePath();
                }
            }
        }

        // File not found in this directory or its subdirectories
        return null;
    }
    */
}
