package com.garbage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Peer2 {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(7890)) {
            System.out.println("Waiting for clients");

            
            
            try (Socket serverSoc = server.accept();
                 BufferedReader clientInput = new BufferedReader(new InputStreamReader(serverSoc.getInputStream()));
                 BufferedReader peer2Msg = new BufferedReader(new InputStreamReader(System.in));
                 PrintWriter serveOutput = new PrintWriter(serverSoc.getOutputStream(), true)) {

                System.out.println("Connection established");

                String message;
                while (true) {
                    // Receive Peer1 Message
                	message = clientInput.readLine();
                	if(message == null || message.equalsIgnoreCase("exit")) {
                		System.out.println("Peer1 left the conversation");
                		break;
                	}
                    System.out.println("Peer1 : " + message);
                	
                    
                    // Peer2 inputs its message
                    System.out.print("Peer2 : ");
                    String reply = peer2Msg.readLine();

                    if (reply == null || reply.equalsIgnoreCase("exit")) {
                        serveOutput.println("exit");
                        break;
                    }
                    serveOutput.println(reply);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}