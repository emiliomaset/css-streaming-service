package com.example.clientserverfinalproject;

import java.io.*;

public class SongObjectWriter {
    public void sendASongToDatabase(Song song) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream
                (new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songDatabase.txt")));
        objectOutputStream.writeObject(song);
    }
}
