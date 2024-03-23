package com.example.clientserverfinalproject;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private DataOutputStream dataOutputStreamToSendFiles;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToClient;
    private ObjectOutputStream objectOutputStreamToWriteToSongLibrary;
    private Scanner stringInput;

    //create file of Song objects whose files are currently in the song-database when receiving a song,
    // then access this file and loop through it when a song is searched.
    // when found, send this Song object to sendSong() method.

    private ArrayList<Song> allSongs = new ArrayList<Song>();

    public ClientHandler(Socket socket){

        this.clientSocket = socket;
        try {
            dataOutputStreamToSendFiles = new DataOutputStream(socket.getOutputStream());
            dataInputStreamToReceiveFiles = new DataInputStream(socket.getInputStream());
            objectOutputStreamToClient = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStreamToWriteToSongLibrary = new ObjectOutputStream(new FileOutputStream("songlibrary.dat"));
            stringInput = new Scanner(clientSocket.getInputStream());
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    @Override
    public void run() {
            try {
                sendASongToDatabase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public Song receiveASong() throws Exception {
            Song song = new Song();
            int bytes = 0;
            File file = new File("dummy.mp3"); // create dummy file to store song in
            FileOutputStream fileOutputStreamToMakeMp3iles = new FileOutputStream(file);

            long size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = dataInputStreamToReceiveFiles.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                fileOutputStreamToMakeMp3iles.write(buffer, 0, bytes);
                size -= bytes;
            }

        fileOutputStreamToMakeMp3iles.flush();

            Scanner stringInput = new Scanner(clientSocket.getInputStream());
            song.setSongTitle(stringInput.nextLine());

            Files.move(Path.of(file.getPath()),
                    Path.of("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/" + song.getSongTitle().replaceAll(" ", "") + ".mp3"));
            // rename song file to title of song and move to mp3-database
            song.setMp3File(new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/" + song.getSongTitle().replaceAll(" ", "") + ".mp3"));
            song.setArtist(stringInput.nextLine());

            return song;

    }


    //    public void analyzeSearch() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (clientSocket.isConnected()) {
//                    String searchedSong = input.nextLine();
//                    searchedSong = searchedSong.toLowerCase();
//                    searchedSong = searchedSong.replace(" ", "");
//                    System.out.println(searchedSong);
//                    try {
//                        ObjectInputStream databaseInputStream = new ObjectInputStream(new FileInputStream("songDatabase.txt"));
//                        while (true) {
//                            Song o = (Song) databaseInputStream.readObject();
//                            System.out.println(o);
//                            if (o.getSongTitle().equals(searchedSong))
//                                try {
//                                    objectOutputStream.writeObject(o);
//                                    objectOutputStream.flush();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                    break;
//                                }
//                        }
//                    } catch (IOException e) {
//                    } catch (ClassNotFoundException e) {
//                    }
//                }
//            }
//        }).start();
////    }
//
    public void sendASongToDatabase() throws IOException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket.isConnected() && !clientSocket.isClosed()) {
                    try {
                        Song song = receiveASong();
                        objectOutputStreamToWriteToSongLibrary.writeObject(song);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
}
