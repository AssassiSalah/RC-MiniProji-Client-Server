package rc_miniproj;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthenticatedFileServer {
    private final String ipAddress;
    private final int port;
    private static final int MAX_CLIENTS = 5;
    private Map<String, String> users;

    public AuthenticatedFileServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.users = new HashMap<>();
        getUsers();
    }

    private void getUsers() {
        users.put("user1", "pass1");
        users.put("user2", "pass2");
        users.put("user3", "pass3");
    }

    public void start() {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);

        try {
            InetAddress bindAddress = InetAddress.getByName(ipAddress);
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.bind(new InetSocketAddress(bindAddress, port));
                System.out.println("Server is running on IP: " + ipAddress + " Port: " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClient(clientSocket));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Shutting down the server.");
            executorService.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())) {

            String username = authenticate(dataInputStream);
            String clientAddress = clientSocket.getInetAddress().toString();
            System.out.println("Client connected: " + clientAddress + " with username: " + username);

            if (username.isEmpty()) {
                dataOutputStream.writeUTF("Authentication Failed");
                System.out.println("Authentication failed for " + clientAddress);
                return;
            }

            dataOutputStream.writeUTF("Authentication Successful. Welcome: " + username);
            System.out.println("Authentication successful for " + clientAddress + " (" + username + ")");

            // Create the user's directory for storing files
            File userDir = new File("server_storage/" + username);
            if (!userDir.exists()) userDir.mkdirs();

            while (true) {
                String command = dataInputStream.readUTF();
                System.out.println("Client (" + clientAddress + ") requested command: " + command);

                switch (command) {
                    case "UPLOAD":
                        receiveFile(userDir, dataInputStream, dataOutputStream, clientAddress);
                        break;
                    case "LIST_FILES_USERS":
                        listFiles(dataOutputStream, clientAddress);
                        break;
                    case "DOWNLOAD":
                        sendFile(dataInputStream, dataOutputStream, userDir, clientAddress);
                        break;
                    case "EXIT":
                        System.out.println("Client (" + clientAddress + ") disconnected.");
                        return;
                    default:
                        dataOutputStream.writeUTF("Invalid command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String authenticate(DataInputStream dataInputStream) throws IOException {
        String username = dataInputStream.readUTF();
        String password = dataInputStream.readUTF();

        if (users.containsKey(username) && users.get(username).equals(password)) {
            return username;
        }
        return "";
    }

    private void receiveFile(File userDir, DataInputStream dataInputStream, DataOutputStream dataOutputStream, String clientAddress) throws IOException {
        String fileName = dataInputStream.readUTF();
        long fileSize = dataInputStream.readLong();
        File file = new File(userDir, fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long bytesReceived = 0;
            while (bytesReceived < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
                System.out.println("Receiving " + fileName + ": " + (bytesReceived * 100 / fileSize) + "%");
            }
            dataOutputStream.writeUTF("File upload complete.");
            System.out.println("File " + fileName + " uploaded by client (" + clientAddress + ")");
        }
    }

    private void listFiles(DataOutputStream dataOutputStream, String clientAddress) throws IOException {
        // Get all user directories in the server_storage folder
        File serverStorageDir = new File("server_storage");//server_storage
        File[] userDirs = serverStorageDir.listFiles(File::isDirectory);  // Only directories (user folders)

        if (userDirs == null || userDirs.length == 0) {
            dataOutputStream.writeUTF("No users found.");
            return;
        }

        // Iterate through each user directory and list their files
        dataOutputStream.writeUTF("Available users and their files:");
        int userIndex = 1;
        for (File userDir : userDirs) {
            String username = userDir.getName();
            dataOutputStream.writeUTF("User " + userIndex + ": " + username);
            userIndex++;

            // List all files in the user's directory
            File[] files = userDir.listFiles(File::isFile);  // Only files (not subdirectories)
            if (files == null || files.length == 0) {
                dataOutputStream.writeUTF("No files found for " + username);
            } else {
                int fileIndex = 1;
                for (File file : files) {
                    dataOutputStream.writeUTF(fileIndex + ": " + file.getName());
                    fileIndex++;
                }
            }
        }
        dataOutputStream.writeUTF("END");
    }

    private void sendFile(DataInputStream dataInputStream, DataOutputStream dataOutputStream, File userDir, String clientAddress) throws IOException {
        // Step 1: Receive the index of the selected user
        int userIndex = dataInputStream.readInt() - 1;  // Convert 1-based index to 0-based
        File[] userDirs = new File("server_storage").listFiles(File::isDirectory);
        
        if (userIndex < 0 || userIndex >= userDirs.length) {
            dataOutputStream.writeUTF("Invalid user choice.");
            return;
        }

        File selectedUserDir = userDirs[userIndex];

        // Step 2: List the files in the selected user's directory
        File[] files = selectedUserDir.listFiles(File::isFile);  // Only files
        if (files == null || files.length == 0) {
            dataOutputStream.writeUTF("No files available for download.");
            return;
        }

        dataOutputStream.writeUTF("Available files for " + selectedUserDir.getName() + ":");
        for (int i = 0; i < files.length; i++) {
            dataOutputStream.writeUTF((i + 1) + ": " + files[i].getName());
        }
        dataOutputStream.writeUTF("END");

        // Step 3: Receive the selected file index from the client
        int fileIndex = dataInputStream.readInt() - 1;  // Convert 1-based index to 0-based

        if (fileIndex < 0 || fileIndex >= files.length) {
            dataOutputStream.writeUTF("Invalid file choice.");
            return;
        }

        File fileToSend = files[fileIndex];
        dataOutputStream.writeUTF("Starting file transfer.");
        dataOutputStream.writeLong(fileToSend.length());

        try (FileInputStream fileInputStream = new FileInputStream(fileToSend)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
        }

        dataOutputStream.writeUTF("File transfer complete.");
        System.out.println("Sent file: " + fileToSend.getName() + " to client (" + clientAddress + ")");
    }

    public static void main(String[] args) {
        new AuthenticatedFileServer("127.0.0.1", 5015).start();
    }
}
