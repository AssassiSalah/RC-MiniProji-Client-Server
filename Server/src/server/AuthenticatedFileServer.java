package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * AuthenticatedFileServer class handles multiple client connections with user
 * authentication, file upload/download functionality, and file management. The
 * server allows clients to log in, register, upload files, download files, list
 * files, and collaborate by searching for files on other users' servers.
 */
public class AuthenticatedFileServer {

	private static final int MAX_CLIENTS = 10; // Maximum number of concurrent clients
	private static final int TIME_OUT = 300000; // 5 minutes timeout

	private int port;
	private Map<String, String> users;

	/**
	 * Constructs an AuthenticatedFileServer instance.
	 * 
	 * @param port the port on which the server will listen for incoming connections
	 */
	public AuthenticatedFileServer(int port) {
		this.port = port;
		users = FileManagerServer.getUsers(); // Replace with DB in production
		FileManagerServer.createIfNotExist(AppConst.PATH_PROJECT);
		FileManagerServer.createIfNotExist(AppConst.PATH_SERVER);
	}

	/**
	 * Starts the file server, accepting client connections and processing commands.
	 */
	public void start() {
		// Create an ExecutorService with a fixed thread pool for handling multiple
		// clients
		ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server is running on port " + port);

			// Accept client connections indefinitely
			while (true) {
				try {
					// Accept client connection
					Socket clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(TIME_OUT); // Set a timeout of 5 minutes
					System.out.println("New Client Is Here IP " + clientSocket.getInetAddress());
					executorService.submit(() -> handleClient(clientSocket)); // Handle client requests
				} catch (IOException e) {
					// Handle exceptions when accepting client connections
					System.err.println("I/O error occurred while accepting a connection: " + e.getMessage());
				} catch (RejectedExecutionException e) {
					// Handle task rejection when thread pool is full
					System.err.println("Task could not be executed: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			// Handle errors during server socket setup
			System.err.println("I/O error occurred while setting up the server socket: " + e.getMessage());
		} finally {
			// Ensure executor service is properly shut down
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
				System.out.println("Executor service is shut down.");
			}
		}
	}

	/**
	 * Handles the communication with a connected client. Processes commands such as
	 * log in, register, upload, download, etc.
	 * 
	 * @param clientSocket the socket representing the client connection
	 */
	private void handleClient(Socket clientSocket) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

			FileManagerServer fileManager = new FileManagerServer(new DataInputStream(clientSocket.getInputStream()),
					new DataOutputStream(clientSocket.getOutputStream()), reader, writer);
			String username = ""; // Initial empty username
			File userDir = null; // Directory for the user files

			// Process client commands indefinitely
			while (true) {
				String command = reader.readLine(); // Read client command
				System.out.println("Client: " + username + " Command: " + command);
				switch (command) {
				case "LOG_IN":
					// Log in the user and authenticate
					username = authenticate(reader, writer);
					if (username.isEmpty()) {
						// If authentication fails, handle exit (if necessary)
					} else {
						userDir = new File(AppConst.PATH_SERVER, username); // Set user directory
					}
					break;
				case "REGISTER":
					// Register a new client
					String newUsername = registerNewClient(reader, writer);
					if (!newUsername.isEmpty()) {
						File newUserDir = new File(AppConst.PATH_SERVER, newUsername);
						if (!newUserDir.exists())
							newUserDir.mkdirs(); // Create user directory
					}
					break;
				case "UPLOAD":
					// Handle file upload
					String fileName = reader.readLine();
					fileManager.receiveFile(userDir, fileName);
					break;
				case "LIST_FILES_USER":
					// List files of the user
					listFilesUser(userDir, writer);
					break;
				case "LIST_FILES_SHARED":
					// List shared files
					listFilesShared(writer);
					break;
				case "DOWNLOAD":
					// Handle file download
					String requestedFileName_DOWNLOAD = reader.readLine();
					System.out.println("File Name : " + requestedFileName_DOWNLOAD);
					fileManager.sendFile(userDir, requestedFileName_DOWNLOAD);
					break;
				case "ADVANCE_DOWNLOAD":
					// Handle advanced file search and download
					String requestedFileName_Search = reader.readLine();
					String whoHaveFile = fileManager.searchInCollaboration(requestedFileName_Search);
					if (!whoHaveFile.isEmpty())
						fileManager.sendFile(new File(AppConst.PATH_SERVER, whoHaveFile), requestedFileName_Search);
					break;
				case "REMOVE":
					// Handle file removal
					String requestedFileName = reader.readLine();
					fileManager.removeFile(userDir, requestedFileName);
					break;
				case "EXIT":
					// Exit the client connection
					exit(clientSocket, reader, writer);
					return;
				default:
					writer.println("Unknown command");
				}
			}
		} catch (IOException e) {
			System.err.println("I/O error occurred while handling client communication: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unexpected error occurred: " + e.getMessage());
		} finally {
			exit(clientSocket); // Ensure client is disconnected properly
		}
	}

	/**
	 * Authenticates the user with a username and password.
	 * 
	 * @param reader BufferedReader to read client input
	 * @param writer PrintWriter to send responses to the client
	 * @return the username if authentication is successful, empty string otherwise
	 */
	private String authenticate(BufferedReader reader, PrintWriter writer) {
		String username = null;
		String password = null;

		try {
			// Read username and password from client
			username = reader.readLine();
			password = reader.readLine();
		} catch (IOException e) {
			System.err.println("I/O error occurred while reading data.");
			writer.println("Authentication Failed");
			return "";
		}

		// Validate user credentials
		if (users.containsKey(username) && users.get(username).equals(password)) {
			System.out.println("Authentication Successful. Welcome: " + username);
			writer.println("Authentication Successful. Welcome: " + username);
			return username;
		} else {
			writer.println("Authentication Failed");
			return "";
		}
	}

	// Method to register a new client account
	// It reads the username and password from the client, validates them, and
	// registers the client if the data is valid.
	private String registerNewClient(BufferedReader reader, PrintWriter writer) {
		// Validate the username and password (basic validation)
		String username = null;
		String password = null;

		try {
			// Reading username and password from the client
			username = reader.readLine(); // Reading username
			password = reader.readLine(); // Reading password
		} catch (EOFException e) {
			// Handles unexpected EOF during reading
			System.err.println("Error: Reached the end of the stream unexpectedly.");
			writer.println("Register Failed");
			return ""; // Return empty string if data could not be read
		} catch (IOException e) {
			// Handles I/O errors during reading
			System.err.println("I/O error occurred while reading data.");
			writer.println("Register Failed");
			return ""; // Return empty string in case of I/O error
		} catch (NullPointerException e) {
			// Handles null pointer exceptions during reading
			System.err.println("Error: DataInputStream is not initialized.");
			writer.println("Register Failed");
			return ""; // Return empty string if stream is null
		} catch (Exception e) {
			// Catches any unexpected exceptions
			System.err.println("Unexpected error occurred: " + e.getMessage());
			writer.println("Register Failed");
			return ""; // Return empty string for unexpected errors
		}

		// Basic validation for username and password
		if (username.isEmpty() || password.isEmpty()) {
			// Username or password cannot be empty
			System.out.println("Error: Username or password cannot be empty.");
			writer.println("Register Failed");
			return ""; // Return empty string if validation fails
		}

		// Check if the username already exists
		if (users.containsKey(username)) {
			// Handle case where username already exists
			System.out.println("Error: Username already exists.");
			writer.println("Register Failed");
			return ""; // Return empty string if username already exists
		}

		// Register the new user (assuming you have a HashMap to store users)
		users.put(username, password); // Store the username and password (this could be improved to hash passwords)
		// TODO: Store in the database instead of HashMap
		System.out.println("User registered successfully.");

		writer.println("Register Success");
		return username; // Return username if registration is successful
	}

	// Method to list the files belonging to a specific user
	// It reads the directory of the user and sends the list of files back to the
	// client.
	private void listFilesUser(File userDir, PrintWriter writer) {
		try {
			// Retrieve the list of files in the user directory
			File[] files = userDir.listFiles();
			if (files == null || files.length == 0) {
				writer.println("No files found.");
			} else {
				// List each file name in the directory
				for (File file : files) {
					writer.println(file.getName());
				}
			}
			writer.println("END");
		} catch (SecurityException e) {
			// Handle permission errors
			System.err.println("Error: Permission denied to access the directory or read the files.");
		} catch (NullPointerException e) {
			// Handle null directory reference
			System.err.println("Error: The directory is null.");
		} catch (Exception e) {
			// Handle unexpected errors
			System.err.println("Unexpected error occurred: " + e.getMessage());
		}
	}

	// Method to list shared files on the server
	// It retrieves and sends the names and contents of .txt files stored on the
	// server to the client.
	private void listFilesShared(PrintWriter writer) {
		try {
			// List all .txt files in the server folder
			File folder = new File(AppConst.PATH_SERVER);
			File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt")); // Only .txt files

			if (files != null) {
				for (File file : files) {
					try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
						String line;
						// Read each line from the shared file and send it to the client
						while ((line = reader.readLine()) != null) {
							writer.println(line);
						}
					}
				}
			}
		} catch (SecurityException e) {
			// Handle permission errors when accessing the folder or reading files
			System.err.println("Error: Permission denied to access the directory or read the files.");
		} catch (NullPointerException e) {
			// Handle null directory reference
			System.err.println("Error: The directory is null.");
		} catch (Exception e) {
			// Handle unexpected errors
			System.err.println("Unexpected error occurred: " + e.getMessage());
		}
		writer.println("END");
	}

	// Method to handle client disconnection and resource cleanup
	// It closes the reader, writer, and socket, ensuring proper cleanup of
	// resources.
	private void exit(Socket clientSocket, BufferedReader reader, PrintWriter writer) {
		System.out.println("Client Wants To Disconnect");
		try {
			// Close the input and output streams first
			reader.close(); // Close input stream
			writer.close(); // Close output stream

			// Then close the client socket
			exit(clientSocket);
		} catch (IOException e) {
			// Handle I/O errors while closing resources
			System.err.println("I/O error occurred while closing resources.");
		} catch (NullPointerException e) {
			// Handle null references during cleanup
			System.err.println("Error: One or more resources are not initialized.");
		} catch (Exception e) {
			// Handle unexpected errors during cleanup
			System.err.println("Unexpected error occurred while closing resources: " + e.getMessage());
		}

		System.out.println("Client Disconnected Successfully");
	}

	// Helper method to close the client socket
	private void exit(Socket clientSocket) {
		try {
			if (!clientSocket.isClosed()) {
				clientSocket.close(); // Close the socket
			}
		} catch (IOException e) {
			// Handle I/O errors while closing the socket
			System.err.println("I/O error occurred while closing the socket.");
		} catch (NullPointerException e) {
			// Handle null socket reference
			System.err.println("Error: The client socket is not initialized.");
		} catch (Exception e) {
			// Handle unexpected errors while closing the socket
			System.err.println("Unexpected error occurred while closing the socket: " + e.getMessage());
		}
	}

}