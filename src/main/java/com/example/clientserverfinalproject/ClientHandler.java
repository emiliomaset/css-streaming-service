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

    //create file of Song objects whose files are currently in the song-database when receiving a song,
    // then access this file and loop through it when a song is searched.
    // when found, send this Song object to sendSong() method.

    // sent file not moving to mp3 directory -- 8:49 am 3/23/24

    private ArrayList<Song> allSongs = new ArrayList<Song>();

    public ClientHandler(Socket socket) {

        this.clientSocket = socket;
        try {
            dataOutputStreamToSendFiles = new DataOutputStream(socket.getOutputStream());
            dataInputStreamToReceiveFiles = new DataInputStream(socket.getInputStream());
            objectOutputStreamToClient = new ObjectOutputStream(socket.getOutputStream());
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
                while (clientSocket.isConnected()) {
                    Scanner input;
                    try {
                        input = new Scanner(clientSocket.getInputStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String searchedSong = input.nextLine(); //culprit to weird print outs. must be getting activated in receiveASong() when getting songtitle
                    System.out.println("hiiiiiii");
                    searchedSong = searchedSong.toLowerCase().replaceAll(" ", "");
                    System.out.println(searchedSong);
                    try {
                        ObjectInputStream databaseObjectInputStream = new ObjectInputStream(new FileInputStream("songlibrary.dat"));
                        while (true) {
                            Song o = (Song) databaseObjectInputStream.readObject();
                            System.out.println(o);
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
        int bytes = 0;
        File file = song.getMp3File();
        FileInputStream fileInputStream = new FileInputStream(file);

        dataOutputStreamToSendFiles = new DataOutputStream(clientSocket.getOutputStream());
        dataOutputStreamToSendFiles.writeLong(file.length()); // null????
        dataOutputStreamToSendFiles.flush();
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStreamToSendFiles.write(buffer, 0, bytes);
            dataOutputStreamToSendFiles.flush();
        }

    }

   // ===========================================================================================================================

    public void sendASongToSongLibraryFile() throws IOException {

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

    // ===========================================================================================================================

    public Song receiveASong() throws Exception {
        Song song = new Song();
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

        Scanner stringInput = new Scanner(clientSocket.getInputStream());
        song.setSongTitle(stringInput.nextLine()); //culprit to weird print outs. getting activated

        Files.move(Path.of(file.getPath()),
                Path.of("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/"
                        + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));
        // rename song file to title of song and move to mp3-database
        song.setMp3File(new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/mp3-database/" + song.getSongTitle().replaceAll(" ", "").toLowerCase() + ".mp3"));
        song.setArtist(stringInput.nextLine());


        return song;
    }

    // ===========================================================================================================================


}
