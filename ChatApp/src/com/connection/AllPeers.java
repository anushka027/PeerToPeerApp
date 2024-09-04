package com.connection;

import java.util.Scanner;;

public class AllPeers{
	
//	 public static CommonServer cs;
	 private int port;

//	 public AllPeers(int port) {
//	     this.port = port;
//	     cs = new CommonServer(port);
//	 }
	    
	 public static void ConnectionHandling(String name) {
		 
	 }
	    
	 public static void main(String[] args) {
		 
		 int PortNumber = 53;
		 Scanner sc = new Scanner(System.in);
		
		 System.out.println("Enter your name");
		 String name = sc.next();
		 
//		 AllPeers peers = new AllPeers(PortNumber);
		 CommonServer cgd = new CommonServer(PortNumber);
		 cgd.start();
//		 ConnectionHandling(name);
	}
}
