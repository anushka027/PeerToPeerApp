package com.garbage;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {

    private static ConcurrentHashMap<String, String> reachableIPs = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String name;
    private static int myPort;
    private static boolean isConnected = false; // Flag to track connection status

    public static void main(String[] args) throws UnknownHostException {
        Scanner sc = new Scanner(System.in);
        myPort = 5684; // Port to listen on

        System.out.println("Enter your name:");
        name = sc.next();

        getAllUser(name, myPort);
        new Thread(() -> startServer(myPort)).start();
        MsgToPeer(myPort, name);
    }

    public static void getAllUser(String name, int myPort) {
        reachableIPs = new ConcurrentHashMap<>();
        String subnet = "192.168.1.";
        ExecutorService executorService = Executors.newCachedThreadPool();

        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (isPortOpen(address)) {
                        try (Socket socket = new Socket(address, myPort)) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out.println("NAME_REQUEST");
                            String peerName = in.readLine();
                            if (peerName != null && !peerName.equals(name) && !host.equals(InetAddress.getLocalHost().getHostAddress())) {
                                reachableIPs.put(host, peerName);
                            }
                        } catch (IOException e) {
                            System.out.println("Error querying peer at " + host + ": " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error checking reachability of " + host + ": " + e.getMessage());
                }
            });
        }
        executorService.shutdown();
    }

    public static boolean isPortOpen(InetAddress ipAddress) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, 5684), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void printListOfUsers() {
        int i = 1;
        System.out.println("\n------------------- ONLINE USERS --------------------");
        System.out.println("---------- CHOOSE AN OPTION FROM THE LIST -----------");
        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println(i + ". " + entry.getValue() + " ( " + entry.getKey() + " )");
            i++;
        }
    }

    private static void GuideMsg() {
        System.out.println("*****************************************************");
        System.out.println("****************** CHOOSE AN OPTION *****************");
        System.out.println("************** 1. List of online user ***************");
        System.out.println("************** 2. Broadcast a message ***************");
        System.out.println("*****************************************************");
    }

    private static void MsgToPeer(int myPort, String name) {
        Scanner sc = new Scanner(System.in);
        String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

        GuideMsg();

        while (true) {
            try {
                while (true) {
                    String choice = sc.nextLine();
                    switch (choice) {
                        case "1": {
                            getAllUser(name, myPort);
                            if (reachableIPs.isEmpty()) {
                                System.out.println("-------- No clients are currently connected ---------\n");
                            } else {
                               
//                                while(true) {
                                printListOfUsers();
                                String position = sc.nextLine().trim(); 

                                if (position.isEmpty()) { 
                                    System.out.println("***** Input cannot be empty *****\n");
                                    GuideMsg();
                                    break;
                                }
//                                }
                                try {
                                    int userPosition = Integer.parseInt(position); 
                                    if (userPosition >= 1 && userPosition <= reachableIPs.size()) {
                                        Map.Entry<String, String>[] entries = reachableIPs.entrySet().toArray(new Map.Entry[0]);
                                        String ipAddressString = entries[userPosition - 1].getKey();
                                        connectToPeer(InetAddress.getByName(ipAddressString), myPort, name);
                                        System.out.println("---- CONNECTED TO " + entries[userPosition - 1].getValue().toUpperCase() + " ----");
                                        System.out.println("-------- Enter message or '/exit' to go back --------");
                                        while (true) {
                                            String prompt = name + " : ";
                                            System.out.print(prompt);
                                            message = consoleInput.readLine();

                                            if (message.equalsIgnoreCase("/exit")) {
                                                System.out.println("----------------- You left the chat -----------------\n");
                                                if (isConnected) {
                                                    out.println("**** " + name + " left the chat ****\n");
                                                }
                                                break;
                                            } else if (isConnected) {
                                                out.println(name + " : " + message);
                                            } else {
                                                System.out.println("Cannot send message, not connected.");
                                                break;
                                            }
                                        }
                                    } else {
                                        System.out.println("***** User not present *****\n");
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("***** Invalid input, please enter a number *****\n"); // Handle invalid input
                                }
                                GuideMsg();
                            }
                        }
                        break;
                        case "2": {
                            broadcast();
                            GuideMsg();
                        }
                        break;
                        case "" : GuideMsg(); 
                        break;
                        default:
                            System.out.println("********* INVALID OPTION *********");
                    }
                }
            } catch (IOException e) {
                System.out.println("*****************************************************");
                System.out.println("************ Connection is disconnected *************");
                System.out.println("*****************************************************");
            }
        }
    }

    public static void broadcast() {
        System.out.println("---------------- BROADCAST STARTED ------------------\n");
        Scanner sc = new Scanner(System.in);
        getAllUser(name, myPort);

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();

        if (reachableIPs.isEmpty()) {
            System.out.println("xxxxxxxxxxxxxxxx No users connected xxxxxxxxxxxxxxxx\n");
        } else {
            System.out.println("-------- Enter message or '/exit' to go back --------");
            while (true) {
                System.out.print(name + "(You) : ");
                String message = sc.nextLine();
                for (Map.Entry<String, String> entry : entries) {
                    String ip = entry.getKey();
                    String userName = entry.getValue();
                    try {
                        if (message.equalsIgnoreCase("/exit")) {
                            System.out.println("-------------- You left the broadcast --------------\n");
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

    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("xxxxxxxxxx This account is already in use xxxxxxxxxx");
        }
    }

    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = clientIn.readLine()) != null) {
                if ("NAME_REQUEST".equals(message)) {
                    clientOut.println(name);
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true; // Set connection status to true

            out.println("**** " + name + " has connected ****");

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("\n***** User Offline *****");
                    cleanUp();
                }
            }).start();

        } catch (IOException e) {
            System.out.println("xxxxxx Failed to connect to peer: " + e.getMessage() + "xxxxxx");
        }
    }

    private static void cleanUp() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
            isConnected = false; // Reset connection status
        }
    }
}
