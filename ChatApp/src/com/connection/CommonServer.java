package com.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CommonServer {
	
	public ServerSocket server;
	public Socket ServerSoc;
	ArrayList<String> AllClient;
	
	public CommonServer(int port) {
		try {
			server = new ServerSocket(port);
			System.out.println("Connection established for port : "+port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() {
		new Thread(()-> {
			
			try {
				ServerSoc = server.accept();
				System.out.println("Client entered the chat application"+ServerSoc.getInetAddress().getHostAddress());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}
	
	public void ClientRoom(String name) throws UnknownHostException {
		
		boolean isNew = AllClient.contains(name);
		
		if(isNew) {
		AllClient.add(name);
		
		System.out.println("Online People");
		
		Iterator itr = AllClient.iterator();
		while(itr.hasNext()) {
			System.out.println(itr.next());
		}
		 
			
		}
		
		
	}
}
