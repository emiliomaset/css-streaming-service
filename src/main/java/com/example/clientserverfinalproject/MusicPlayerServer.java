package com.example.clientserverfinalproject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MusicPlayerServer {
    private static ServerSocket serverSocket;
    private static final int PORT = 4321;
    public static void main(String[] args) throws IOException, ClassNotFoundException {


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
    private ArrayList<Song> allSongs = new ArrayList<Song>();

    public ClientHandler(Socket socket) throws IOException, ClassNotFoundException {
        this.client = socket;
        try {
            objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            objectInputStream = new ObjectInputStream(client.getInputStream());
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

        do {
            allSongs.add((Song) objectInputStream.readObject());
            System.out.println(allSongs.toArray());
        } while (true);


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
