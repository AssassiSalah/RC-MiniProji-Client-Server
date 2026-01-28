package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ConnectionManager {
	
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;	
	private Socket socket;
	
	String connected() {
        try {
            // Initialize connection to the server
            socket = new Socket(AppConstants.SERVER_ADDRESS, AppConstants.SERVER_PORT);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            return "";
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
	
	void exit(boolean waitForAuthontication) throws IOException {
		
		if(waitForAuthontication) {
			dataOutputStream.writeUTF("Wrong Userame");
			dataOutputStream.writeUTF("Wrong Password");
			dataInputStream.readUTF();
		} else
			dataOutputStream.writeUTF("EXIT");
	}
	
	public DataInputStream getInputStream() {
        return dataInputStream;
    }

    public DataOutputStream getOutputStream() {
        return dataOutputStream;
    }

}
