package com.connection;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;;

public class AllPeers{
	
	 public static CommonServer cs;

//	 public AllPeers(int port) {
//	     this.port = port;
//	     cs = new CommonServer(port);
//	 }
	    
	 public static void ConnectionHandling() {
		 Scanner sc = new Scanner(System.in);
	        System.out.println("Enter the IP address of your friend");
	        String ipAddress = sc.next(); 
	        System.out.println("Enter the port you need to connect");
	        int port = sc.nextInt();
	        try {
	            Socket socket = new Socket(ipAddress, port);
	            System.out.println("Connected to friend at " + ipAddress + " on port " + port);
	             
	            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	            out.println("Hello from your friend!");

	         

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
		 
		 System.out.println("You want toconnect to a friend");
		 String reply =sc.next();
		 
		 if(reply.endsWith("Yes")) {
			 ConnectionHandling();
		 }
	}
}
