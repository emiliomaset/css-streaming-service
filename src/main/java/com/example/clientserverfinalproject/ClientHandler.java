package com.example.clientserverfinalproject;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private Scanner input;

    private File dataBaseFile = new File("songDatabase.txt");

    public ClientHandler(Socket socket) throws IOException, ClassNotFoundException {

        this.clientSocket = socket;
        try {
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
            input = new Scanner(clientSocket.getInputStream());

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    @Override
    public void run() {
        analyzeSearch();
        try {
            sendASongToDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void analyzeSearch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket.isConnected()) {
                    String searchedSong = input.nextLine();
                    searchedSong = searchedSong.toLowerCase();
                    searchedSong = searchedSong.replace(" ", "");
                    System.out.println(searchedSong);
                    try {
                        ObjectInputStream databaseInputStream = new ObjectInputStream(new FileInputStream("songDatabase.txt"));
                        while (true) {
                            Song o = (Song) databaseInputStream.readObject();
                            System.out.println(o);
                            if (o.getSongTitle().equals(searchedSong))
                                try {
                                    objectOutputStream.writeObject(o);
                                    objectOutputStream.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    break;
                                }
                        }
                    } catch (IOException e) {
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        }).start();
    }

    public void sendASongToDatabase() throws IOException {
        ObjectOutputStream objectOutputStreamToDatabase = new ObjectOutputStream(new FileOutputStream
                (new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songDatabase.txt")));
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket.isConnected()) {
                    try {
                        objectOutputStreamToDatabase.writeObject((Song) objectInputStream.readObject());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }).start();
    }
}
