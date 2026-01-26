package network;

import controllers.ServerController;
import database.DatabaseHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FTPServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> connectedClients = new ArrayList<>();
    private final DatabaseHandler dbHandler;
    private final ServerController controller;

    public FTPServer(int port, String dbPath, ServerController controller) throws IOException, SQLException {
        this.serverSocket = new ServerSocket(port);
        this.dbHandler = new DatabaseHandler(dbPath);
        this.controller = controller;
    }

    // Start the server
    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientInfo = clientSocket.getInetAddress().getHostAddress();
                controller.addClient(clientInfo);

                ClientHandler handler = new ClientHandler(clientSocket, dbHandler, this, controller);
                connectedClients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server stopped.");
        }
    }

    // Remove a disconnected client
    public void removeClient(ClientHandler handler, String clientInfo) {
        connectedClients.remove(handler);
        controller.removeClient(clientInfo);
    }

    // Stop the server
    public void stop() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        for (ClientHandler client : connectedClients) {
            client.disconnect();
        }
    }
}
