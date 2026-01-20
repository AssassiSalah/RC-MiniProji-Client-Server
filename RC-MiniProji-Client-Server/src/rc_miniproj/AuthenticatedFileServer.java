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
	
	private static final String PATH_SERVER = System.getProperty("user.home") + "/RC_miniproj" + "/Server_storage";
		
    private int port;
    
    // maximum number of concurrent clients the server can handle.
    private static final int MAX_CLIENTS = 5;
        
    private Map<String, String> users;
    
    public AuthenticatedFileServer(int port) {
    	this.port = port;
    	users = new HashMap<>();
    	getUsers();
    	createIfNotExist(System.getProperty("user.home") + "/RC_miniproj");
    	createIfNotExist(PATH_SERVER);
	}
    
    // HACK this method work instead of Database (we don't have DB)
    public void getUsers() {
        // Sample user credentials
        users.put("user1", "pass1");
        users.put("user2", "pass2");
    }
    
    public static void createIfNotExist(String pathDir) {
		 File folderDir= new File(pathDir);
	    	
	    if(!folderDir.exists())
	    	folderDir.mkdirs();
	 }

    public void start() {
    	
    	// created with a fixed-size thread
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Client Is Here");
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
                    	File newFile = new File(userDir, fileName);
                        receiveFile(newFile, fileName, dataInputStream, dataOutputStream);
                        //ASYNC Check the file for viruses asynchronously
                        /*
                        new Thread(() -> {
                            if (CheckVirus.isSafe(newFile)) {
                                System.out.println("The file is safe.");
                            } else {
                                System.out.println("Warning: The file is infected!");
                            	// TODO Remove File
                            }
                        }).start();
                        */
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

	//UPLOAD Command
    private void receiveFile(File file, String fileName, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {

        if (file.exists()) {
            dataOutputStream.writeUTF("File Exist Already.");
            return;
        } else
        	dataOutputStream.writeUTF("Ready To Resieve.");

        long totalSize = Long.parseLong(dataInputStream.readUTF());
        long currentSize = 0;

        try (FileOutputStream fileOut = new FileOutputStream(file)) {
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
        File serverDir = new File(PATH_SERVER);
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
        
        long totalSize = file.length();
        long currentSize = 0;
        dataOutputStream.writeUTF("" + totalSize);

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
    
    private void exit(Socket clientSocket) throws IOException {
    	System.out.println("Client Want To Disconnecting");
    	clientSocket.close();
    	System.out.println("Client Disconnected Successfuly"); 
	}

}