package rc_miniproj;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AuthenticatedFileClient {
    private String serverAddress;
    private int serverPort;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Scanner scanner;

    public AuthenticatedFileClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            System.out.println("Connected to the server at " + serverAddress + ":" + serverPort);

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            if (!authenticate()) return;

            while (true) {
                System.out.print("\nEnter command (UPLOAD, LIST_FILES_USERS, DOWNLOAD, EXIT): ");
                String command = scanner.nextLine().trim();
                dataOutputStream.writeUTF(command);

                switch (command.toUpperCase()) {
                    case "UPLOAD":
                        uploadFile();
                        break;
                    case "LIST_FILES_USERS":
                        listFiles();
                        break;
                    case "DOWNLOAD":
                        downloadFile();
                        break;
                    case "EXIT":
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid command.");
                }

                System.out.print("\nWould you like to perform another action? (y/n): ");
                String continueResponse = scanner.nextLine().trim();
                if (!continueResponse.equalsIgnoreCase("y")) {
                    System.out.println("Goodbye!");
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate() throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        dataOutputStream.writeUTF(username);
        dataOutputStream.writeUTF(password);

        String response = dataInputStream.readUTF();
        if (response.equals("Authentication Failed")) {
            System.out.println("Authentication failed!");
            return false;
        }

        System.out.println(response);
        return true;
    }

    private void uploadFile() throws IOException {
        System.out.print("Enter the file path to upload: ");
        String filePath = scanner.nextLine();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.writeLong(file.length());

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("File uploaded successfully.");
    }

    private void listFiles() throws IOException {
        String response = dataInputStream.readUTF();
        if ("No users found.".equals(response)) {
            System.out.println("No users found.");
        } else {
            while (!response.equals("END")) {
                System.out.println(response);
                response = dataInputStream.readUTF();
            }
        }
    }
    private void downloadFile() throws IOException {
        // Step 1: Fetch the list of users and display them with indexing
        dataOutputStream.writeUTF("LIST_FILES_USERS");
        String response = dataInputStream.readUTF();

        if ("No users found.".equals(response)) {
            System.out.println("No users found.");//No users found.
            return;
        }

        // Display the list of users
        System.out.println("Available users and their files:");
        while (!response.equals("END")) {
            System.out.println(response);
            response = dataInputStream.readUTF();
        }
        System.out.println(); // Add a newline for better readability

        // Step 2: Ask the client to select a user by index
        System.out.print("Select a user by index to view their files: ");
        int userChoice;
        try {
            userChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        dataOutputStream.writeInt(userChoice);

        // Step 3: Fetch and display the list of files for the selected user
        response = dataInputStream.readUTF();
        if (response.equals("Invalid user choice.")) {
            System.out.println("Invalid user choice.");
            return;
        }

        System.out.println("Files available for download:");
        while (!response.equals("END")) {
            System.out.println(response);
            response = dataInputStream.readUTF();
        }
        System.out.println(); // Add a newline for better readability

        // Step 4: Ask the client to select a file by index
        System.out.print("Enter the file number to download: ");
        int fileChoice;
        try {
            fileChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        dataOutputStream.writeInt(fileChoice);

        // Step 5: Start the file download process
        response = dataInputStream.readUTF();
        if (response.equals("Invalid file choice.")) {
            System.out.println("Invalid file choice.");
            return;
        } else if (response.equals("Starting file transfer.")) {
            long totalBytes = dataInputStream.readLong();

            // Prepare the download folder
            File downloadFolder = new File("downloads");
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }

            System.out.println("Starting the file download... Please wait until it's finished.");

            // Receive the file
            try (FileOutputStream fileOut = new FileOutputStream("downloads/" + dataInputStream.readUTF())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long bytesReceived = 0;
                while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    bytesReceived += bytesRead;
                    int progress = (int) ((bytesReceived * 100) / totalBytes);
                    System.out.print("\rReceiving file: " + progress + "%");
                }

                response = dataInputStream.readUTF();
                if (response.equals("File transfer complete.")) {
                    System.out.println("\nFile downloaded successfully.");
                } else {
                    System.out.println("\nUnexpected response after file transfer: " + response);
                }
            }
        } else {
            System.out.println("Unexpected response from server: " + response);
        }
    }


    public static void main(String[] args) {
        new AuthenticatedFileClient("127.0.0.1", 5015).start();
    }
}
