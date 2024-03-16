package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    private static InetAddress host;
    private static final int PORT = 1234;
    private static Scanner userInput;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;


    private static MediaPlayer mediaPlayer;

    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {

        try {
            host = InetAddress.getLocalHost();
        }
        catch (UnknownHostException unknownHostException) {
            System.out.println("\nHost not found!");
            System.exit(1);
        }

        try {
            Socket socket = null;
            socket = new Socket(host, PORT);
            userInput = new Scanner(System.in);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

        Song received = (Song) objectInputStream.readObject();


       // Song howToDisappearLDR = new Song("How to disappear", "Lana Del Rey", new File("Howtodisappear.mp3"));

        Media hit = new Media(received.getMp3File().toURI().toString());
        mediaPlayer = new MediaPlayer(hit);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> playSong());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseSong());

        HBox hBox = new HBox(playButton, pauseButton);


        Scene scene = new Scene(hBox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(500);
        primaryStage.setWidth(500);
        primaryStage.show();

    }

    public void playSong() {
        mediaPlayer.play();
    }

    public void pauseSong() {
        mediaPlayer.pause();
    }
}
