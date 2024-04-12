package com.example.clientserverfinalproject;


import java.io.*;
import java.util.ArrayList;

public class AlterSongLibrarySer { // used to correct a typo in song title
    public static void main(String[] args) throws IOException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("test.ser"));
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("songlibrary.ser"));

        ArrayList<Song> songsFromSongLibrary = new ArrayList<>();
        try {
            while (true)
                songsFromSongLibrary.add((Song) objectInputStream.readObject());
        }
        catch(EOFException | ClassNotFoundException eoEx){
            }

        for (Song song : songsFromSongLibrary) {
            if (song.getSongTitle().contains("Nicki"))
                song.setSongTitle("New Body (feat. Nicki Minaj & Ty Dolla $ign");
            objectOutputStream.writeObject(song);
        }

    }
}
