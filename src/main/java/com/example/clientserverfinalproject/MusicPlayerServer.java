package com.example.clientserverfinalproject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MusicPlayerServer {
    private static ServerSocket serverSocket1;
    private static ServerSocket serverSocket2;
    private static ServerSocket serverSocket3;
    private static final int PORT1 = 1222;
    private static final int PORT2 = 2111;
    private static final int PORT3 = 3333;

    public static void main(String[] args) throws IOException, InterruptedException {

        try {
            serverSocket1 = new ServerSocket(PORT1);
            serverSocket2 = new ServerSocket(PORT2);
            serverSocket3 = new ServerSocket(PORT3);
        } catch (IOException ioEx) {
            System.out.println("\nUnable to set up port!");
            System.exit(1);
        }

        while (!serverSocket1.isClosed() & !serverSocket2.isClosed()) {
            Socket client1 = serverSocket1.accept();
            Socket client2 = serverSocket2.accept();
            Socket client3 = serverSocket3.accept();
            System.out.println("\nNew client accepted.\n");
            ClientHandler handler = new ClientHandler(client1, client2, client3);
            handler.start();
        }

    }
}
