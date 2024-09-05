package com.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AllPeers {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the IP address of the server");
        String ipAddress = sc.next();
        System.out.println("Enter the port to connect");
        int port = sc.nextInt();

        try {
            socket = new Socket(ipAddress, port);
            System.out.println("Connected to server at " + ipAddress + " on port " + port);

            // Setup input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

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

            // Handle sending messages to the server
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while ((input = consoleInput.readLine()) != null) {
                out.println(input);
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server.");
            e.printStackTrace();
        }
    }
}