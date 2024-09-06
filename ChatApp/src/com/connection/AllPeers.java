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

        try {
            socket = new Socket(serverIpAddress, 5678);
            if (socket.isConnected()) {
                System.out.println("Connected to: " + serverIpAddress);
            }

            // Setup input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            while(true){
            	String MsgReceived = consoleInput.readLine();
            	if(MsgReceived == null){
            	break;
            	}
            	 
            	out.println(MsgReceived);
            	String MsgSend = in.readLine();
            	System.out.println("Server: "+MsgSend);
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

        int serverPort = 5678;

        CommonServer cs = new CommonServer(serverPort);
        cs.start();

        connectionHandling();
    }
}
