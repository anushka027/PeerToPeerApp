package com.connection;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;

public class Peer {

    // Map to store reachable IPs and their corresponding names
    private static ConcurrentHashMap<String, String> reachableIPs = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out; // For sending messages
    private static BufferedReader in; // For receiving messages
    private static String name; // User's name
    private static int myPort; // Port to listen on
    private static boolean isConnected = false; // Flag to track connection status
    private static InnerThread innerThread; // Class-level variable for InnerThread

    public static void main(String[] args) {
        AnsiConsole.systemInstall(); // Install ANSI console for colored output

        myPort = 5684; // Port to listen on
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nENTER YOUR NAME :").reset());
        
        // Loop until a valid name is entered
        while (true) {
            Scanner sc = new Scanner(System.in);
            try {
                name = sc.nextLine().trim();
                // Validate the name (only alphabets and spaces allowed)
                if (isValidName(name)) {
                    getAllUser(name, myPort); // Get reachable peers
                    break; // Exit loop if valid name
                } else {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("Invalid name. Please enter a valid name.").reset());
                }
            } catch (NoSuchElementException e1) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("Invalid name. Please enter a valid name.").reset());
            } catch (Exception e) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("An unexpected error occurred: " + e.getMessage()).reset());
                break;
            }
        }

        // Start server thread
        new Thread(() -> startServer(myPort)).start();
        MsgToPeer(myPort, name); // Start messaging functionality
    }

    // Validate the name using regex
    private static boolean isValidName(String name) {
        Pattern pattern = Pattern.compile("^[a-zA-Z]+( [a-zA-Z]+)*$");
        return pattern.matcher(name).matches();
    }

    // Discover all reachable users in the local network
    public static void getAllUser(String name, int myPort) {
        reachableIPs = new ConcurrentHashMap<>();
        String subnet = "192.168.1."; // Local subnet
        ExecutorService executorService = Executors.newCachedThreadPool(); // Thread pool for concurrent tasks

        // Check each IP in the subnet
        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            executorService.submit(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    // Check if the port is open
                    if (isPortOpen(address)) {
                        try (Socket socket = new Socket(address, myPort)) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out.println("NAME_REQUEST"); // Request name from the peer
                            String peerName = in.readLine();
                            // If the peer's name is valid, store it
                            if (peerName != null && !peerName.equals(name) && !host.equals(InetAddress.getLocalHost().getHostAddress())) {
                                reachableIPs.put(host, peerName);
                            }
                        } catch (IOException e) {
                            System.out.println(ansi().fg(Ansi.Color.RED).a("Error querying peer at " + host + ": " + e.getMessage()).reset());
                        }
                    }
                } catch (NoSuchElementException | IOException e) {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("Error checking reachability of " + host + ": " + e.getMessage()).reset());
                }
            });
        }
        executorService.shutdown(); // Shutdown the executor service
    }

    // Check if the specified port is open on the given IP address
    public static boolean isPortOpen(InetAddress ipAddress) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, 5684), 1000); // Attempt to connect
            return true; // Port is open
        } catch (IOException e) {
            return false; // Port is closed
        }
    }

    // Print the list of reachable users
    public static void printListOfUsers() {
        int i = 1;

        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("\n------------------- ONLINE USERS --------------------").reset());
        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---------- CHOOSE AN OPTION FROM THE LIST -----------").reset());

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println(ansi().fg(Ansi.Color.CYAN).a(i + ". " + entry.getValue() + " ( " + entry.getKey() + " )").reset());
            i++;
        }
    }

    // Display guidance message for user options
    private static void GuideMsg() {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*****************************************************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("****************** CHOOSE AN OPTION *****************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("************** 1. List of online user ***************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("************** 2. Broadcast a message ***************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*****************************************************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*************** /exit - Close the app ***************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*****************************************************").reset());
    }

    // Main loop for sending messages to peers
    private static void MsgToPeer(int myPort, String name) {
        GuideMsg(); // Display options

        while (true) {
            Scanner sc = new Scanner(System.in);
            String message;
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (true) {
                    String choice = sc.nextLine().trim(); // Get user choice
                    if (choice.equalsIgnoreCase("/exit")) {
                        System.exit(0); // Exit application
                    }
                    switch (choice) {

                        case "1": {
                            getAllUser(name, myPort); // Refresh user list
                            if (reachableIPs.isEmpty()) {
                                System.out.println(ansi().fg(Ansi.Color.RED).a("-------- No clients are currently connected ---------\n").reset());
                                GuideMsg();
                            } else {
                                printListOfUsers(); // Print available users
                                String position = sc.nextLine().trim(); // Get user position

                                if (position.isEmpty()) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Input cannot be empty *****\n").reset());
                                    GuideMsg();
                                    break;
                                }

                                try {
                                    int userPosition = Integer.parseInt(position); // Convert input to integer
                                    if (userPosition >= 1 && userPosition <= reachableIPs.size()) {
                                        Map.Entry<String, String>[] entries = reachableIPs.entrySet().toArray(new Map.Entry[0]);
                                        String ipAddressString = entries[userPosition - 1].getKey(); // Get IP address of selected user
                                        connectToPeer(InetAddress.getByName(ipAddressString), myPort, name); // Connect to selected peer
                                        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---- CONNECTED TO " + entries[userPosition - 1].getValue().toUpperCase() + " ----").reset());
                                        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("-------- Enter message or '/exit' to go back --------").reset());
                                        while (true) {
                                            String prompt = name + " : ";
                                            System.out.print(ansi().fg(Ansi.Color.BLUE).a(prompt).reset());
                                            message = consoleInput.readLine(); // Read user message

                                            try {
                                                if (message.equalsIgnoreCase("/exit")) {
                                                    System.out.println(ansi().fg(Ansi.Color.RED).a("--------------- You left the chat ---------------").reset());
                                                    out.println(ansi().fg(Ansi.Color.RED).a("**** " + name + " left the chat ****").reset());
                                                    innerThread.stopRunning(); // Stop the inner thread if exiting
                                                    cleanUp(); // Clean up resources
                                                    break; // Exit message loop
                                                } else if (isConnected) {
                                                    out.println(ansi().fg(Ansi.Color.BLUE).a(name).reset() + " : " + message); // Send message to peer
                                                } else {
                                                    System.out.println(ansi().fg(Ansi.Color.RED).a("Cannot send message, not connected.").reset());
                                                    break; // Exit if not connected
                                                }
                                            } catch (NullPointerException e) {
                                                System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid Message *****").reset());
                                            }
                                        }
                                    } else {
                                        System.out.println(ansi().fg(Ansi.Color.RED).a("***** User not present *****\n").reset());
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid input, please enter a number *****\n").reset());
                                } catch (NoSuchElementException e) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid input, please enter a number *****\n").reset());
                                    return; // Exit on invalid input
                                }
                                GuideMsg(); // Display options again
                            }
                        }
                        break;

                        case "2": {
                            broadcast(); // Start broadcasting message to all users
                            GuideMsg(); // Display options again
                        }
                        break;

                        case "":
                            System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
                            GuideMsg(); // Display options again
                            break;

                        default: {
                            System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
                        }
                    }
                }
            } catch (NoSuchElementException e1) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
                GuideMsg(); // Display options again
            } catch (IOException e) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("*****************************************************").reset());
                System.out.println(ansi().fg(Ansi.Color.RED).a("************ Connection is disconnected *************").reset());
                System.out.println(ansi().fg(Ansi.Color.RED).a("*****************************************************").reset());
            }
        }
    }

    // Method to broadcast a message to all reachable users
    public static void broadcast() {
        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---------------- BROADCAST STARTED ------------------").reset());
        getAllUser(name, myPort); // Refresh user list

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();

        if (reachableIPs.isEmpty()) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxxxxxxxxxxxx No users connected xxxxxxxxxxxxxxxx\n").reset());
        } else {
            System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("-------- Enter message or '/exit' to go back --------").reset());

            while (true) {
                Scanner sc = new Scanner(System.in);
                try {
                    System.out.print(ansi().fg(Ansi.Color.CYAN).a(name + "(You) : ").reset());
                    String message = sc.nextLine(); // Read broadcast message
                    for (Map.Entry<String, String> entry : entries) {
                        String ip = entry.getKey(); // Get user IP
                        String userName = entry.getValue(); // Get user name
                        try {
                            if (message.equalsIgnoreCase("/exit")) {
                                System.out.println(ansi().fg(Ansi.Color.RED).a("-------------- You left the broadcast --------------\n").reset());
                                return; // Exit broadcast
                            } else {
                                // Send message to each reachable user
                                try (Socket socket = new Socket(ip, myPort)) {
                                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                    out.println(ansi().fg(Ansi.Color.BLUE).a(name + " : ").reset() + message);
                                }
                            }
                        } catch (IOException e) {
                            System.out.println(ansi().fg(Ansi.Color.RED).a(userName + "(" + ip + "): " + "Disconnected").reset());
                        } catch (NoSuchElementException | NullPointerException e) {
                            System.out.println("***** Invalid Message *****");
                        }
                    }
                } catch (NoSuchElementException e3) {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid Message *****").reset());
                }
            }
        }
    }

    // Start a server to accept incoming connections from peers
    private static void startServer(int myPort) {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming connections
                new Thread(() -> handleClientMessages(clientSocket)).start(); // Handle client messages in a new thread
            }
        } catch (NoSuchElementException | IOException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxxxxxx This account is already in use xxxxxxxxxx").reset());
            System.exit(0); // Exit on error
        }
    }

    // Handle messages from connected clients
    private static void handleClientMessages(Socket clientSocket) {
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
             PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = clientIn.readLine()) != null) {
                // Respond to name requests and print other messages
                if ("NAME_REQUEST".equals(message)) {
                    clientOut.println(name);
                } else {
                    System.out.println(message); // Print received messages
                }
            }
        } catch (NoSuchElementException | IOException e) {
            // Handle unexpected disconnection
        }
    }

    // Inner class to handle incoming messages from the connected peer
    static class InnerThread extends Thread {
        private volatile boolean isRunning = true; // Flag to control the thread's execution

        public void run() {
            String message = " ";
            try {
                while (isRunning && (message = in.readLine()) != null) {
                    System.out.println(message); // Print incoming messages
                }
            } catch (IOException e) {
                // Handle exception if needed
            } finally {
                isConnected = false; // Reset connection status
                cleanUp(); // Ensure cleanup on thread exit
            }
        }

        public void stopRunning() {
            isRunning = false; // Method to stop the thread
        }
    }

    // Connect to a peer given its IP address and port
    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort); // Establish connection
            out = new PrintWriter(socket.getOutputStream(), true); // Initialize PrintWriter
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initialize BufferedReader

            isConnected = true; // Set connection status to true
            out.println(ansi().fg(Ansi.Color.CYAN).a("**** " + name + " has connected ****").reset());

            // Initialize and start the InnerThread to listen for incoming messages
            if (innerThread == null || !innerThread.isAlive()) {
                innerThread = new InnerThread();
                innerThread.start();
            }
        } catch (NoSuchElementException | IOException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxx Failed to connect to peer: " + e.getMessage() + "xxxxxx").reset());
        }
    }

    // Clean up resources before closing connections
    private static void cleanUp() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Close socket connection
            }
            if (out != null) {
                out.close(); // Close PrintWriter
            }
            if (in != null) {
                in.close(); // Close BufferedReader
            }
        } catch (IOException e) {
            // Handle cleanup error if needed
        } finally {
            socket = null;
            out = null;
            in = null;
            isConnected = false; // Reset connection status
        }
    }
}
