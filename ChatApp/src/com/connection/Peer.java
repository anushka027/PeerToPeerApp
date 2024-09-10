package com.connection;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

class Client {
    private String ipAddress;
    private String name;

    public Client(String ipAddress, String name) {
        this.ipAddress = ipAddress;
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
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
        int myPort = 5684;

        System.out.println("Enter your name:");
        String name = sc.next();
     
        // Start listening for incoming connections
        new Thread(() -> startServer(myPort)).start();
        
        String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the IP to connect to a client or type List to get the IP of all connected clients");
        while(true) {
        	 try {
				message = consoleInput.readLine();
				
				if(message.equalsIgnoreCase("List")) {
					 if (allClients.isEmpty()) {
		                    System.out.println("No clients are currently connected.");
		                } else {
		                    sendClientList();
		                }
				}
				else {
					System.out.println("Enter the IP you want to send this message to");
					String getIP = sc.next();
			        try { 
			            peerIp = InetAddress.getByName(getIP);
			            connectToPeer(peerIp, myPort, name);
			            out.println(name + " : " + message);
			        } catch (UnknownHostException e) {
			        	System.out.println("Invalid IP");
			        	continue;
			        }
				}
			} catch (IOException e) {
				System.out.println("Main: Peer disconnected the connection");
			}
        }
    }
    

    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Listening for incoming connections on port " + myPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Start a new thread to handle messages from this client
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Start server disconnected");
        }
    }

    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String getName;
            // Read the client's name first
            if ((getName = clientIn.readLine()) != null) {
                Client newClient = new Client(clientSocket.getInetAddress().getHostAddress(), getName);
                allClients.put(clientSocket, newClient);
//                broadcastClientList();  // Notify all clients about the new connection
            }

            // Continue to read messages from this client
            String message;
            while ((message = clientIn.readLine()) != null) {
                if (message.equals("LIST")) {
                    sendClientList();
                } else {
                    System.out.println(message);
                }
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
//        broadcastClientList(); // Notify all clients about the disconnection
    }

//    private static void broadcastClientList() {
//        for (Map.Entry<Socket, Client> entry : allClients.entrySet()) {
//            Socket socket = entry.getKey();
//            try {
//                PrintWriter clientOut = new PrintWriter(socket.getOutputStream(), true);
//                sendClientList(clientOut);
//            } catch (IOException e) {
//                System.out.println("Error broadcasting client list to " + entry.getValue().getIpAddress());
//            }
//        }
//    }

    private static void sendClientList() {
        for (Client client : allClients.values()) {
            System.out.println("IP: " + client.getIpAddress() + ", Name: " + client.getName());
        }
    }

    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            System.out.println("Connected to peer: " + socket.getInetAddress().getHostAddress());

            // Send the user's name to the peer
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name); // Send the name first

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
