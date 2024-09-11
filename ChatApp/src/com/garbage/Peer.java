package com.garbage;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
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
    private static final ConcurrentSkipListMap<String, String> reachableIPs = new ConcurrentSkipListMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int myPort = 5684;

        System.out.println("Enter your name:");
        String name = sc.next();

        getAllUser(name, myPort);
        new Thread(() -> startServer(myPort)).start();
        MsgToPeer(myPort, name);
    }

    public static void getAllUser(String name, int myPort) {
        int TIMEOUT = 100;
        String subnet = "192.168.1."; // Change this to your subnet
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        for (int i = 1; i < 255; i++) {
            String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if(!reachableIPs.containsKey(address)) {
                    if (address.isReachable(TIMEOUT)) {
                        try (Socket socket = new Socket(address, myPort)) {
                            reachableIPs.put(host, name); // Use host (IP as String) as key
                            System.out.println("Found reachable IP: " + host);
                        } catch (IOException e) {
                            // Port is not open
                        }
                    }
                    }
                } catch (IOException e) {
                    System.out.println("Unknown host");
                }
            });
        }

        executorService.shutdown();
    }

    public static void printListOfUsers() {
    	
        System.out.println("Reachable IPs on the network:");
        Set s2 = reachableIPs.entrySet();
		Iterator itr3 = s2.iterator();
		while(itr3.hasNext()) {
			Map.Entry o1 = (java.util.Map.Entry) itr3.next();
			System.out.println(o1.getKey()+" : "+o1.getValue());
		}
    }

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

    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Listening for incoming connections on port " + myPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected: " + clientIp);
                
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Start server disconnected: " + e.getMessage());
        }
    }

    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String clientName;
            clientName = clientIn.readLine();
            String clientIp = clientSocket.getInetAddress().getHostAddress();
                
            getAllUser(clientName, 5684);

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
