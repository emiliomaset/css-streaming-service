package com.example.clientserverfinalproject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MusicPlayerServer {
    private static ServerSocket serverSocket;
    private static final int PORT = 1234;
    public static void main(String[] args) throws IOException, ClassNotFoundException {


        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException ioEx) {
            System.out.println("\nUnable to set up port!");
            System.exit(1);
        }

        while (!serverSocket.isClosed()) {
            Socket client = serverSocket.accept();
            System.out.println("\nNew client accepted.\n");
            ClientHandler handler = new ClientHandler(client);
            handler.start();
        }

    }
}
