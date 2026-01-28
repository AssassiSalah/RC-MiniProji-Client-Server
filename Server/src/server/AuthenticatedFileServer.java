package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
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
	
	private static final String PATH_PROJECT = System.getProperty("user.home") + "/RC_miniproj";
	
	private static final String PATH_SERVER = PATH_PROJECT + "/Server_storage";
    
    // maximum number of concurrent clients the server can handle.
    private static final int MAX_CLIENTS = 5;
    
    private int port;
        
    private Map<String, String> users;
        
    public AuthenticatedFileServer(int port) {
    	this.port = port;
    	
    	users = FileManager_server.getUsers();//REPLACE with DB
    	
    	FileManager_server.createIfNotExist(PATH_PROJECT);
    	FileManager_server.createIfNotExist(PATH_SERVER);
	}
    
    public void start() {
    	// created with a fixed-size thread
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Client Is Here");
                executorService.submit(() -> {
					try {
						handleClient(clientSocket);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Shutdown the Server");
            executorService.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) throws NoSuchAlgorithmException {
        try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
        	 DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());)
        {
        	
        	FileManager_server fileManager = new FileManager_server(dataInputStream, dataOutputStream);
            		
        	String username = authenticate(dataInputStream);
        	
            if (username.isEmpty()) {
                dataOutputStream.writeUTF("Authentication Failed");
                clientSocket.close();
                return;
            }    

            dataOutputStream.writeUTF("Authentication Successful Welcome : " + username);
            System.out.println("Authentication Successful Welcome : " + username);
            
            File userDir = new File(PATH_SERVER, username);
            
            // Create Folder For This User If Is Not Exist
            if (!userDir.exists()) 
            	userDir.mkdirs();

            // Process client commands
            while (true) {
                String command = dataInputStream.readUTF();
                System.out.println("Client : " + username + " Choose : " + command);
                switch (command) {
                    case "UPLOAD":
                    	String fileName = dataInputStream.readUTF();
                        fileManager.receiveFile(userDir, fileName);
                        break;
                    case "LIST_FILES_USER":
                        listFiles(userDir, dataOutputStream);
                        break;
                    case "DOWNLOAD":
                    	String requestedFileName_DOWNLOAD = dataInputStream.readUTF();
					try {
						fileManager.sendFile(userDir, requestedFileName_DOWNLOAD);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}                      
                        break;
                    case "REMOVE":
                    	String requestedFileName = dataInputStream.readUTF();
                        fileManager.removeFile(userDir, requestedFileName);                      
                        break;
                    case "EXIT":
                    	exit(clientSocket, dataInputStream, dataOutputStream);
                        return;
                    default:
                        dataOutputStream.writeUTF("Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                exit(clientSocket);
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
   
    private void exit(Socket clientSocket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
    	System.out.println("Client Want To Disconnecting");
    	clientSocket.close();
    	dataInputStream.close();
        dataOutputStream.close();
    	System.out.println("Client Disconnected Successfuly"); 
	}
    

	private void exit(Socket clientSocket) throws IOException {
		clientSocket.close();
	}
}