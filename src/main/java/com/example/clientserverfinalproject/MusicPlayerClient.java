package com.example.clientserverfinalproject;

import java.io.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    private static MediaPlayer mediaPlayer;

    public static void main(String[] args) throws InterruptedException {

        Song howToDisappearLDR = new Song("How to disappear", "Lana Del Rey", new File("Howtodisappear.mp3"));

        Media hit = new Media(howToDisappearLDR.getMp3File().toURI().toString());
        mediaPlayer = new MediaPlayer(hit);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Button playButton = new Button("Play");
        playButton.setOnAction(event -> {
            playSong();
        });
        Scene scene = new Scene(playButton);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void playSong() {
        mediaPlayer.play();
    }
}
