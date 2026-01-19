package rc_miniproj;

import java.util.Scanner;

public class Main_Client {
    public static void main(String[] argv) {
        Scanner scanner = new Scanner(System.in);

        // Prompt user for server IP and port
        System.out.print("Enter server IP (default: 127.0.0.1): ");
        String serverIp = scanner.nextLine().trim();
        if (serverIp.isEmpty()) {
            serverIp = "192.168.1.7"; // Default IP
        }

        int serverPort = 0;
        
        while (serverPort <= 0 || serverPort > 65535) {
            System.out.print("Enter server port (default: 5015): ");
            String portInput = scanner.nextLine().trim();
            if (portInput.isEmpty()) {
                serverPort = 1238; // Default port
            } else {
                try {
                    serverPort = Integer.parseInt(portInput);
                    if (serverPort <= 0 || serverPort > 65535) {
                        System.out.println("Invalid port. Please enter a value between 1 and 65535.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number. Please enter a numeric value.");
                }
            }
        }

        System.out.println("Connecting to server at IP: " + serverIp + " Port: " + serverPort);
        AuthenticatedFileClient client = new AuthenticatedFileClient(serverIp, serverPort);
        client.start();
    }
}
