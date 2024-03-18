package com.example.clientserverfinalproject;

import java.io.*;

public class SongObjectWriter {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File songDatabase = new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songDatabase.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(songDatabase));
        //ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(songDatabase));

        objectOutputStream.writeObject(new Song("How to disappear", "Lana Del Rey", new File("Howtodisappear.mp3")));



    }
}
