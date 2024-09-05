//package com.connection;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.InetAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
//public class CommonServer {
//
//    private static ServerSocket server;
//    private static final List<Socket> allClients = new ArrayList<>();
//    private static final ConcurrentMap<Socket, PrintWriter> clientWriters = new ConcurrentHashMap<>();
//    private static boolean running = true; // Server running flag
//
//    public CommonServer(int port) {
//        try {
//            server = new ServerSocket(port, 50, InetAddress.getLocalHost());
//            System.out.println("Server started on port: " + port);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void start() {
//        Thread acceptThread = new Thread(() -> {
//            while (running) {
//                try {
//                    // Accept a new client connection
//                    Socket clientSocket = server.accept();
//                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
//
//                    synchronized (allClients) {
//                        allClients.add(clientSocket);
//                    }
//
//                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
//                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//
//                    clientWriters.put(clientSocket, out);
//
//                    Thread clientThread = new Thread(() -> handleClientMessages(clientSocket, in));
//                    clientThread.start();
//                } catch (IOException e) {
//                    if (running) { // Only log if server is still running
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        acceptThread.start();
//
//        // Ensure the accept thread joins to keep server running
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("Shutting down server...");
//            running = false;
//            try {
//                server.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            synchronized (allClients) {
//                for (Socket socket : allClients) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }));
//
//        try {
//            acceptThread.join(); // Wait for the accept thread to finish
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void handleClientMessages(Socket clientSocket, BufferedReader in) {
//        try {
//            String message;
//            while ((message = in.readLine()) != null) {
//                System.out.println("Message from client: " + message);
//                broadcastMessage(clientSocket, message);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            removeClient(clientSocket);
//        }
//    }
//
//    private void broadcastMessage(Socket senderSocket, String message) {
//        synchronized (allClients) {
//            for (Socket clientSocket : allClients) {
//                if (!clientSocket.equals(senderSocket)) {
//                    PrintWriter out = clientWriters.get(clientSocket);
//                    if (out != null) {
//                        out.println(message);
//                    }
//                }
//            }
//        }
//    }
//
//    private void removeClient(Socket clientSocket) {
//        synchronized (allClients) {
//            allClients.remove(clientSocket);
//        }
//        clientWriters.remove(clientSocket);
//        try {
//            clientSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
//    }
//
//    public static void main(String[] args) {
//        CommonServer server = new CommonServer(12345);
//        server.start();
//    }
//}
