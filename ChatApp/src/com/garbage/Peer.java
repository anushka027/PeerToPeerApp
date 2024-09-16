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

public class Peer {
    // Map to store reachable IPs and their corresponding names
    private static ConcurrentHashMap<String, String> reachableIPs = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String name;
    private static int myPort;

    public static void main(String[] args) throws UnknownHostException {
        Scanner sc = new Scanner(System.in);
        myPort = 5684; // Port to listen on

        System.out.println("Enter your name:");
        name = sc.next();

        getAllUser(name, myPort); // Fetch IPs of all online users
        new Thread(() -> startServer(myPort)).start(); // Start server to accept incoming connections
        MsgToPeer(myPort, name); // Handle sending messages to peers
    }

    // Discover all users on the network
    public static void getAllUser(String name, int myPort) {
        reachableIPs = new ConcurrentHashMap<>();
        String subnet = "192.168.1."; // Subnet for the local network
        ExecutorService executorService = Executors.newCachedThreadPool(); // Thread pool for network discovery
        
        // Iterate over possible IP addresses in the subnet
        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (isPortOpen(address)) {
                        try (Socket socket = new Socket(address, myPort)) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out.println("NAME_REQUEST"); // Request the peer's name
                            String peerName = in.readLine(); // Read the peer's name
                            if (peerName != null && !peerName.equals(name) && !host.equals(InetAddress.getLocalHost().getHostAddress())) {
                                reachableIPs.put(host, peerName);
                            }
                        } catch (IOException e) {
//                            System.out.println("Error querying peer at " + host + ": " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error checking reachability of " + host + ": " + e.getMessage());
                }
            });
        }
        executorService.shutdown(); // Shutdown executor service when done
    }
    
    // Check if the port is open on the given IP address
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
        int i = 1;
        System.out.println("Reachable IPs on the network:");
        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            System.out.println(i + " - " + entry.getKey() + " : " + entry.getValue());
            i++;
        }
    }

    // Handle messaging to peers
    private static void MsgToPeer(int myPort, String name) {
        Scanner sc = new Scanner(System.in);
        String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("*****************************************************");
        System.out.println("********* LIST : To get list of online users *********");
        System.out.println("***** BROADCAST : Send message to all the users ******");
        System.out.println("*****************************************************");

        while (true) {
            try {
                message = consoleInput.readLine();
                if (message.equalsIgnoreCase("List")) {
                    getAllUser(name, myPort);
                    if (reachableIPs.isEmpty()) {
                        System.out.println("-------- No clients are currently connected ---------");
                    } else {
                        printListOfUsers();
                        System.out.println("------------ Choose a user from the list ------------");
                    }
                } else if (message.equalsIgnoreCase("Broadcast")) {
                    broadcast();
                } else {
                    try {
                        int choice = Integer.parseInt(message);
                        if (choice >= 1 && choice <= reachableIPs.size()) {
                            Map.Entry<String, String>[] entries = reachableIPs.entrySet().toArray(new Map.Entry[0]);
                            String ipAddressString = entries[choice - 1].getKey();
                            connectToPeer(InetAddress.getByName(ipAddressString), myPort, name);
                            while (true) {
                                message = consoleInput.readLine();
                                if (message.equalsIgnoreCase("/exit")) {
                                    System.out.println("----------------- You left the chat -----------------");
                                    out.println("**** " + name + " left the chat ****");
                                    break;
                                } else {
                                    out.println(name + " : " + message);
                                }
                            }
                        } else {
                            System.out.println("---------- CHOOSE AN OPTION FROM THE LIST -----------");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("---------- CHOOSE AN OPTION FROM THE LIST -----------");
                    }
                }
            } catch (IOException e) {
                System.out.println("*****************************************************");
                System.out.println("************ Connection is disconnected *************");
                System.out.println("*****************************************************");
            }
        }
    }

    // Broadcast message to all the connected peers
    public static void broadcast() {
        System.out.println("----------------- BROADCAST STARTED -----------------");
        Scanner sc = new Scanner(System.in);
        getAllUser(name, myPort); // Update the current user map

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();

        if (reachableIPs.isEmpty()) {
            System.out.println("No users connected");
        } else {
            while (true) {
                System.out.println("---------- Enter message ['/exit' to stop] ----------");
                String message = sc.nextLine();
                for (Map.Entry<String, String> entry : entries) {
                    String ip = entry.getKey();
                    String userName = entry.getValue();
                    try {
                        if (message.equalsIgnoreCase("/exit")) {
                            System.out.println("-------------- You left the broadcast --------------");
                            // Note: The `out` PrintWriter here is for the current connection, not all connections.
                            // Broadcasting should be done in a loop for each connection.
                            // Instead, just stop the loop and exit the broadcast mode.
                            return;
                        } else {
                            try (Socket socket = new Socket(ip, myPort)) {
                                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                out.println(name + " : " + message);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("------- Failed to send message to " + userName + " (" + ip + "): " + e.getMessage() + " --------");
                    }
                }
            }
        }
    }

    // Start server to listen for incoming connections
    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("---------- This account is already in use ----------");
        }
    }

    // Handle incoming messages from a peer
    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = clientIn.readLine()) != null) {
//                if ("NAME_REQUEST".equals(message)) {
//                    clientOut.println(name); // Respond with your name
//                } else {
                    System.out.println(message);
//                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // Connect to a peer and start communication
    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("**** " + name + " has connected ****");

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("Connection to peer lost: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.out.println("Failed to connect to peer: " + e.getMessage());
        }
    }
}
