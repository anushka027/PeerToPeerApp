package com.connection;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerServer {
    private ServerSocket server;
    private Map<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

    public PeerServer(int port) throws IOException {
        server = new ServerSocket(port);
        System.out.println("Peer server started on port: " + port);
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket clientSocket = server.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Request username from client
            out.println("Enter your username:");
            String username = in.readLine();

            if (username == null || username.trim().isEmpty() || clientHandlers.containsKey(username)) {
                out.println("Invalid or already taken username. Disconnecting...");
                clientSocket.close();
                return;
            }

            ClientHandler clientHandler = new ClientHandler(clientSocket, username, in, out);
            clientHandlers.put(username, clientHandler);

            // Notify all clients about the new client
            broadcastMessage(username + " has joined the chat");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("@")) {
                    // Handle direct message
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        String recipient = parts[0].substring(1);
                        sendMessageToClient(recipient, parts[1]);
                    }
                } else {
                    // Broadcast message
                    broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Handle client disconnection
            clientHandlers.values().removeIf(handler -> handler.socket == clientSocket);
            broadcastMessage("A client has left the chat");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler handler : clientHandlers.values()) {
            handler.out.println(message);
        }
    }

    private void sendMessageToClient(String username, String message) {
        ClientHandler handler = clientHandlers.get(username);
        if (handler != null) {
            handler.out.println(message);
        } else {
            // Notify sender that recipient was not found
            System.out.println("User " + username + " not found.");
        }
    }

    public void stop() throws IOException {
        server.close();
    }

    private static class ClientHandler {
        Socket socket;
        String username;
        BufferedReader in;
        PrintWriter out;

        ClientHandler(Socket socket, String username, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.username = username;
            this.in = in;
            this.out = out;
        }
    }
}
