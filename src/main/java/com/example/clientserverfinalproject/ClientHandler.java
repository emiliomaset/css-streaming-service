package com.example.clientserverfinalproject;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private final Socket clientSocket1;
    private final Socket clientSocket2;
    private final Socket clientSocket3;
    private DataOutputStream dataOutputStreamToSendFiles;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToClient;
    private ObjectInputStream objectInputStreamFromClient;
    private ObjectOutputStream objectOutputStreamToWriteToSongLibrary;
    private Scanner stringInputFromClient;

    private ArrayList<Song> allSongs = new ArrayList<>();

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
            ObjectInputStream objectInputStreamFromSongLibrary = new ObjectInputStream(new FileInputStream("songlibrary.ser"));

            try {
                while (true) { // since you cannot append to files with serialized objects between runs, one must recreate the songlibrary.ser file each run
                    allSongs.add((Song) objectInputStreamFromSongLibrary.readObject());
                }
            } catch(Exception e) { // catch songlibrary.ser EOF
            }

            Files.delete(Paths.get("songlibrary.ser"));

            objectOutputStreamToWriteToSongLibrary = new ObjectOutputStream(new FileOutputStream("songlibrary.ser", true));

            for (Song song : allSongs) {
                objectOutputStreamToWriteToSongLibrary.writeObject(song); // populating songlibrary.ser file
            }

            objectOutputStreamToClient.writeObject(allSongs); // send allSongs arraylist to client so they have a copy
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    // ===========================================================================================================================

    @Override
    public void run() {
        try {
            sendASongToSongLibraryFile();
            analyzeSearch();
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
                        databaseObjectInputStream = new ObjectInputStream(new FileInputStream("songlibrary.ser"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String searchedSong = stringInputFromClient.nextLine();
                    try {
                        while (true) {
                            Song o = (Song) databaseObjectInputStream.readObject();
                            if (o.getSongTitle().equals(searchedSong))
                                try {
                                    sendSongToClient(o);
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    break;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // ===========================================================================================================================

    public void sendSongToClient(Song song) throws Exception {
        int bytes = 0;

        String filePath = song.getMp3File().toString().substring(song.getMp3File().toString().indexOf("mp3"));
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);

        dataOutputStreamToSendFiles.writeLong(file.length());
        dataOutputStreamToSendFiles.flush();
        byte[] buffer = new byte[5000];
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
                        allSongs.add(song);
                        objectOutputStreamToWriteToSongLibrary.writeObject(song);
                        objectOutputStreamToWriteToSongLibrary.flush();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    // ===========================================================================================================================

    public Song receiveASong() throws Exception {
        int numOfBytesReadIntoBuffer;
        File file = new File("dummy.mp3"); // create dummy file to store song in // just make this file in mp3 database
        FileOutputStream fileOutputStreamToMakeMp3Files = new FileOutputStream(file);

        long size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
        byte[] buffer = new byte[5000];
        while (size > 0 &&
                ((numOfBytesReadIntoBuffer = dataInputStreamToReceiveFiles.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) ) {
            fileOutputStreamToMakeMp3Files.write(buffer, 0, numOfBytesReadIntoBuffer);
            size = size - numOfBytesReadIntoBuffer;
        }

        fileOutputStreamToMakeMp3Files.flush();

        Song song = (Song) objectInputStreamFromClient.readObject();

        Files.move(Path.of(file.getPath()),
                Path.of("mp3-database/"
                        + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));
        // move mp3 file to mp3-database directory

        song.setMp3File(new File("mp3-database/"
                + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));
        // rename song object's mp3 file field in preparation for songblibrary.ser storage

        return song; // return song object to place it into songlibrary.ser
    }

    // ===========================================================================================================================
}