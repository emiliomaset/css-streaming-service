package com.example.clientserverfinalproject;

import java.io.*;

public class Song implements Serializable {
    private String songTitle;
    private String artist;
    private File mp3File;

    public Song (String songTitle, String artist, File mp3File) {
        this.songTitle = songTitle;
        this.artist = artist;
        this.mp3File = mp3File;
    }

    public String getSongTitle() {
        return this.songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public String getArtist() {
        return this.artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public File getMp3File() {
        return this.mp3File;
    }

    public void setMp3File(File mp3File) {
        this.mp3File = mp3File;
    }


}

