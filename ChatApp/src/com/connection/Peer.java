package com.connection;

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
                 if(isPortOpen(address)) {
                        if ((!reachableIPs.containsKey(host)))
                        {
                        	if(!host.equals(InetAddress.getLocalHost().getHostAddress())) 
                            reachableIPs.put(host, name);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error checking reachability of " + host + ": " + e.getMessage());
                }});}
        executorService.shutdown(); // Shutdown executor service when done
    }
    
    //Check the IPs present in the port
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
    	int i =1;
        System.out.println("Reachable IPs on the network:");
        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
//            System.out.println(i+ " - "+entry.getKey() + " : " + entry.getValue());
            System.out.println(i+ " - "+entry.getKey());
            i++;
        }
    }

    // Handle messaging to peers
    private static void MsgToPeer(int myPort, String name) {
        Scanner sc = new Scanner(System.in);
        String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        //Options to start communication
        System.out.println("\n--------LIST : To get list of online user.\n--------BROADCAST : Send message to all the user"
        		+ "\n--------ENTER IP : To directly connect to a user.");

        while (true) {
            try {
                message = consoleInput.readLine();
                //Display connected users
                if (message.equalsIgnoreCase("List")) {
                    getAllUser(name, myPort);
                    if (reachableIPs.isEmpty()) {
                        System.out.println("--------No clients are currently connected--------");
                    } else {
                        printListOfUsers();
                        System.out.println("-----------Choose a user from the list------------");
                    }
                    //broadcast msg to all Peers
                }else if(message.equalsIgnoreCase("Broadcast")) {
                	broadcast();
                }
                //
                else {
                	//Display result on IP or index of List
                    try {
                        int choice = Integer.parseInt(message);
                        if (choice >= 1 && choice <= reachableIPs.size()) {
                            Map.Entry<String, String>[] entries = reachableIPs.entrySet().toArray(new Map.Entry[0]);
                            String ipAddressString = entries[choice - 1].getKey();
                            connectToPeer(InetAddress.getByName(ipAddressString), myPort, name);
                            while (true) {
                                message = consoleInput.readLine();
                                if (message.equalsIgnoreCase("exit")) {
                                	System.out.println("----------You left the chat-----------");
                                    out.println(name + " left the chat");
                                    break;
                                } else {
                                    out.println(name + " : " + message);
                                }
                            }
                        } else {
                            System.out.println("********Invalid choice. Please enter a number from the list********");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("***********Invalid input. Please enter a number.***********");
                    }
                }
            } catch (IOException e) {
                System.out.println("***********Connection is disconnected***********");
            }
        }
    }
   //Broadcast msg to all the connected Peers
    public static void broadcast() {
        System.out.println("-----------Broadcast chat-----------");
        Scanner sc = new Scanner(System.in);
        getAllUser(name, myPort); //Update the current user map

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();

        if (reachableIPs.isEmpty()) {
            System.out.println("No users connected");
        } else {
        	//Message loop for broadcast chats
            while (true) {
                System.out.println("-------------Enter message ['exit' to stop]-------------");
                String message = sc.nextLine();
                for (Map.Entry<String, String> entry : entries) {
                    String ip = entry.getKey();
                    String userName = entry.getValue();
                    try {
                    	 if (message.equalsIgnoreCase("exit")) {
                             System.out.println("-------------You left the broadcast-------------");
                             out.println("-------------"+name + " left the chat-------------");
                             return;
                         }
                    	 else {
                        Socket socket = new Socket();
                        SocketAddress socketAddress = new InetSocketAddress(ip, myPort);
                        socket.connect(socketAddress, 1000); // 1-second timeout
                        out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(name + " : " + message);
                        socket.close();
                    	 }
                    } catch (IOException e) {
                        System.out.println("-------Failed to send message to " + userName + " (" + ip + "): " + e.getMessage()+"--------");
                    }
                }
            }
        }
    }

    // Start server to listen for incoming connections
    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)){ //Establish connection
            while (true) {
                Socket clientSocket = serverSocket.accept(); //Accepting connection request                
                // Use thread pool to handle client connections
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("-----------------This account is already in use-----------------");
        }
    }

//     Handle incoming messages from a Peer
    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            while ((message = clientIn.readLine()) != null) {
                if (message.equalsIgnoreCase("LIST")) {
                    printListOfUsers();
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
//            System.out.println("handleClientMessages: Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // Connect to a peer and start communication
    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            
            System.out.println("Connected");

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          
            out.println(name + " has connected!");
            // Notify the new peer about all current peers
  

//            String clientName;
//            clientName = in.readLine(); // Read client's name
//            
//            getAllUser(clientName, 5684);

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
//                    System.out.println("connectToPeer Thread Disconnected: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.out.println("Failed to connect to peer: " + e.getMessage());
        }
    }
}