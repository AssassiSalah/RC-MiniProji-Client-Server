package rc_miniproj;

import java.io.*;
import java.net.Socket;

public class FileClient {
    private static final String SERVER_ADDRESS = "localhost"; // Server IP address
    private static final int SERVER_PORT = 5001; // Server port

    public static void main(String[] args) {
        String filePath = "C:/Users/Salah Eddine/Desktop/test.txt";

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             FileInputStream fileInputStream = new FileInputStream(filePath);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

            // Send file name
            File file = new File(filePath);
            dataOutputStream.writeUTF(file.getName());

            // Send file data
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File " + file.getName() + " sent to server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
