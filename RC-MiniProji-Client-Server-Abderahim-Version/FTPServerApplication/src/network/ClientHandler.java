package network;

import controllers.ServerController;
import database.DatabaseHandler;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DatabaseHandler dbHandler;
    private final FTPServer server;
    private final ServerController controller;
    private BufferedReader reader;
    private PrintWriter writer;
    private DataInputStream dataInput;
    private DataOutputStream dataOutput;
    private String username = null;
    private static final String SHARED_FOLDER = "server_shared";
    private static final String CLIENTS_FOLDER = "clients";

    public ClientHandler(Socket socket, DatabaseHandler dbHandler, FTPServer server, ServerController controller) throws IOException {
        this.socket = socket;
        this.dbHandler = dbHandler;
        this.server = server;
        this.controller = controller;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.dataInput = new DataInputStream(socket.getInputStream());
        this.dataOutput = new DataOutputStream(socket.getOutputStream());

        // Ensure shared and clients folders exist
        Files.createDirectories(Paths.get(SHARED_FOLDER));
        Files.createDirectories(Paths.get(CLIENTS_FOLDER));
    }

    @Override
    public void run() {
        String clientInfo = socket.getInetAddress().getHostAddress();
        try {
            String command;
            while ((command = reader.readLine()) != null) {
                controller.logFileTransfer("Client " + clientInfo + ": " + command);
                handleCommand(command);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientInfo);
        } finally {
            disconnect();
            server.removeClient(this, clientInfo);
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ", 2);
        String action = parts[0];
        String parameter = parts.length > 1 ? parts[1] : "";

        try {
            switch (action) {
                case "REGISTER":
                    handleRegister(parameter);
                    break;
                case "LOGIN":
                    handleLogin(parameter);
                    break;
                case "LIST":
                    listFiles(parameter);
                    break;
                case "UPLOAD":
                    receiveFile(parameter);
                    break;
                case "DOWNLOAD":
                    sendFile(parameter);
                    break;
                case "REMOVE":
                    removeFile(parameter);
                    break;
                default:
                    writer.println("FAIL: Unknown command.");
            }
        } catch (Exception e) {
            writer.println("FAIL: Error processing command.");
            e.printStackTrace();
        }
    }

    private void handleRegister(String parameter) throws IOException {
        String[] credentials = parameter.split(" ", 2);
        if (credentials.length < 2) {
            writer.println("FAIL: Invalid registration command.");
            return;
        }
        String username = credentials[0];
        String password = credentials[1];

        try {
            if (dbHandler.registerUser(username, password)) {
                Files.createDirectories(Paths.get(CLIENTS_FOLDER, username));
                writer.println("SUCCESS");
            } else {
                writer.println("FAIL: Username already exists.");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogin(String parameter) {
        String[] credentials = parameter.split(" ", 2);
        if (credentials.length < 2) {
            writer.println("FAIL: Invalid login command.");
            return;
        }
        String username = credentials[0];
        String password = credentials[1];

        try {
            if (dbHandler.authenticateUser(username, password)) {
                this.username = username;
                writer.println("SUCCESS");
            } else {
                writer.println("FAIL: Invalid credentials.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void listFiles(String folderType) throws IOException {
        Path folder = resolveFolder(folderType);
        if (folder != null && Files.exists(folder) && Files.isDirectory(folder)) {
            StringBuilder fileList = new StringBuilder();
            Files.list(folder).forEach(path -> fileList.append(path.getFileName()).append("\n"));
            writer.println(fileList.toString().trim());
        } else {
            writer.println("ERROR: Folder not found.");
        }
    }

    private void receiveFile(String filePath) throws IOException {
        String[] parts = filePath.split("/", 2);
        if (parts.length < 2) {
            writer.println("FAIL: Invalid file path.");
            return;
        }
        Path folder = resolveFolder(parts[0]);
        if (folder == null) {
            writer.println("FAIL: Invalid folder.");
            return;
        }
        Path targetPath = folder.resolve(parts[1]);
        Files.createDirectories(targetPath.getParent());

        try (OutputStream out = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dataInput.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
        }
        writer.println("UPLOAD_SUCCESS");
    }

    private void sendFile(String filePath) throws IOException {
        String[] parts = filePath.split("/", 2);
        if (parts.length < 2) {
            writer.println("FAIL: Invalid file path.");
            return;
        }
        Path folder = resolveFolder(parts[0]);
        if (folder == null) {
            writer.println("FAIL: Invalid folder.");
            return;
        }
        Path targetPath = folder.resolve(parts[1]);
        if (Files.exists(targetPath)) {
            writer.println("DOWNLOAD_START");
            try (InputStream in = Files.newInputStream(targetPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    dataOutput.write(buffer, 0, bytesRead);
                }
            }
            writer.println("DOWNLOAD_SUCCESS");
        } else {
            writer.println("ERROR: File not found.");
        }
    }

    private void removeFile(String filePath) throws IOException {
        String[] parts = filePath.split("/", 2);
        if (parts.length < 2) {
            writer.println("FAIL: Invalid file path.");
            return;
        }
        Path folder = resolveFolder(parts[0]);
        if (folder == null) {
            writer.println("FAIL: Invalid folder.");
            return;
        }
        Path targetPath = folder.resolve(parts[1]);
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
            writer.println("REMOVE_SUCCESS");
        } else {
            writer.println("ERROR: File not found.");
        }
    }


    private Path resolveFolder(String folderType) {
        if (folderType.equalsIgnoreCase("SHARED")) {
            return Paths.get(SHARED_FOLDER);
        } else if (folderType.equalsIgnoreCase("USER") && username != null) {
            return Paths.get(CLIENTS_FOLDER, username);
        } else {
            return null;
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
