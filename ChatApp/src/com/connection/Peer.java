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

    private static ConcurrentHashMap<String, String> reachableIPs = new ConcurrentHashMap<>();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String name;
    private static int myPort;
    private static boolean isConnected = false; // Flag to track connection status
    private static InnerThread innerThread; // Class-level variable for InnerThread

    public static void main(String[] args) {
        AnsiConsole.systemInstall();

        myPort = 5684; // Port to listen on
        System.out.println(ansi().fg(Ansi.Color.CYAN).a("\nENTER YOUR NAME :").reset());
//        boolean flag = true;
        while (true) {
            Scanner sc = new Scanner(System.in);
            try {
                name = sc.nextLine().trim();
                // Check if the name is valid (only alphabets)
                if (isValidName(name)) {
                    getAllUser(name, myPort);
                    break;
                    // Valid name, exit loop
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

        new Thread(() -> startServer(myPort)).start();
        MsgToPeer(myPort, name);
    }

    private static boolean isValidName(String name) {
        // Regex to allow only alphabets
        Pattern pattern = Pattern.compile("^[a-zA-Z]+( [a-zA-Z]+)*$");
        return pattern.matcher(name).matches();
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
                            System.out.println(ansi().fg(Ansi.Color.RED).a("Error querying peer at " + host + ": " + e.getMessage()).reset());
                        }
                    }
                } catch (NoSuchElementException | IOException e) {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("Error checking reachability of " + host + ": " + e.getMessage()).reset());
                    System.out.println();
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

        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("\n------------------- ONLINE USERS --------------------").reset());
        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---------- CHOOSE AN OPTION FROM THE LIST -----------").reset());

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println(ansi().fg(Ansi.Color.CYAN).a(i + ". " + entry.getValue() + " ( " + entry.getKey() + " )").reset());
            i++;
        }
    }

    private static void GuideMsg() {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*****************************************************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("****************** CHOOSE AN OPTION *****************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("************** 1. List of online user ***************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("************** 2. Broadcast a message ***************").reset());
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("*****************************************************").reset());

    }

    private static void MsgToPeer(int myPort, String name) {
        GuideMsg();

        while (true) {
            Scanner sc = new Scanner(System.in);
            String message;
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (true) {
                    String choice = sc.nextLine().trim();
                    if (choice.equalsIgnoreCase("/exit")) {
                        System.exit(0);
                    }
                    switch (choice) {

                        case "1": {
                            getAllUser(name, myPort);
                            if (reachableIPs.isEmpty()) {
                                System.out.println(ansi().fg(Ansi.Color.RED).a("-------- No clients are currently connected ---------\n").reset());
                                GuideMsg();
                            } else {

//                                while(true) {
                                printListOfUsers();
                                String position = sc.nextLine().trim();

                                if (position.isEmpty()) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Input cannot be empty *****\n").reset());
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
                                        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---- CONNECTED TO " + entries[userPosition - 1].getValue().toUpperCase() + " ----").reset());
                                        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("-------- Enter message or '/exit' to go back --------").reset());
                                        while (true) {
                                            String prompt = name + " : ";
                                            System.out.print(ansi().fg(Ansi.Color.BLUE).a(prompt).reset());
                                            message = consoleInput.readLine();

                                            if (message.equalsIgnoreCase("/exit")) {
                                                System.out.println(ansi().fg(Ansi.Color.RED).a("--------------- You left the chat ---------------").reset());
                                                out.println(ansi().fg(Ansi.Color.RED).a("**** " + name + " left the chat ****").reset());
                                                innerThread.stopRunning(); // Stop the inner thread if exiting
                                                cleanUp(); // Ensure we clean up resources
                                                break; // Exit the message loop
                                            } else if (isConnected) {
                                                out.println(ansi().fg(Ansi.Color.BLUE).a(name).reset() + " : " + message);
                                            } else {
                                                System.out.println(ansi().fg(Ansi.Color.RED).a("Cannot send message, not connected.").reset());
                                                break; // Exit if not connected
                                            }

                                        }

                                    } else {
                                        System.out.println(ansi().fg(Ansi.Color.RED).a("***** User not present *****\n").reset());
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid input, please enter a number *****\n").reset()); // Handle invalid input
                                } catch (NoSuchElementException e) {
                                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid input, please enter a number *****\n").reset()); // Handle invalid input
                                    return;
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
                        case "":
                            System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
                            GuideMsg();
                            break;
                        default:
                            System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
                    }
                }
            } catch (NoSuchElementException e1) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("******************* INVALID OPTION ******************\n").reset());
            } catch (IOException e) {
                System.out.println(ansi().fg(Ansi.Color.RED).a("*****************************************************").reset());
                System.out.println(ansi().fg(Ansi.Color.RED).a("************ Connection is disconnected *************").reset());
                System.out.println(ansi().fg(Ansi.Color.RED).a("*****************************************************").reset());
            }
        }
    }

    public static void broadcast() {
        System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("---------------- BROADCAST STARTED ------------------").reset());
        getAllUser(name, myPort);

        Set<Map.Entry<String, String>> entries = reachableIPs.entrySet();

        if (reachableIPs.isEmpty()) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxxxxxxxxxxxx No users connected xxxxxxxxxxxxxxxx\n").reset());
        } else {
            System.out.println(ansi().fg(Ansi.Color.MAGENTA).a("-------- Enter message or '/exit' to go back --------").reset());

            while (true) {
                Scanner sc = new Scanner(System.in);
                try {
                    System.out.print(ansi().fg(Ansi.Color.CYAN).a(name + "(You) : ").reset());
                    String message = sc.nextLine();
                    for (Map.Entry<String, String> entry : entries) {
                        String ip = entry.getKey();
                        String userName = entry.getValue();
                        try {
                            if (message.equalsIgnoreCase("/exit")) {
                                System.out.println(ansi().fg(Ansi.Color.RED).a("-------------- You left the broadcast --------------\n").reset());
                                return;
                            } else {
                                try (Socket socket = new Socket(ip, myPort)) {
                                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                    out.println(ansi().fg(Ansi.Color.BLUE).a(name + " : ").reset() + message);
                                }
                            }
                        } catch (NoSuchElementException | IOException e) {
                            System.out.println(ansi().fg(Ansi.Color.RED).a(userName + "(" + ip + "): " +  "Disconnected").reset());
                        }
                    }
                } catch (NoSuchElementException e3) {
                    System.out.println(ansi().fg(Ansi.Color.RED).a("***** Invalid Message *****").reset());
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
        } catch (NoSuchElementException | IOException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxxxxxx This account is already in use xxxxxxxxxx").reset());
            System.exit(0);
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
        } catch (NoSuchElementException | IOException e) {
            // System.out.println(ansi().fg(Ansi.Color.RED).a("Client disconnected unexpectedly: " + e.getMessage()).reset());
        }
    }

    static class InnerThread extends Thread {

        private volatile boolean isRunning = true; // Flag to control the thread's execution

        public void run() {
            String message = " ";
            try {
                while (isRunning && (message = in.readLine()) != null) {
                    System.out.println(message);
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

    private static void connectToPeer(InetAddress peerIp, int peerPort, String name) {
        try {
            socket = new Socket(peerIp, peerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            isConnected = true; // Set connection status to true
            out.println(ansi().fg(Ansi.Color.CYAN).a("**** " + name + " has connected ****").reset());

            // Initialize and start the InnerThread
            if (innerThread == null || !innerThread.isAlive()) {
                innerThread = new InnerThread();
                innerThread.start();
            }

        } catch (NoSuchElementException | IOException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("xxxxxx Failed to connect to peer: " + e.getMessage() + "xxxxxx").reset());
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
//            System.out.println("Error during cleanup: " + e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
            isConnected = false; // Reset connection status
        }
    }
}
