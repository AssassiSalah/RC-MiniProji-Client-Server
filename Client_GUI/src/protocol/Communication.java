package protocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import application.Load_Interfaces;
import javafx.application.Platform;

public class Communication {
	
	public ConnectionManager connectionManager;
	private FileManager fileManager;
	
    private BufferedReader reader;
    private PrintWriter writer;
    
    public Communication() {
    	connectionManager = new ConnectionManager();    	
    }
    
    public String read() {
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

	public void download(String fileName) {
		write("DOWNLOAD");
		write(fileName);

		String response = read();
		
		System.out.println(response);

		if (!response.contains("Ready")) {
			if(response.contains("Algo"))
				System.out.println("Algo Not Exist.");
			else
				System.out.println("File Already Exist");
			return;
		}
		try {
			Load_Interfaces.displayCircleProgress();
			fileManager.downloadFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Load_Interfaces.displayDownload();
	}
	
	public void advDownload(String fileName) {
		write("ADVANCE_DOWNLOAD");
		write(fileName);
		
		String response = read();
		
		if(response.equals("File Exist.")) {
			try {
				Load_Interfaces.displayCircleProgress();
				fileManager.downloadFile(fileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("i told you");
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Load_Interfaces.displayAdvDownload();
	}
	
	public void upload(String filePath) {

		write("UPLOAD");
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("File Not Exist");
			return;
		}

		write(file.getName());

		String serverResponse = read();
		System.out.println(serverResponse);
		if (!serverResponse.contains("Ready")) {
			if (serverResponse.contains("Algo"))
				System.out.println("Algo Not Exist.");
			else
				System.out.println("File Already Exist");
			return;
		}
		
		try {
			Load_Interfaces.displayCircleProgress();
			fileManager.uploadFile(file, file.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeFile(String fileName) {
		fileManager.removeFileFromServer(fileName);
	}
	
	public boolean isConnect() {
		return connectionManager.isConnect();
	}
	
	public void connect() {
    	if(!connectionManager.connected()) {
    		System.exit(1);
    	}
    			
		Socket clientSocket = connectionManager.getClientSoket();
    	try {
    		fileManager = new FileManager(this, new DataInputStream(clientSocket.getInputStream()), new DataOutputStream(clientSocket.getOutputStream()));
    		reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        	writer = new PrintWriter(clientSocket.getOutputStream(), true);
    	} catch (IOException e) {
    	    System.err.println("I/O error occurred while initializing streams.");
    	    System.err.println("Cause: Issues with the socket input/output streams, possibly due to network problems or resource limitations.");
    	    System.exit(1);
    	} catch (NullPointerException e) {
    	    System.err.println("Error: clientSocket is not initialized.");
    	    System.err.println("Cause: The clientSocket object is null, potentially due to incorrect initialization.");
    	    System.exit(1);
    	} catch (SecurityException e) {
    	    System.err.println("Error: Access denied to the socket streams.");
    	    System.err.println("Cause: Security manager restrictions prevented access to the input/output streams.");
    	    System.exit(1);
    	} catch (Exception e) {
    	    System.err.println("Unexpected error occurred while initializing streams: " + e.getMessage());
    	    e.printStackTrace(); // Log the stack trace for debugging.
    	    System.exit(1);
    	}
	}
	
	public void disconnect() {
		connectionManager.exit();
	}

	public Socket getSocket() {
		return connectionManager.getClientSoket();
	}

	public void startProgressTimer(long totalSize) {
	    // Update UI on JavaFX Application Thread
	    Platform.runLater(() -> {
		Load_Interfaces.startCircleProgress(totalSize);
	    });
		
	}
	
	public void updateProgressTimer(long currentSize, long timeOnePacket) {
	    // Update UI on JavaFX Application Thread
	    Platform.runLater(() -> {
		Load_Interfaces.updateCircleProgress(currentSize, timeOnePacket);
	    });
		
	}
	
	
}


