package com.connection;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;


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
       
        Scanner sc = new Scanner(System.in);
        int myPort = 5684;

        System.out.println("Enter your name:");
        String name = sc.next();
     
        // Start listening for incoming connections
        new Thread(() -> startServer(myPort)).start();
        
        MsgToPeer(myPort,name);
        
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
                    if (allClients.isEmpty()) {
                        System.out.println("No clients are currently connected.");
                    } else {
                        selectClientFromList(); // Use JLine to select IP
                    }
                } else {
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
						}					}
                }
            } catch (IOException e) {
                System.out.println("Main: Peer disconnected the connection");
            }
        }
    }

    private static void selectClientFromList() {
        try {
            Terminal terminal = TerminalBuilder.terminal();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Collect client IPs
            List<String> ipList = allClients.values().stream()
                    .map(Client::getIpAddress)
                    .toList();

            int cursor = 0;

            while (true) {
                terminal.writer().println("\033[H\033[2J"); // Clear console
                terminal.writer().flush();

                // Print the list with the cursor
                for (int i = 0; i < ipList.size(); i++) {
                    if (i == cursor) {
                        terminal.writer().println("> " + ipList.get(i)); // Highlight selected IP
                    } else {
                        terminal.writer().println("  " + ipList.get(i));
                    }
                }
                terminal.writer().flush();

                // Read user input
                String input = reader.readLine();
                if (input == null) {
                    break; // Exit on null input (Ctrl+D)
                }

                switch (input) {
                    case "\u001B[A": // Up arrow
                        cursor = (cursor - 1 + ipList.size()) % ipList.size();
                        break;
                    case "\u001B[B": // Down arrow
                        cursor = (cursor + 1) % ipList.size();
                        break;
                    case "\n":
                    case "\r": // Enter key
                        String selectedIp = ipList.get(cursor);
                        System.out.println("Selected IP: " + selectedIp);
                        connectToPeer(InetAddress.getByName(selectedIp), 5684, "YourName"); // Adjust port and name as needed
                        return; // Exit after selection
                }
            }
        } catch (IOException e) {
            System.out.println("Error selecting client from list");
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
            while ((message = clientIn.readLine()) != "exit") {
                if (message.equals("LIST")) {
                	selectClientFromList();
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
                    while ((message = in.readLine()) != "exit") {
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
