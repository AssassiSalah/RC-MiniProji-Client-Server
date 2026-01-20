package rc_miniproj;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AuthenticatedFileClient {
    private static final String SERVER_ADDRESS = "localhost";//"192.168.1.4";//"localhost";
    private static final int SERVER_PORT = 1234;
    
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Scanner scanner;

    AuthenticatedFileClient() {
    	scanner = new Scanner(System.in);
    }
    
    public void start() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
        	
        	dataInputStream = new DataInputStream(socket.getInputStream());
        	dataOutputStream = new DataOutputStream(socket.getOutputStream());
        	
            // Call authenticate method
            if (!authenticate()) {
                System.out.println("Client Disconnecting.");
                return;
            }

            System.out.println("HI");
            // Process Commands
            while (true) {
            	System.out.println();
                System.out.print("Enter command (UPLOAD, LIST_FILES_USER, DOWNLOAD, EXIT): ");
                String command = scanner.nextLine();
            	System.out.println();
                dataOutputStream.writeUTF(command);

                switch (command) {
                    case "UPLOAD":
                    	System.out.print("Enter file path to upload: ");
                        String filePath = scanner.nextLine();
                        uploadFile(filePath);
                        break;
                    case "LIST_FILES_USER":
                    	List_Files_Server();
                        break;
                    case "DOWNLOAD":
                    	File downloadDir = new File("downloads");
                        if (!downloadDir.exists()) 
                        	downloadDir.mkdirs();
                        
                        System.out.print("Enter file name to download: ");
                        String fileName = scanner.nextLine();
                        downloadFile(fileName, "downloads");
                        break;
                    case "EXIT":
                        exitConnection();
                        return;
                    default:
                        System.out.println("Unknown command.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private boolean authenticate() throws IOException {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            // Send credentials to server
            dataOutputStream.writeUTF(username);
            dataOutputStream.writeUTF(password);

            // Read response from server
            String response = dataInputStream.readUTF();
            System.out.println(response);
            return response.contains("Successful");
    }

	private void uploadFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()) {
        	System.out.println("File Not Exist");
        	return;
        }
        
        dataOutputStream.writeUTF(file.getName());
        
        String serverResponse = dataInputStream.readUTF();
        System.out.println(serverResponse);
        if (!serverResponse.contains("Ready")) {
            //System.out.println("Server not ready to receive the file.");
        	System.out.println("File Already Exist");
            return;
        }
        
        long totalSize = file.length();
        long currentSize = 0;
        dataOutputStream.writeUTF("" + totalSize);
        
        //COMPLITE before send file check if the is virus or not (another class)

        System.out.println("start to upload ");
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while(currentSize < totalSize) {
            	bytesRead = fileInputStream.read(buffer);
            	// Check if the end of the file is reached
                if (bytesRead == -1) {
                    break; // Exit the loop
                }
                dataOutputStream.write(buffer, 0, bytesRead);
                //System.out.println(currentSize + " " + totalSize);
                System.out.println((double) currentSize / totalSize * 100 + "%");
                System.out.println();
                currentSize += bytesRead; 
            }
            
            System.out.println((double) currentSize / totalSize * 100 + "%");
        	System.out.println();
            dataOutputStream.flush();
            System.out.println("File uploaded successfully.");
        }
    }

    private void downloadFile(String fileName, String destination) throws IOException {
        dataOutputStream.writeUTF(fileName);

        String response = dataInputStream.readUTF();
        System.out.println(response);
        
        if (response.contains("Not Found")) {
            System.out.println("Error: " + response);
            return;
        }

        File downloadedFile = new File(destination + "/" + fileName);
        String s = dataInputStream.readUTF();
        System.out.println(s);
        long totalSize = Long.parseLong(s);
        long currentSize = 0;

        try (FileOutputStream fileOut = new FileOutputStream(downloadedFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            System.out.println("Start The Reading");
            
            // Read file data until the end of the stream
            while(currentSize < totalSize) {
            	bytesRead = dataInputStream.read(buffer);
            	
            	// Check if the end of the file is reached
                if (bytesRead == -1) {
                    break; // Exit the loop
                }
                
            	//System.out.println(bytesRead);
                fileOut.write(buffer, 0, bytesRead);
                
                System.out.println((double) currentSize / totalSize * 100 + "%");
                System.out.println();
                currentSize += bytesRead; 
            }
        }
            
        	System.out.println((double) currentSize / totalSize * 100 + "%");
        	System.out.println();
        	System.out.println("File downloaded successfully.");
        	System.out.println();
    }

    private void List_Files_Server() throws IOException { 	
        String response;
        while (!(response = dataInputStream.readUTF()).equals("END")) {
            System.out.println(response);
        }
    }
    
    private static void exitConnection() {
    	System.out.println();
    	System.out.println("Client Disconnected Successfuly");
		// TODO Nothing Here For The Current Time
		
	}

}