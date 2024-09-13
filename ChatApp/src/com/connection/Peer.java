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

        getAllUser(name, myPort);
        
        // Start server to accept incoming connections
        new Thread(() -> startServer(myPort)).start();
 
        // Handle sending messages to peers
        MsgToPeer(myPort, name);
    }

    // Discover all users on the network
    public static void getAllUser(String name, int myPort) {
        reachableIPs = new ConcurrentHashMap<>();
        // Discover reachable users on the network
       
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
                        if ((!reachableIPs.containsKey(host)))
                        {
                        	if(!host.equals(InetAddress.getLocalHost().getHostAddress())) 
                            reachableIPs.put(host, name);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error checking reachability of " + host + ": " + e.getMessage());
                }
            });
        }

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
    public static void printListOfUsers() throws UnknownHostException {
    	int i =1;
    	
        System.out.println("Reachable IPs on the network:");
        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
//        Set<Map.Entry<String, String>> filteredEntries = entries.stream()
//        	    .filter(entry -> !entry.getValue().equals(currentIp))
//        	    .collect(Collectors.toSet());
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
        System.out.println("\nLIST : To get list of online user.\nBROADCAST : Send message to all the user"
        		+ "\nENTER IP : To directly connect to a user.");

        while (true) {
            try {
                message = consoleInput.readLine();
                
                if (message.equalsIgnoreCase("List")) {
                    getAllUser(name, myPort);
                    if (reachableIPs.isEmpty()) {
                        System.out.println("No clients are currently connected.");
                    } else {
                        printListOfUsers();
                        System.out.println("-----------Choose a user from the list------------");
                    }
                }else if(message.equalsIgnoreCase("Broadcast")) {
                	broadcast();
                }
                
                else {
                    try {
                        int choice = Integer.parseInt(message);
                        if (choice >= 1 && choice <= reachableIPs.size()) {
                            Map.Entry<String, String>[] entries = reachableIPs.entrySet().toArray(new Map.Entry[0]);
                            String ipAddressString = entries[choice - 1].getKey();
                            connectToPeer(InetAddress.getByName(ipAddressString), myPort, name);
                            while (true) {
                                message = consoleInput.readLine();
                                if (message.equalsIgnoreCase("exit")) {
                                	System.out.println("You left the chat");
                                    out.println(name + " left the chat");
                                    break;
                                } else {
                                    out.println(name + " : " + message);
                                }
                            }
                        } else {
                            System.out.println("Invalid choice. Please enter a number from the list");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection is disconnected");
            }
        }
    }
   
    public static void broadcast() {
    	System.out.println("Broadcast chat");
    	Scanner sc = new Scanner(System.in);
    	getAllUser(name, myPort);
    	
         Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
         Iterator<Map.Entry<String, String>> iterator = entries.iterator();
         
         if(reachableIPs.isEmpty()) {
        	 System.out.println("No users connected");
         }
        	 
         else {
         while(iterator.hasNext()) {
        	 String message = sc.next();
        	 if(!message.equalsIgnoreCase("exit")) {
             Map.Entry<String, String> entry = iterator.next();
             try {
				connectToPeer(InetAddress.getByName(entry.getKey()), myPort, name);
				out.print(name+ " : " +message);
			} catch (UnknownHostException e) {
//				e.printStackTrace();
			}
        	 }
        	 else {
        		 System.out.println("You left the broadcast");
        		 break;
        	 }
         }
         }
    }

    // Start server to listen for incoming connections
    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
           
            while (true) {
                Socket clientSocket = serverSocket.accept();

                String clientIp = clientSocket.getInetAddress().getHostAddress();
                
                // Use thread pool to handle client connections
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("-------------This account is already in use-------------");
        }
    }

//     Handle incoming messages from a client
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
            System.out.println("Connected to peer: " + socket.getInetAddress().getHostAddress());

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