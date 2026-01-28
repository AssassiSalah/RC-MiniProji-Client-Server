package model;

import application.AppConst;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ConnectionManager {

    private Socket socket;
    private boolean isConnected;
    
    ConnectionManager() {
    	isConnected = false;
    }

    public boolean connected() {
        if (isConnected) {
            System.out.println("Client is already connected.");
            return true;
        }
        try {
            // Initialize connection to the server
            socket = new Socket(AppConst.SERVER_ADDRESS, AppConst.SERVER_PORT);
            // dataInputStream = new DataInputStream(socket.getInputStream());
            // dataOutputStream = new DataOutputStream(socket.getOutputStream());
            isConnected = true;
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Error: Unknown host - " + AppConst.SERVER_ADDRESS);
            System.err.println("Cause: The host address cannot be resolved to an IP address.");
        }   catch (IllegalArgumentException e) {
            System.err.println("Invalid port number: " + AppConst.SERVER_PORT);
            System.err.println("Cause: Port numbers must be in the range 0 and 65535.");
        } catch (ConnectException e) {
            System.err.println("Error: Unable to connect to the server at " + AppConst.SERVER_ADDRESS + ":" + AppConst.SERVER_PORT);
            System.err.println("Cause: The server might not be running, or the port is unavailable.");
        } catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
            System.err.println("Cause: Socket issues (e.g., Connection Reset, Socket Closed Unexpectedly, or Other Networking Problems).");
        } catch (IOException e) {
            System.err.println("I/O error while setting up connection: " + e.getMessage());
            System.err.println("Cause: An error occurred while creating input/output streams or during the connection setup.");
        }
        
        return false;
    }

    public void exit(boolean waitForAuthontication) {
        if (!isConnected) {
            System.out.println("Client is already disconnected.");
            return;
        }
       // try {
            if (waitForAuthontication) {
                // Sending error messages for authentication failure
        //        dataOutputStream.writeUTF("Wrong Username");
        //        dataOutputStream.writeUTF("Wrong Password");

                // Reading response from client (authentication status)
        //        String response = dataInputStream.readUTF();
        //        System.out.println("Response from client: " + response);
            } else {
                // Sending an exit message if authentication is successful
        //        dataOutputStream.writeUTF("EXIT");
            }
     /*   } catch (EOFException e) {
            System.err.println("Error: Reached end of stream unexpectedly while reading data.");
            System.err.println("Cause: The client or server closed the connection prematurely.");
        } catch (SocketException e) {
            System.err.println("Socket error occurred: " + e.getMessage());
            System.err.println("Cause: Issues related to the socket, such as a closed socket or protocol errors.");
        } catch (IOException e) {
            System.err.println("I/O error occurred: " + e.getMessage());
            System.err.println("Cause: General input/output issues while reading or writing data.");
        } catch (NullPointerException e) {
            System.err.println("Stream not initialized: " + e.getMessage());
            System.err.println("Cause: Attempting to use dataInputStream or dataOutputStream if they are not properly initialized.");
        }
*/
        isConnected = false;
    }
    
    public Socket getClientSoket() {
    	return socket;
    }
}
