package com.connection;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AllPeers {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void connectionHandling() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the IP address of the server");
        String serverIpAddress = sc.next();
        System.out.println("Enter the port of the server");
        int port = sc.nextInt();

        try {
            socket = new Socket(serverIpAddress, port);
            if (socket.isConnected()) {
                System.out.println("Connected to: " + serverIpAddress);
            }

            // Setup input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            // Start a thread to handle incoming messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println("Server: " + message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                String messageToSend = consoleInput.readLine();
                if (messageToSend.equalsIgnoreCase("exit")) {
                    break;
                }
                out.println(messageToSend);
            }

            socket.close();
            System.out.println("Connection closed");

        } catch (IOException e) {
            System.err.println("Error connecting to server.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your name");
        String name = sc.next();

        System.out.println("Enter your port");
        int serverPort = sc.nextInt();

//        System.out.println("Are you starting the server? (yes/no)");
//        String answer = sc.next();
//        if (answer.equalsIgnoreCase("yes")) {
            CommonServer cs = new CommonServer(serverPort);
            cs.start();
//        }

//        System.out.println("Connecting as client...");
        connectionHandling();
    }
}
