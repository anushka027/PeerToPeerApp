package com.garbage;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    // Map to store reachable IPs and their corresponding names
	private static final ConcurrentHashMap<String, String> reachableIPs = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int myPort = 5684; // Port to listen on

        System.out.println("Enter your name:");
        String name = sc.next();

        getAllUser(name, myPort);
        
        // Start server to accept incoming connections
        new Thread(() -> startServer(myPort)).start();
        // Handle sending messages to peers
        MsgToPeer(myPort, name);
    }

    // Discover all users on the network
    public static void getAllUser(String name, int myPort) {
        // Discover reachable users on the network'
       
        String subnet = "192.168.1."; // Subnet for the local network
        ExecutorService executorService = Executors.newCachedThreadPool(); // Thread pool for network discovery
        // Iterate over possible IP addresses in the subnet
        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
//                    System.out.println("Loop");
                 if(isPortOpen(address)) {
                        if (!reachableIPs.containsKey(host)) {
                            reachableIPs.put(host, name);
                            System.out.println("Found reachable IP: " + host);
                        }
                    }
                } catch (IOException e) {
//                    System.out.println("Error checking reachability of " + host + ": " + e.getMessage());
                }
            });
        }

        executorService.shutdown(); // Shutdown executor service when done
      
    }
    
    public static boolean isPortOpen(InetAddress ipAddress) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, 5684), 1000); // 1-second timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Print list of reachable users
    public static void printListOfUsers() {
        System.out.println("Reachable IPs on the network:");
        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    // Handle messaging to peers
    private static void MsgToPeer(int myPort, String name) {
        Scanner sc = new Scanner(System.in);
        String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the IP to connect to a client or type List to get the IP of all connected clients");

        while (true) {
            try {
                message = consoleInput.readLine();
                if (message.equalsIgnoreCase("List")) {
                    if (reachableIPs.isEmpty()) {
                        System.out.println("No clients are currently connected.");
                    } else {
                        printListOfUsers();
                    }
                } else {
                    InetAddress peerIp = InetAddress.getByName(message);
                    connectToPeer(peerIp, myPort, name);
                    while (true) {
                        message = consoleInput.readLine();
                        if (message.equalsIgnoreCase("exit")) {
                            out.println(name + " left the chat");
                            break;
                        } else {
                            out.println(name + " : " + message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Main: Peer disconnected the connection");
            }
        }
    }

    // Start server to listen for incoming connections
    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Listening for incoming connections on port " + myPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected: " + clientIp);
                
                // Use thread pool to handle client connections
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Start server disconnected: " + e.getMessage());
        }
    }

    // Handle incoming messages from a client
    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
//            String clientName;
//            clientName = clientIn.readLine(); // Read client's name
//            String clientIp = clientSocket.getInetAddress().getHostAddress();
//                
//            
//            getAllUser(clientName, 5684);

            String message;
            while ((message = clientIn.readLine()) != null) {
                if (message.equalsIgnoreCase("LIST")) {
                    printListOfUsers();
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("handleClientMessages: Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // Remove client from reachable IPs when they disconnect
    private static void removeClient(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        if (reachableIPs.containsKey(clientIp)) {
            reachableIPs.remove(clientIp);
            System.out.println("Client disconnected: " + clientIp);
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }

    // Connect to a peer and start communication
    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            System.out.println("Connected to peer: " + socket.getInetAddress().getHostAddress());

            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(name); // Send the name first
            out.println(name + " has connected!");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("connectToPeer Thread Disconnected: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.out.println("Failed to connect to peer: " + e.getMessage());
        }
    }
}
