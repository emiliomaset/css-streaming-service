package com.example.clientserverfinalproject;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private Socket clientSocket1;
    private Socket clientSocket2;
    private Socket clientSocket3;
    private DataOutputStream dataOutputStreamToSendFiles;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToClient;
    private ObjectInputStream objectInputStreamFromClient;
    private ObjectOutputStream objectOutputStreamToWriteToSongLibrary;
    private Scanner stringInputFromClient;

    //create file of Song objects whose files are currently in the song-database when receiving a song,
    // then access this file and loop through it when a song is searched.
    // when found, send this Song object to sendSong() method.


    private ArrayList<Song> allSongs = new ArrayList<Song>();

    public ClientHandler(Socket socket1, Socket socket2, Socket socket3) {
        this.clientSocket1 = socket1;
        this.clientSocket2 = socket2;
        this.clientSocket3 = socket3;
        try {
            dataOutputStreamToSendFiles = new DataOutputStream(clientSocket1.getOutputStream());
            dataInputStreamToReceiveFiles = new DataInputStream(clientSocket1.getInputStream());
            objectOutputStreamToClient = new ObjectOutputStream(clientSocket2.getOutputStream());
            objectInputStreamFromClient = new ObjectInputStream(clientSocket2.getInputStream());
            stringInputFromClient = new Scanner(clientSocket3.getInputStream());
            objectOutputStreamToWriteToSongLibrary = new ObjectOutputStream(new FileOutputStream("songlibrary.dat", true));
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    // ===========================================================================================================================

    @Override
    public void run() {
        try {
            sendASongToSongLibraryFile();
            analyzeSearch(); // doesnt like this.
            //mp3 files arent added to mp3-database, only kept in dummy.mp3, and objects arent written to songlibrary.dat
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===========================================================================================================================

    public void analyzeSearch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket2.isConnected()) {
                    ObjectInputStream databaseObjectInputStream = null;
                    try {
                        databaseObjectInputStream = new ObjectInputStream(new FileInputStream("songlibrary.dat"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    String searchedSong = stringInputFromClient.nextLine(); //culprit to weird print outs. must be getting activated in receiveASong() when getting songtitle
                    System.out.println(searchedSong);
                    try {
                        while (true) {
                            Song o = (Song) databaseObjectInputStream.readObject();
                            System.out.println(o.getSongTitle());
                            if (o.getSongTitle().equals(searchedSong))
                                try {
                                    sendSongToClient(o);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    break;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                        }
                    } catch (IOException e) {
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        }).start();
    }

    // ===========================================================================================================================

    public void sendSongToClient(Song song) throws Exception {
        System.out.println("sendSongToClient() activated");
        int bytes = 0;
        File file = song.getMp3File();
        FileInputStream fileInputStream = new FileInputStream(file);

        dataOutputStreamToSendFiles.writeLong(file.length());
        dataOutputStreamToSendFiles.flush();
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStreamToSendFiles.write(buffer, 0, bytes);
            dataOutputStreamToSendFiles.flush();
        }

    }

    // ===========================================================================================================================

    public void sendASongToSongLibraryFile(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket1.isConnected() && !clientSocket1.isClosed()) {
                    try {
                        Song song = receiveASong();
                        objectOutputStreamToWriteToSongLibrary.writeObject(song);
                        //objectOutputStreamToWriteToSongLibrary.reset();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    // ===========================================================================================================================

    public Song receiveASong() throws Exception {
        int bytes = 0;
        File file = new File("dummy.mp3"); // create dummy file to store song in // just make this file in mp3 database
        FileOutputStream fileOutputStreamToMakeMp3iles = new FileOutputStream(file);

        long size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = dataInputStreamToReceiveFiles.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStreamToMakeMp3iles.write(buffer, 0, bytes);
            size -= bytes;
        }

        fileOutputStreamToMakeMp3iles.flush();

        Song song = (Song) objectInputStreamFromClient.readObject();

        Files.move(Path.of(file.getPath()),
                Path.of("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/"
                        + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));
        // rename song file to title of song and move to mp3-database
        song.setMp3File(new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/" + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));

        return song;
    }

    // ===========================================================================================================================
}
