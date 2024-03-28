package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.FieldPosition;
import java.util.Scanner;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    private static final int PORT1 = 1234;
    private static final int PORT2 = 4321;
    private static final int PORT3 = 1324;
    private Socket socket1;
    private Socket socket2;
    private Socket socket3;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToServer;
    private FileInputStream fileInputStream;
    private PrintWriter stringOutputStream;

    private Scene scene;

    private MediaPlayer mediaPlayer;
    private File mp3FileChosenByUser;

    public MusicPlayerClient() {
        InetAddress host;

        try {
            host = InetAddress.getLocalHost();
            socket1 = new Socket(host, PORT1);
            socket2 = new Socket(host, PORT2);
            socket3 = new Socket(host, PORT3);
            dataOutputStream = new DataOutputStream(socket1.getOutputStream());
            dataInputStreamToReceiveFiles = new DataInputStream(socket1.getInputStream());
            objectOutputStreamToServer = new ObjectOutputStream(socket2.getOutputStream());
            stringOutputStream = new PrintWriter(socket3.getOutputStream());
        } catch (UnknownHostException unknownHostException) {
            System.out.println("\nHost not found!");
            System.exit(1);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

    }

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> playSong());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseSong());

        Button addSongButton = new Button("Add song");
        addSongButton.setOnAction(e -> addSongMenuCreator());

        Button searchASong = new Button("Search a song");
        searchASong.setOnAction(e -> searchASongMenuCreator());

        Button viewAllSongs = new Button("View all songs in library");
        viewAllSongs.setOnAction(e -> viewAllSongsMenuCreator());

        VBox vBox = new VBox(playButton, pauseButton, addSongButton, viewAllSongs, searchASong);

        scene = new Scene(vBox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(500);
        primaryStage.setWidth(500);
        primaryStage.show();

    }

    public void setMediaPlayer(String mp3FileName) {
        Media hit = new Media(new File(mp3FileName).toURI().toString());
        mediaPlayer = new MediaPlayer(hit);
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
        mp3FileTextField.setOnMouseClicked(mouseEvent -> {
            FileChooser mp3FileChooser = new FileChooser();
            mp3FileChosenByUser = mp3FileChooser.showOpenDialog(addSongStage);
            mp3FileTextField.setText(mp3FileChosenByUser.getName());
            mp3FileTextField.setEditable(false);
        });

        Button addSongButton = new Button("add");
        Label messageSentLabel = new Label();
        addSongButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    sendSong(new Song(songTitleTextField.getText(), artistNameTextField.getText(), mp3FileChosenByUser));
                    messageSentLabel.setText("song successfully added to library!");
                    songTitleTextField.setText("");
                    artistNameTextField.setText("");
                    mp3FileTextField.setText("");
                } catch (Exception e) {
                    messageSentLabel.setText("song could not be added!");
                    e.printStackTrace();
                }
            }
        });

        VBox addSongTextFieldsAndFileChooser = new VBox(songTitleTextField, artistNameTextField, mp3FileTextField, messageSentLabel);
        addSongTextFieldsAndFileChooser.setAlignment(Pos.CENTER_RIGHT);

        HBox hbox = new HBox(addSongLabels, addSongTextFieldsAndFileChooser, addSongButton);
        hbox.setAlignment(Pos.CENTER);

        Scene addSongScene = new Scene(hbox);

        addSongStage.setScene(addSongScene);
        addSongStage.setWidth(400);
        addSongStage.setHeight(400);
        addSongStage.setResizable(false);
        addSongStage.show();
    }

    public void viewAllSongsMenuCreator() {
        Stage viewALlSongsStage = new Stage();
        viewALlSongsStage.setTitle("all songs in library");

    }

    public void searchASongMenuCreator() {
        Stage searchASongStage = new Stage();
        searchASongStage.setTitle("search a song");

        TextField searchBar = new TextField();
        Label messageLabel = new Label();
        Button searchButton = new Button("search");
        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    searchASong(searchBar.getText());
                } catch (Exception e) {
                    messageLabel.setText("song could not be searched!");
                    throw new RuntimeException(e);
                }
            }
        });



        VBox searchVbox = new VBox(searchBar, searchButton, messageLabel);

        Scene searchASongScene = new Scene(searchVbox);
        searchASongStage.setScene(searchASongScene);

        searchASongStage.setWidth(400);
        searchASongStage.setHeight(400);
        searchASongStage.setResizable(false);
        searchASongStage.show();
    }

    public void searchASong(String search) throws Exception {
        stringOutputStream.println(search);
        stringOutputStream.flush();
        receiveASong();
    }



    public void sendSong(Song song) throws Exception {
        int bytes = 0;
        File file = song.getMp3File();
        fileInputStream = new FileInputStream(file);

        dataOutputStream.writeLong(file.length()); // activating searchedSong = input.nextLine() in analyzeSearch() method in server?
        dataOutputStream.flush();
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }

        objectOutputStreamToServer.writeObject(song);
        objectOutputStreamToServer.flush();

    }

    public void receiveASong() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes = 0;
                File fileReceivedFromServer = new File("dummy2.mp3"); // create dummy file to store song in // delete after use?
                FileOutputStream fileOutputStreamToMakeMp3iles;
                try {
                    fileOutputStreamToMakeMp3iles = new FileOutputStream(fileReceivedFromServer);
                    long size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
                    System.out.println(size);
                    byte[] buffer = new byte[4 * 1024];
                    while (size > 0 && (bytes = dataInputStreamToReceiveFiles.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                        fileOutputStreamToMakeMp3iles.write(buffer, 0, bytes);
                        size -= bytes;
                    }
                    fileOutputStreamToMakeMp3iles.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

//                setMediaPlayer("dummy2.mp3");
//                playSong();

            }
        }).start();
    }
}





