package com.example.clientserverfinalproject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MusicPlayerServer {
    private static ServerSocket serverSocket;
    private static final int PORT = 4321;
    public static void main(String[] args) throws IOException {

        try {
            serverSocket = new ServerSocket(PORT);
        }
        catch (IOException ioEx) {
            System.out.println("\nUnable to set up port!");
            System.exit(1);
        }

        do {
            Socket client = serverSocket.accept();
            System.out.println("\nNew client accepted.\n");
            ClientHandler handler = new ClientHandler(client);
            handler.start();
        } while (true);



    }
}

class ClientHandler extends Thread {
    private Socket client;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public ClientHandler(Socket socket) {
        this.client = socket;
        try {
            objectOutputStream = new ObjectOutputStream(client.getOutputStream());
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            objectOutputStream.writeObject(new Song("How to disappear", "Lana Del Rey", new File("Howtodisappear.mp3")));
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

    }
}
