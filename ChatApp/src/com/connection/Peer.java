package com.connection;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
//    private static final Map<Socket,String> allClients = new ConcurrentHashMap<>();
    private static final ArrayList<Socket> allClients = new ArrayList<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        System.out.println("Enter your Port:");
        int Myport = sc.nextInt();
        System.out.println("Enter your name");
        String name = sc.next();

        // Start listening for incoming connections
        new Thread(() -> startServer(Myport,name)).start();

        // Connect to another peer
        System.out.println("Enter the Port of the peer to connect to:");
        int peerPort = sc.nextInt();
        System.out.println("Enter the IP address of the peer to connect to:");
        String peerIp = sc.next();
        connectToPeer(peerIp, peerPort,name);

        // Handle user input and send messages
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        String message;
        while (true) {
            try {
                message = consoleInput.readLine();
                if (message != null) {
                    out.println(name+ " : "+message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void startServer(int Myport, String name) {
        try (ServerSocket serverSocket = new ServerSocket(Myport)) {
            System.out.println("Listening for incoming connections on port "+Myport);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientMessages(clientSocket,name)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientMessages(Socket clientSocket,String name) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            while ((message = clientIn.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected");
        }
    }

    private static void connectToPeer(String peerIp, int port,String name) {
        try {
            socket = new Socket(peerIp, port);
            
            System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());

            synchronized (allClients) {
                allClients.add(socket);
            }
            
//            for (Entry<Socket, String> entry : allClients.entrySet())  {
//                System.out.println("IP Address = " + entry.getKey() + 
//                                 ", Name = " + entry.getValue()); 
//            }
            
            for(Socket clients : allClients)
            	System.out.println(clients.getInetAddress().getHostAddress());
            
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to read incoming messages from the peer
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println("Peer: " + message);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected");
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
