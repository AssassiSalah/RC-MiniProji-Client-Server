package rc_miniproj;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiClientFileServer {

    private static final int PORT = 5001;
    private static final int MAX_CLIENTS = 3; // Limit to 3 concurrent clients

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a new thread
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
    	// Create the server_storage directory if it doesn't exist
    	File storageDir = new File("server_storage");
    	if (!storageDir.exists()) {
    	    storageDir.mkdir();
    	}

        try (
            InputStream in = clientSocket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in)
        ) {
            // Read the file name first
            String fileName = dataInputStream.readUTF();
            System.out.println("Receiving file: " + fileName);

            // Create output stream for file
            File receivedFile = new File("server_storage/" + fileName);
            try (FileOutputStream fileOut = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                // Read file data from client and save it
                while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }

                System.out.println("File " + fileName + " received successfully.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
