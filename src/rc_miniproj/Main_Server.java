package rc_miniproj;

import java.util.Scanner;

public class Main_Server {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt user for IP and port
        System.out.print("Enter server IP (default: 127.0.0.1): ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "192.168.1.7"; // Default IP

        int port = 0;
        while (port <= 0 || port > 65535) {
            System.out.print("Enter server port (default: 5015): ");
            String portInput = scanner.nextLine().trim();
            if (portInput.isEmpty()) port = 1238; // Default port
            else {
                try {
                    port = Integer.parseInt(portInput);
                    if (port <= 0 || port > 65535) {
                        System.out.println("Invalid port. Please enter a value between 1 and 65535.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number. Please enter a numeric value.");
                }
            }
        }

        System.out.println("Starting server on IP: " + ip + " Port: " + port);
        AuthenticatedFileServer server = new AuthenticatedFileServer(ip, port);
        server.start();
    }
}
