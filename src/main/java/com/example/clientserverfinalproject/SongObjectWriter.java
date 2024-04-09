package com.example.clientserverfinalproject;

import java.io.*;

public class SongObjectWriter {
    public static void main (String[] args) throws IOException {
        sendASongToDatabase(new Song("Eternal Sunshine", "Ariana Grande", new File("/Users/emiliomaset/Downloads/Ariana Grande - eternal sunshine (lyric visualizer).mp3")));
    }
    public static void sendASongToDatabase(Song song) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream
                (new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songlibrary.ser")));
        objectOutputStream.writeObject(song);


    }
}
