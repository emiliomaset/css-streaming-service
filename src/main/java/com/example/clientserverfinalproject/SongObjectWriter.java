package com.example.clientserverfinalproject;

import java.io.*;

public class SongObjectWriter {
    public static void main (String[] args) throws IOException {
        sendASongToDatabase(new Song("How to disappear", "Lana Del Rey", new File("/Users/emiliomaset/Downloads/How to disappear.mp3")));
    }
    public static void sendASongToDatabase(Song song) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream
                (new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songlibrary.dat")));
        objectOutputStream.writeObject(song);


    }
}
