package com.garbage;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

class Client {
    private Socket socket;
    private String name;

    public Client(Socket socket, String name) {
        this.socket = socket;
        this.name = name;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

public class Peer {
    private static final ConcurrentHashMap<Socket, Client> allClients = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        InetAddress peerIp;
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your Port:");
        int myPort = sc.nextInt();
        System.out.println("Enter your name:");
        String name = sc.next();

        System.out.println("Connected clients:");
        for (Map.Entry<Socket, Client> entry : allClients.entrySet()) {
            Client client = entry.getValue();
            System.out.println("Name: " + client.getName() + ", IP: " + client.getSocket().getInetAddress().getHostAddress());
        }
        
        // Start listening for incoming connections
        new Thread(() -> startServer(myPort,name)).start();

        // Connect to another peer
        System.out.println("Do you want to connect to a peer:\nY = Yes\nN = No");
        String reply = sc.next();

        if (reply.equalsIgnoreCase("Y")) {
            System.out.println("Enter the Port of the peer to connect to:");
            int peerPort = sc.nextInt();
            System.out.println("Enter the IP address of the peer to connect to:");
            String getIP = sc.next();
            try {
                peerIp = InetAddress.getByName(getIP);
                connectToPeer(peerIp, peerPort, name);
            } catch (UnknownHostException e) {
                System.out.println("Not an IP");
            }
        } else if (reply.equalsIgnoreCase("N")) {
           
        }

        // Handle user input and send messages
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        String message;
        while (true) {
            try {
                message = consoleInput.readLine();
                if (!message.equals("exit")) {
                    if (out != null) {
                        out.println(name + " : " + message);
                    }
                } else {
                    break; // Exit the loop if user types "exit"
                }
            } catch (IOException e) {
                System.out.println("Main: Peer disconnected the connection");
                break; // Exit the loop on exception
            }
        }

        // Close resources
        try {
            if (socket != null) {
                socket.close();
            }
            sc.close();
        } catch (IOException e) {
            System.out.println("Error closing the socket.");
        }
    }

    private static void startServer(int myPort, String name) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Listening for incoming connections on port " + myPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Start a new thread to handle messages from this client
                new Thread(() -> handleClientMessages(clientSocket,name)).start();
            }
        } catch (IOException e) {
            System.out.println("Start server disconnected");
        }
    }

    private static void handleClientMessages(Socket clientSocket, String name) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            // Read the client's name first
            if ((message = clientIn.readLine()) != null) {
                // Create a new Client object with the name received
                Client newClient = new Client(clientSocket, message);
                allClients.put(clientSocket, newClient);
            }

            // Continue to read messages from this client
            while ((message = clientIn.readLine()) != null) {
                System.out.println(name+" : " + message);
            }
        } catch (IOException e) {
            System.out.println("handleClientMessages Disconnected");
        } finally {
            removeClient(clientSocket);
        }
    }

    private static void removeClient(Socket clientSocket) {
        allClients.remove(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing client socket.");
        }
        System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
    }

    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            System.out.println("Connected to peer: " + socket.getInetAddress().getHostAddress());

            // Send the user's name to the peer
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name); // Send the name first

            // Add the new client to the allClients map
            Client newClient = new Client(socket, name);
            allClients.put(socket, newClient);

            // Notify the peer of the new connection
            out.println(name + " has connected!");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to read incoming messages from the peer
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("connectToPeer Thread Disconnected");
                }
            }).start();

        } catch (IOException e) {
            System.out.println("Failed to connect to peer: " + e.getMessage());
        }
    }
}