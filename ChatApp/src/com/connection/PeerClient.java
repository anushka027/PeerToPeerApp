package com.connection;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class PeerClient {
    private int port;

    public PeerClient(int port) {
        this.port = port;
    }

    public void connectToPeer(String host, int port) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to peer. Type messages to send:");
            String userInput;
            while (true) {
                userInput = scanner.nextLine();
                out.println(userInput); 
            }

        } catch (IOException e) {
            System.out.println("Could not connect to peer: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            int serverPort = 12345; 
            PeerServer peerServer = new PeerServer(serverPort);
            peerServer.start(); // Start the server

            int clientPort = 12346; 
            PeerClient client = new PeerClient(clientPort);

            client.connectToPeer("localhost", serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
