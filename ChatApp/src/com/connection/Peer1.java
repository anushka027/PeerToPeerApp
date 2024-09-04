package com.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Peer1 {

    public static void main(String[] args) {
    	
    	 try (
//    			 InetAddress ipv4 = InetAddress.getLocalHost();
    			 Socket peer1Soc = new Socket("192.168.1.21", 7890);                 
    			 BufferedReader peer1Msg = new BufferedReader(new InputStreamReader(System.in));
                 PrintWriter out = new PrintWriter(peer1Soc.getOutputStream(), true);
                 BufferedReader data = new BufferedReader(new InputStreamReader(peer1Soc.getInputStream()))) {

                System.out.println("Peer1 joined");

                while (true) {
                    // Peer1 inputs its message
                    System.out.print("Peer1 : ");
                    String message = peer1Msg.readLine();
                    
                    if (message == null || message.equalsIgnoreCase("exit")) {
                        out.println("exit");
                        break;
                    }
                    
                    // Peer1 sends the message to Peer2
                    out.println(message);
                    
                    // Receive message from Peer2 and print reply on console
                    String reply = data.readLine();
                    if (reply == null || reply.equalsIgnoreCase("exit")) {
                        System.out.println("Peer2 left the conversation");
                        break;
                    }
                    System.out.println("Peer2 : " + reply);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//    		ListenSockets();
//    		try {
//				System.out.println(InetAddress.getLocalHost());
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
       
    }
    
//    public static void ListenSockets() {
//    	
//    	
//    }
}