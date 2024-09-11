package com.garbage;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final ConcurrentSkipListMap<InetAddress,String> reachableIPs = new ConcurrentSkipListMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) throws UnknownHostException {
       
        Scanner sc = new Scanner(System.in);
        int myPort = 5684;

        System.out.println("Enter your name:");
        String name = sc.next();
        
        getAllUsers(name,myPort);
        // Start listening for incoming connections
        new Thread(() -> startServer(myPort)).start();
  
        MsgToPeer(myPort,name);
        
    }
    private static void MsgToPeer(int myPort,String name){
    	Scanner sc = new Scanner(System.in);
    	
    	String message;
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the IP to connect to a client or type List to get the IP of all connected clients");
        while(true) {
        	 try {
				message = consoleInput.readLine();
				
				if(message.equalsIgnoreCase("List")) {
					 if (reachableIPs.isEmpty()) {
		                    System.out.println("No clients are currently connected.");
		                } else {
		                    sendClientList();
		                }
				}
				else {
					InetAddress peerIp = InetAddress.getByName(message);
		            connectToPeer(peerIp, myPort, name);
					while(true) {
						message = consoleInput.readLine();
						if(message.equalsIgnoreCase("exit")) {
							out.println(name +" left the chat");
							break;
						}
						else
						{
							out.println(name + " : " + message);
						}
					}
				}
			} catch (IOException e) {
				System.out.println("Main: Peer disconnected the connection");
			}
        }
    }
    
    private static void getAllUsers(String name , int myPort) {
        //----------------------------------------------------------------------------------
        
        int TIMEOUT = 500;
        
        String subnet = "192.168.1."; // Change this to your subnet
        
        ExecutorService executorService = Executors.newFixedThreadPool(20); // Thread pool for concurrent scanning

        // Scan the subnet for reachable IPs
        for (int i = 1; i < 255; i++) {
            String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (address.isReachable(TIMEOUT)) {
                        // Try to establish a socket connection to the specified port
                        try (Socket socket = new Socket(address, myPort)) {
                            reachableIPs.put(address,name); // Add reachable IP to the set
                        } catch (IOException e) {
                            // Do nothing; the port is not open
                        }
                    }
                } catch (IOException e) {
                    // Handle unknown host or unreachable address
                }
            });
        }

        executorService.shutdown(); // Shutdown the executor service
        
        //----------------------------------------------------------------------------------
    }

    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Listening for incoming connections on port " + myPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();    
                
                // Start a new thread to handle messages from this client
                new Thread(() -> handleClientMessages(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Start server disconnected");
        }
    }
    
    

    private static void handleClientMessages(Socket clientSocket) {
    	 try (BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
             // Read the client's name first
//             String clientName = clientInput.readLine();
//             InetAddress clientAddress = clientSocket.getInetAddress();
//             reachableIPs.put(clientAddress, clientName); // Add the new client to the list
//             System.out.println("Client name set to: " + clientName);

            // Continue to read messages from this client
            String message;
            while ((message = clientInput.readLine()) != null) {
                if (message.equals("LIST")) {
                    sendClientList();
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("handleClientMessages Disconnected");
        }
    }

    private static void removeClient(Socket clientSocket) {
        reachableIPs.remove(clientSocket);
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
    	   for (Map.Entry<InetAddress, String> entry : reachableIPs.entrySet()) {
               InetAddress ip = entry.getKey();
               String deviceName = entry.getValue();
               System.out.println("IP Address: " + ip.getHostAddress() + " Name: " + deviceName);
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
