package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import check_virus.CheckVirus;

public class FileManager {
	
	private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
	
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
		 File folderDir= new File(pathDir);
	    	
	    if(!folderDir.exists())
	    	folderDir.mkdirs();
	    
	 }
	
	//UPLOAD Command
    public void receiveFile(File dir, String fileName) throws IOException {
    	File newFile = new File(dir, fileName);

        if (newFile.exists()) {
            dataOutputStream.writeUTF("File Exist Already.");
            return;
        } else
        	dataOutputStream.writeUTF("Ready To Receive.");

        long totalSize = dataInputStream.readLong(); // Read file size
        long currentSize = 0;

        try (FileOutputStream fileOut = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while(currentSize < totalSize) {
            	bytesRead = dataInputStream.read(buffer);
            	// Check if the end of the file is reached
                if (bytesRead == -1) {
                    break; // Exit the loop
                }
                fileOut.write(buffer, 0, bytesRead);
                //System.out.println(currentSize + " " + totalSize);
                System.out.println((double) currentSize / totalSize * 100 + "%");
                System.out.println();
                currentSize += bytesRead; 
            }
            System.out.println((double) currentSize / totalSize + "%");
            System.out.println();
            dataOutputStream.writeUTF("File " + fileName + " uploaded successfully.");
        }
        
      //ASYNC Check the file for viruses asynchronously
        new Thread(() -> {
            if (CheckVirus.isSafe(newFile)) {
                System.out.println("The file is safe.");
            } else {
                System.out.println("Warning: The file is infected!");
            	// TODO Remove File
            }
        }).start();
    }

	//DOWNLOAD Command
    public boolean sendFile(File dir, String requestedFileName) throws IOException {
        
        File file = new File(dir, requestedFileName);

        if (!file.exists()) {
            dataOutputStream.writeUTF("File Not Found.");
            return false;
        }
        
        dataOutputStream.writeUTF("Starting file transfer");
        
        long totalSize = file.length();
        long currentSize = 0;
        System.out.println(totalSize + " " + currentSize);
        
        dataOutputStream.writeLong(totalSize); // Send file size
        //dataOutputStream.writeUTF("" + totalSize);

        System.out.println("Starting file transfer");
        System.out.println();
        
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while(currentSize < totalSize) {
            	bytesRead = fileIn.read(buffer);
            	
            	// Check if the end of the file is reached
                if (bytesRead == -1) {
                    break; // Exit the loop
                }
                
                dataOutputStream.write(buffer, 0, bytesRead);
                
                System.out.println((double) currentSize / totalSize * 100 + "%");
                System.out.println();
                currentSize += bytesRead; 
            }
            
            System.out.println((double) currentSize / totalSize + "%");
            System.out.println();
            
            System.out.println("Done");
            dataOutputStream.flush();
 
            //dataOutputStream.writeUTF("File Transfer Complete.");
            System.out.println("File Transfer Complete.");
            return true;
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
