package com.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;;

public class peerabc{
	
	public static CommonServer cs;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
	    
	 public static void ConnectionHandling() {
		 Scanner sc = new Scanner(System.in);
	        System.out.println("Enter the IP address of your friend");
	        String ipAddress = sc.next(); 
	        System.out.println("Enter the port you need to connect");
	        int port = sc.nextInt();
	        try {
	            socket = new Socket(ipAddress, port);
	            System.out.println("Connected to friend at " + ipAddress + " on port " + port);
	             
	            // Setup input and output streams
	            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	            out = new PrintWriter(socket.getOutputStream(), true);

	            // Start a thread to handle incoming messages
	            new Thread(() -> {
	                try {
	                    String message;
	                    while ((message = in.readLine()) != null) {
	                        System.out.println("Friend: " + message);
	                    }
	                } catch (IOException e) {
	                    System.err.println("Error reading from friend.");
	                    e.printStackTrace();
	                }
	            }).start();

	            // Handle sending messages from the client
	            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
	            String input;
	            while ((input = consoleInput.readLine()) != null) {
	                out.println(input);
	            }
	        } catch (IOException e) {
	            System.err.println("Error connecting to friend.");
	            e.printStackTrace();
	        }
	 }
	    
	 public static void main(String[] args) {
		 
//		 int PortNumber = 53;
		 Scanner sc = new Scanner(System.in);
		
		 System.out.println("Enter your port number");
		 int PortNumber = sc.nextInt();
		 
		 System.out.println("Enter your name");
		 String name = sc.next();
		 
//		 AllPeers peers = new AllPeers(PortNumber);
		 cs = new CommonServer(PortNumber);
		 cs.start();
		 
			 ConnectionHandling();
	
	}
}
