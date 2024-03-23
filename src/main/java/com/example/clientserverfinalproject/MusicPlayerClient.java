package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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

    private static final int PORT = 1234;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private PrintWriter stringOutputStream;

    private Scene scene;

    private MediaPlayer mediaPlayer;
    private File mp3FileChosenByUser;

//    public MusicPlayerClient(Socket socket) {
//        this.socket = socket;
//        try {
//            this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
//            this.dataInputStream = new DataInputStream(this.socket.getInputStream());
//            this.stringOutputStream = new PrintWriter(this.socket.getOutputStream());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

    public MusicPlayerClient() {
        InetAddress host;

        try {
            host = InetAddress.getLocalHost();
            socket = new Socket(host, PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            stringOutputStream = new PrintWriter(socket.getOutputStream());
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
                } catch (IOException e) {
                    messageSentLabel.setText("song could not be added!");
                    e.printStackTrace();
                } catch (Exception e) {
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
        Button searchButton = new Button("search");
        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    searchASong(searchBar.getText());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        VBox searchVbox = new VBox(searchBar, searchButton);

        Scene searchASongScene = new Scene(searchVbox);
        searchASongStage.setScene(searchASongScene);

        searchASongStage.setWidth(400);
        searchASongStage.setHeight(400);
        searchASongStage.setResizable(false);
        searchASongStage.show();
    }

    public void searchASong(String search) throws IOException {
        PrintWriter output = null;
        try {
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        output.println(search);

        //File foundSong = (Song) objectInputStream.readObject();
        //System.out.println(objectInputStream.readObject());
        //mediaPlayer = new MediaPlayer(new Media(foundSong.getMp3File().toURI().toString()));
        // playSong();

    }



    public void sendSong(Song song) throws Exception {
        int bytes = 0;
        File file = song.getMp3File();
        fileInputStream = new FileInputStream(file);


        System.out.println(socket.isClosed());
        dataOutputStream.writeLong(file.length()); // null????
        dataOutputStream.flush();
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }
        //fileInputStream.close(); // culprit

        stringOutputStream.println(song.getSongTitle());
        stringOutputStream.println(song.getArtist());
        stringOutputStream.flush();
    }
}


