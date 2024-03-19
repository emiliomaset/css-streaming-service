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
import javafx.scene.input.MouseEvent;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MusicPlayerClient extends Application {

    private static InetAddress host;
    private static final int PORT = 1234;
    private static Socket socket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private static Scene scene;

    private static MediaPlayer mediaPlayer;
    private static File mp3FileChosenByUser;

    public static void main(String[] args) {
        try {
            host = InetAddress.getLocalHost();
        }
        catch (UnknownHostException unknownHostException) {
            System.out.println("\nHost not found!");
            System.exit(1);
        }

        try {
            socket = null;
            socket = new Socket(host, PORT);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

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


    public void playSong()  {
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
                mp3FileChosenByUser = mp3FileChooser.showOpenDialog(addSongStage);
                mp3FileTextField.setText(mp3FileChosenByUser.getName());
                mp3FileTextField.setEditable(false);
            }
        });

        Button addSongButton = new Button("add");
        Label messageSentLabel = new Label();
        addSongButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
//                if (songTitleTextField.getText().isEmpty()) {
//                    songTitleTextField.setBorder(Border.stroke(Color.RED));
//                }
//                if (artistNameTextField.getText().isEmpty()) {
//                    artistNameTextField.setBorder(Border.stroke(Color.RED));
//                }
//                if (mp3FileTextField.getText().isEmpty()) {
//                    mp3FileTextField.setBorder(Border.stroke(Color.RED));
//                }
//
//                if(songTitleTextField.getText().isEmpty() || artistNameTextField.getText().isEmpty() || mp3FileTextField.getText().isEmpty())
//                    return;
                try {
                    objectOutputStream.flush();
                    objectOutputStream.writeObject(new Song(songTitleTextField.getText(), artistNameTextField.getText(), mp3FileChosenByUser));
                }
                catch (IOException e) {
                    messageSentLabel.setText("song could not be added!");
                    throw new RuntimeException(e);
                }

                messageSentLabel.setText("song successfully added to library!");

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                PrintWriter output = null;
                try {
                    output = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                output.println(search);
                while (socket.isConnected()){
                    try {
                        //Song foundSong = (Song) objectInputStream.readObject();
                        System.out.println(objectInputStream.readObject());
                        //mediaPlayer = new MediaPlayer(new Media(foundSong.getMp3File().toURI().toString()));
                        // playSong();
                    }
                    catch (IOException | ClassCastException ioEx){
                        ioEx.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }).start();
    }
}
