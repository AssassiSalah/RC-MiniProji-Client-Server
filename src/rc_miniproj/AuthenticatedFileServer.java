package rc_miniproj;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * CHECK used for method that i didn't check they results [Low]
 * COMPLITE used for method that didn't finish [Normal]
 * FIXME used for method have an error [HIGH]
 * HACK used for temporary solution [Normal]
 * REPLACE used for method i must replace [Low] 
 * TODO used for other problem [Normal]
 */

public class AuthenticatedFileServer {
	
    private static final int PORT = 5015;
    
    // maximum number of concurrent clients the server can handle.
    private static final int MAX_CLIENTS = 5;
        
    private Map<String, String> users;
    
    public AuthenticatedFileServer() {
    	users = new HashMap<>();
    	getUsers();
	}
    
    // HACK this method work instead of Database (we don't have DB)
    public void getUsers() {
        // Sample user credentials
        users.put("user1", "pass1");
        users.put("user2", "pass2");
    }

    public void start() {
    	
    	// created with a fixed-size thread
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Shutdown the Server");
            executorService.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
        	 DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());)
        {
            		
        	String username = authenticate(dataInputStream);
        	
            if (username.isEmpty()) {
                dataOutputStream.writeUTF("Authentication Failed");
                clientSocket.close();
                return;
            }    

            dataOutputStream.writeUTF("Authentication Successful Welcome : " + username);
            
            File serverDir = new File("server_storage");
            
            // Create Folder For This User If Is Not Exist
            if (!serverDir.exists()) 
            	serverDir.mkdirs();
            
            File userDir = new File("server_storage/" + username);
            
            // Create Folder For This User If Is Not Exist
            if (!userDir.exists()) 
            	userDir.mkdirs();

            // Process client commands
            while (true) {
                String command = dataInputStream.readUTF();
                System.out.println("client Choose : " + command);
                switch (command) {
                    case "UPLOAD":
                    	String fileName = dataInputStream.readUTF();
                        receiveFile(userDir, fileName, dataInputStream, dataOutputStream);
                        break;
                    case "LIST_FILES_USER":
                        listFiles(userDir, dataOutputStream);
                        break;
                    case "DOWNLOAD":
                    	String requestedFileName = dataInputStream.readUTF();
                        boolean foundFile = sendFile(userDir, requestedFileName, dataOutputStream);
                        
                        if(!foundFile)
                        	System.out.println("Not Fount In The Server");
                        	//sendFile(serverDir, requestedFileName, dataOutputStream);

                        break;
                    case "EXIT":
                    	exit(clientSocket);
                        return;
                    default:
                        dataOutputStream.writeUTF("Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                //clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	private String authenticate(DataInputStream dataInputStream) throws IOException {
    	// Authenticate user
        String username = dataInputStream.readUTF();
        String password = dataInputStream.readUTF();
        
        return users.containsKey(username) && users.get(username).equals(password) ? username : "";
    }

    private void receiveFile(File userDir, String fileName, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {

        File file = new File(userDir, fileName);
        
        if (file.exists()) {
            dataOutputStream.writeUTF("File Exist Already.");
            return;
        } else
        	dataOutputStream.writeUTF("Ready To Resieve.");
        
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            dataOutputStream.writeUTF("File " + fileName + " uploaded successfully.");
        }
    }

    private void listFiles(File userDir, DataOutputStream dataOutputStream) throws IOException {
    	
        File[] files = userDir.listFiles();
        if (files == null || files.length == 0) {
            dataOutputStream.writeUTF("No files found.");
        } else {
        	dataOutputStream.writeUTF("Files:");
        	for (File file : files) {
            	dataOutputStream.writeUTF(file.getName());
        	}
        }
        dataOutputStream.writeUTF("END");
    }

    
    // CHECK We Will Not Use This Method For The Current Time
    @SuppressWarnings("unused")
	private void listServerFiles(DataOutputStream dataOutputStream) throws IOException {
        File serverDir = new File("server_storage");
        File[] files = serverDir.listFiles();
        if (files == null || files.length == 0) {
            dataOutputStream.writeUTF("No files found on server.");
            return;
        }
        dataOutputStream.writeUTF("Server Files:");
        for (File file : files) {
            dataOutputStream.writeUTF(file.getName());
        }
    }

    private boolean sendFile(File dir, String requestedFileName, DataOutputStream dataOutputStream) throws IOException {
        
        File file = new File(dir, requestedFileName);

        if (!file.exists()) {
            dataOutputStream.writeUTF("File Not Found.");
            return false;
        }

        dataOutputStream.writeUTF("Starting file transfer");
        System.out.println("Starting file transfer");
        System.out.println();
        
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
            	System.out.println(bytesRead);
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            
            System.out.println("Done");
            dataOutputStream.flush();
 
            //dataOutputStream.writeUTF("File Transfer Complete.");
            System.out.println("File Transfer Complete.");
            return true;
        }
    }
    
    private void exit(Socket clientSocket) throws IOException {
    	System.out.println("Client Want To Disconnecting");
    	clientSocket.close();
    	System.out.println("Client Disconnected Successfuly"); 
	}
}





// pathon install JDK
