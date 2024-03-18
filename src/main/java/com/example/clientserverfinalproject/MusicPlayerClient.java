package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    private static InetAddress host;
    private static final int PORT = 4321;
    private static Scanner userInput;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;


    private static Scene scene;
    private static StackPane musicBarStackPane;

    private static MediaPlayer mediaPlayer;

    public static void main(String[] args) throws IOException, ClassNotFoundException {

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

        Button addSongButton = new Button("Add song");
        addSongButton.setOnAction(e -> addSongMenuCreator());


//        Rectangle musicBar = new Rectangle();
//        musicBar.setFill(Color.BLUE);
//        musicBar.setX(100);
//        musicBar.setY(100);
//        musicBar.setWidth(100);
//        musicBar.setHeight(55); // good

        VBox vBox = new VBox(playButton, pauseButton, addSongButton);


        scene = new Scene(vBox);
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

    public void addSongMenuCreator() {
        Stage addSongStage = new Stage();
        addSongStage.setTitle("add song to library");

        Label songTitleLabel = new Label("song title: ");
        Label artistNameLabel = new Label("artist name: ");
        Label mp3FileLabel = new Label("mp3 file: ");
        VBox addSongLabels = new VBox(songTitleLabel, artistNameLabel, mp3FileLabel);
        addSongLabels.setAlignment(Pos.CENTER_LEFT);
        addSongLabels.setSpacing(10);

        TextField songTitleTextField = new TextField();
        TextField artistNameTextField = new TextField();
        TextField mp3FileTextField = new TextField();
        mp3FileTextField.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser mp3FileChooser = new FileChooser();
                File mp3FileChosenByUser = mp3FileChooser.showOpenDialog(addSongStage);
            }
        });
        VBox addSongTextFieldsAndFileChooser = new VBox(songTitleTextField, artistNameTextField, mp3FileTextField);
        addSongTextFieldsAndFileChooser.setAlignment(Pos.CENTER_RIGHT);

        HBox hbox = new HBox(addSongLabels, addSongTextFieldsAndFileChooser);
        hbox.setAlignment(Pos.CENTER);

        Scene addSongScene = new Scene(hbox);

        addSongStage.setScene(addSongScene);
        addSongStage.setWidth(400);
        addSongStage.setHeight(400);
        addSongStage.setResizable(false);
        addSongStage.show();
    }

    public void sendASongToServer(String songTitle, String artist, File mp3File) {

    }
}
