package com.example.clientserverfinalproject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class AlterSongLibrarySer { // used to add an indiviudal song object to database or to correct a typo in song title
    public static void main(String[] args){
    }

    public static void sendASongToDatabase(Song song) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream
                (new File("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject", "songlibrary.ser")));
        objectOutputStream.writeObject(song);


    }

    public static void correctATypoInSongTitle () throws IOException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("songlibrary.ser"));

        ArrayList<Song> songsFromSongLibrary = new ArrayList<>();
        try {
            while (true)
                songsFromSongLibrary.add((Song) objectInputStream.readObject());
        }
        catch(EOFException | ClassNotFoundException eoEx) {
        }

        objectInputStream.close();

        Files.delete(Path.of("songlibrary.ser"));

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("songlibrary.ser"));

        for (Song song : songsFromSongLibrary) {
            if (song.getSongTitle().contains("Nicki"))
                song.setSongTitle("New Body (feat. Nicki Minaj & Ty Dolla $ign)");
            objectOutputStream.writeObject(song);
        }
    }
}
