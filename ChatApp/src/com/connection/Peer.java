package com.connection;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Peer {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public void startServer(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server started on port: " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Message from peer: " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectToPeer(String address, int port) {
        try {
            socket = new Socket(address, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(() -> listenForMessages()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received message: " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        Peer peer = new Peer();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter your port:");
        int port = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        peer.startServer(port);

        System.out.println("Enter peer address to connect:");
        String address = scanner.nextLine();
        System.out.println("Enter peer port to connect:");
        int peerPort = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        peer.connectToPeer(address, peerPort);

        while (true) {
            String message = scanner.nextLine();
            peer.sendMessage(message);
        }
    }
}
