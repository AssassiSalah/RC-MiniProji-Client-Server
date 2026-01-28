package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

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
    
    private static final int TIME_OUT = 300000; // 5 Minute
    
    private int port;
        
    private Map<String, String> users;
        
    public AuthenticatedFileServer(int port) {
    	this.port = port;
    	
    	users = FileManager.getUsers();//REPLACE with DB
    	
    	FileManager.createIfNotExist(PATH_PROJECT);
    	FileManager.createIfNotExist(PATH_SERVER);
	}
    
    public void start() {
    	// created with a fixed-size thread
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(TIME_OUT); // Set timeout to 5000 milliseconds (5 seconds)
                    System.out.println("New Client Is Here IP " + clientSocket.getInetAddress());
                    executorService.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    System.err.println("I/O error occurred while accepting a connection: " + e.getMessage());
                    System.err.println("Cause: Possible network or I/O issues.");
                } catch (RejectedExecutionException e) {
                    System.err.println("Task could not be executed: " + e.getMessage());
                    System.err.println("Cause: Executor service is not accepting tasks.");
                }
            }
        } catch (IOException e) {
            System.err.println("I/O error occurred while setting up the server socket: " + e.getMessage());
            System.err.println("Cause: Network issues or unable to bind to the specified port.");
        } catch (SecurityException e) {
            System.err.println("Security error: " + e.getMessage());
            System.err.println("Cause: Insufficient permissions to bind to the specified port.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid port number: " + port);
            System.err.println("Cause: Port numbers must be in the range 0-65535.");
        } finally {
            // Ensure that executor service is properly shut down
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                System.out.println("Executor service is shut down.");
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        	FileManager fileManager = new FileManager(new DataInputStream(clientSocket.getInputStream()), new DataOutputStream(clientSocket.getOutputStream()),reader,  writer);
            		
        	String username = ""; // initial with empty name
        	
        	File userDir = null;

            // Process client commands
            while (true) {
                String command = reader.readLine();
                System.out.println("Client : " + username + " Choose : " + command);
                switch (command) {
                	case "LOG_IN":
                		username = authenticate(reader, writer);
                    	
                        if (username.isEmpty()) {
                            // exit(clientSocket);
                        } else {
                        	userDir = new File(PATH_SERVER, username);
                        }

                        break;
                	case "REGISTER":
                			String newUsername = registerNewClient(reader, writer);
                			if(!newUsername.isEmpty()) {
                				File newUserDir = new File(PATH_SERVER, newUsername);
                				
                				if (!newUserDir.exists()) 
                					newUserDir.mkdirs();
                			}
                		break;
                    case "UPLOAD":
                    	String fileName = reader.readLine();
                        fileManager.receiveFile(userDir, fileName);
                        break;
                    case "LIST_FILES_USER":
                        listFilesUser(userDir, writer);
                        break;
                    case "LIST_FILES_SHARED":
                        listFilesShared(writer);
                        break;
                    case "DOWNLOAD":
                    	String requestedFileName_DOWNLOAD = reader.readLine();
                        fileManager.sendFile(userDir, requestedFileName_DOWNLOAD);                      
                        break;
                    case "ADVANCE_DOWNLOAD":
                    	String requestedFileName_Search = reader.readLine();
                        String whoHaveFile = fileManager.searchInCollaboration(requestedFileName_Search);
                        
                        if(!whoHaveFile.isEmpty())
                        	fileManager.sendFile(new File(PATH_SERVER, whoHaveFile), requestedFileName_Search);
                        
                        break;
                    case "REMOVE":
                    	String requestedFileName = reader.readLine();
                        fileManager.removeFile(userDir, requestedFileName);                      
                        break;
                    case "EXIT":
                    	exit(clientSocket, reader, writer);
                        return;
                    default:
                        writer.println("Unknown command");
                }
            }
        } catch (FileNotFoundException e) { //TODO
            System.err.println("File not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("I/O error occurred while handling client communication: " + e.getMessage());
            //e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Null pointer exception encountered: " + e.getMessage());
            //e.printStackTrace();
        } catch (SecurityException e) {
            System.err.println("Security exception: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
             exit(clientSocket);
        }
    }



	private String authenticate(BufferedReader reader, PrintWriter writer) {
    	// Authenticate user
		
		// Validate the username and password (basic validation)
        String username = null;
        String password = null;
        
		try {
            username = reader.readLine();  // Reading username
            password = reader.readLine();  // Reading password
        } catch (EOFException e) {
            System.err.println("Error: Reached the end of the stream unexpectedly.");
            System.err.println("Cause: The client or server closed the connection prematurely.");
            
	        writer.println("Authentication Failed");
	        return "";  // Returning false since data could not be read
        } catch (IOException e) {
            System.err.println("I/O error occurred while reading data.");
            System.err.println("Cause: General input/output issues, like network failure or data corruption.");
            
	        writer.println("Authentication Failed");
	        return "";  // Returning false in case of I/O error
        } catch (NullPointerException e) {
            System.err.println("Error: DataInputStream is not initialized.");
            System.err.println("Cause: The DataInputStream object is null.");
            
	        writer.println("Authentication Failed");
            return "";  // Returning false as the stream is null
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
            // e.printStackTrace();  // Log the stack trace for unexpected errors
	        writer.println("Authentication Failed");
            return "";  // Returning false for unexpected errors
        }
        
		try {
		    if (users.containsKey(username) && users.get(username).equals(password)) {
		        System.out.println("Authentication Successful. Welcome: " + username);
		        writer.println("Authentication Successful. Welcome: " + username);
		        return username;
		    } else {
		        writer.println("Authentication Failed");
		        return "";
		    }
		} catch (Exception e) {
		    System.err.println("Unexpected error occurred: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
		    // e.printStackTrace();  // Log the stack trace for unexpected errors
		    
		    writer.println("Authentication Failed");
			return "";
		}
    }
	
	 // Method to register a new client account
    private String registerNewClient(BufferedReader reader, PrintWriter writer) {
        // Validate the username and password (basic validation)
        String username = null;
        String password = null;

        try {
            username = reader.readLine();  // Reading username
            password = reader.readLine();  // Reading password
        } catch (EOFException e) {
            System.err.println("Error: Reached the end of the stream unexpectedly.");
            System.err.println("Cause: The client or server closed the connection prematurely.");
            writer.println("Register Failed");
            return "";  // Returning false since data could not be read
        } catch (IOException e) {
            System.err.println("I/O error occurred while reading data.");
            System.err.println("Cause: General input/output issues, like network failure or data corruption.");
            writer.println("Register Failed");
            return "";  // Returning false in case of I/O error
        } catch (NullPointerException e) {
            System.err.println("Error: DataInputStream is not initialized.");
            System.err.println("Cause: The DataInputStream object is null.");
            writer.println("Register Failed");
            return "";  // Returning false as the stream is null
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
		    // e.printStackTrace();  // Log the stack trace for unexpected errors
		    writer.println("Register Failed");
            return "";  // Returning false for unexpected errors
        }

        // Basic validation for username and password
        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Error: Username or password cannot be empty.");
            writer.println("Register Failed");
            return "";  // Returning false if username or password is empty
        }

        // Check if the username already exists
        if (users.containsKey(username)) {
            System.out.println("Error: Username already exists.");
            writer.println("Register Failed");
            return "";  // Returning false if username already exists
        }

        // Register the new user (assuming you have a HashMap to store users)
        users.put(username, password);  // Store the username and password (this could be improved to hash passwords)
        // TODO add in the database
        System.out.println("User registered successfully.");
        
        writer.println("Register Success");
        return username;  // Return true if registration is successful
    }
   
    private void listFilesUser(File userDir, PrintWriter writer) {
    	try {
    	    File[] files = userDir.listFiles();
    	    if (files == null || files.length == 0) {
    	        writer.println("No files found.");
    	    } else {
    	        for (File file : files) {
    	            writer.println(file.getName());
    	        }
    	    }
    	    writer.println("END");
    	} catch (SecurityException e) {
    	    System.err.println("Error: Permission denied to access the directory or read the files.");
    	    System.err.println("Cause: The program does not have permission to access the directory or its contents.");
    	} catch (NullPointerException e) {
    	    System.err.println("Error: The directory is null.");
    	    System.err.println("Cause: The `userDir` directory object is not properly initialized.");
    	} catch (Exception e) {
    	    System.err.println("Unexpected error occurred: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
		    // e.printStackTrace();  // Log the stack trace for unexpected errors
    	}

    }
   
    private void listFilesShared(PrintWriter writer) {
    	try {
            File folder = new File(AppConst.PATH_SERVER);
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt")); // Only .txt files

            if (files != null) {
                for (File file : files) {
                    //System.out.println("Reading file: " + file.getName());
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.println(line);
                        }
                    }
                }
            }
    	} catch (SecurityException e) {
    	    System.err.println("Error: Permission denied to access the directory or read the files.");
    	    System.err.println("Cause: The program does not have permission to access the directory or its contents.");
    	} catch (NullPointerException e) {
    	    System.err.println("Error: The directory is null.");
    	    System.err.println("Cause: The `userDir` directory object is not properly initialized.");
    	} catch (Exception e) {
    	    System.err.println("Unexpected error occurred: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
		    // e.printStackTrace();  // Log the stack trace for unexpected errors
    	}
	    writer.println("END");
	}
    
    private void exit(Socket clientSocket, BufferedReader reader, PrintWriter writer) {
    	System.out.println("Client Want To Disconnecting");
    	try {
    		// Closing streams first
    	    reader.close();  // Close input stream
    	    writer.close(); // Close output stream
    	    
    	    // Then close the socket
    	    exit(clientSocket);
    	} catch (IOException e) {
    	    System.err.println("I/O error occurred while closing resources.");
    	    System.err.println("Cause: There was an error while closing the socket or streams, possibly due to network or I/O issues.");
    	} catch (NullPointerException e) {
    	    System.err.println("Error: One or more resources (socket, input stream, or output stream) are not initialized.");
    	    System.err.println("Cause: Attempting to close a null reference of the socket or streams.");
    	} catch (Exception e) {
    	    System.err.println("Unexpected error occurred while closing resources: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
    	    // e.printStackTrace();  // Log the stack trace for unexpected errors
    	}

    	System.out.println("Client Disconnected Successfuly"); 
	}
    
	private void exit(Socket clientSocket) {
		try {
			
			if(!clientSocket.isClosed())
				clientSocket.close();
			
		} catch (IOException e) {
		    System.err.println("I/O error occurred while closing the socket.");
		    System.err.println("Cause: There was an error while closing the socket, possibly due to network or I/O issues.");
		} catch (NullPointerException e) {
		    System.err.println("Error: The client socket is not initialized.");
		    System.err.println("Cause: Attempting to close a null socket reference.");
		} catch (Exception e) {
		    System.err.println("Unexpected error occurred while closing the socket: " + e.getMessage());
		    System.err.println("Cause: Unknown.");
		    // e.printStackTrace();  // Log the stack trace for unexpected errors
		}

	}
}