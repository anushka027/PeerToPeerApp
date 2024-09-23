package com.garbage;

import java.util.Scanner;

public class Test {
	private static String typeMessage() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter Message");
		String message = "\033[F"+"\r"+ " ".repeat(("Enter Message").length())+"\r"+sc.nextLine();
		return (message.length()>0)?message:("Enter Message");
	}
	public static void main(String[] args) {
		System.out.print(typeMessage());
	}
}
