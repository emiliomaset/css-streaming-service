package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MusicPlayerClient extends Application {

    private static final int PORT1 = 1222;
    private static final int PORT2 = 2111;
    private static final int PORT3 = 3333;
    private Socket socket1;
    private Socket socket2;
    private Socket socket3;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToServer;
    private ObjectInputStream objectInputStreamFromServer;
    private FileInputStream fileInputStream;
    private PrintWriter stringOutputStream;

    private Scene scene;
    private Label titleOfSongCurrentlyPlayingLabel;
    private Label artistOfSongCurrentlyPlayingLabel;

    private Label searchSongMessageLabel;
    private MediaPlayer mediaPlayer;
    private File mp3FileChosenByUser;
    private long size;

    private ArrayList<Song> allSongs = new ArrayList<>();

    private ArrayList<Song> songQueue = new ArrayList<>();

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
            objectInputStreamFromServer = new ObjectInputStream(socket2.getInputStream());
            stringOutputStream = new PrintWriter(socket3.getOutputStream());

            allSongs = (ArrayList<Song>) objectInputStreamFromServer.readObject();
        } catch (UnknownHostException unknownHostException) {
            System.out.println("\nHost not found!");
            System.exit(1);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }


    public static void main(String[] args) {
        launch(args);
    }

    // ===========================================================================================================================

    @Override
    public void start(Stage primaryStage) {

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> playSong());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseSong());

        Button skipButton = new Button("Skip");
        skipButton.setOnAction(e -> skipSong());

        Button addSongButton = new Button("Add song");
        addSongButton.setOnAction(e -> addSongMenuCreator());

        Button searchASong = new Button("Search a song");
        searchASong.setOnAction(e -> searchASongMenuCreator());

        Button viewAllSongs = new Button("View all songs in library");
        viewAllSongs.setOnAction(e -> viewAllSongsMenuCreator());

        Slider volumeSlider = new Slider();

        HBox buttonsHbox = new HBox(playButton, pauseButton, skipButton, addSongButton, viewAllSongs, searchASong, volumeSlider);
        buttonsHbox.setAlignment(Pos.BOTTOM_CENTER);


        titleOfSongCurrentlyPlayingLabel = new Label();
        artistOfSongCurrentlyPlayingLabel = new Label();

        VBox labelsVbox = new VBox(titleOfSongCurrentlyPlayingLabel, artistOfSongCurrentlyPlayingLabel);
        labelsVbox.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(labelsVbox, buttonsHbox);
        vbox.setSpacing(210);

        scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(300);
        primaryStage.setWidth(700);
        primaryStage.setResizable(false);
        primaryStage.show();

    }

    // ===========================================================================================================================

    public void setMediaPlayer(String mp3FileName) {
        if (!(mediaPlayer == null)) // making sure songs dont overlap
            mediaPlayer.pause();
        Media hit = new Media(new File(mp3FileName).toURI().toString());
        mediaPlayer = new MediaPlayer(hit);
    }

    // ===========================================================================================================================

    public void playSong() {
        if (new File("dummy2.mp3").exists()){
            setMediaPlayer("dummy2.mp3");
            mediaPlayer.play();
        }

    }

    // ===========================================================================================================================

    public void pauseSong() {
        mediaPlayer.pause();
    }

    // ===========================================================================================================================

    public void skipSong() {
        System.out.println("queue" + songQueue);
        if (songQueue.size() == 1) {
            mediaPlayer.pause();
            mediaPlayer.seek(Duration.ZERO);
            return;
        }
        songQueue.remove(0);
        searchASong(songQueue.get(0).getSongTitle());
        titleOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getSongTitle());
        artistOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getArtist());
        playSong();
    }

    // ===========================================================================================================================

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

    // ===========================================================================================================================

    public void viewAllSongsMenuCreator() {
        Stage viewALlSongsStage = new Stage();
        viewALlSongsStage.setTitle("all songs in library");

        ScrollPane viewAllSongsScrollPane = new ScrollPane();
        ArrayList<HBox> songCards = new ArrayList<>();
        ArrayList<Label> songCardTitleAndArtistLabelList = new ArrayList<>();
        ArrayList<Button> songCardPlayButtonList = new ArrayList<>();
        ArrayList<Button> songCardDownloadButtonList = new ArrayList<>();
        ArrayList<Button> songCardQueueButtonList = new ArrayList<>();


        for (int i = 0; i < allSongs.size(); i++) {
            songCardTitleAndArtistLabelList.add(new Label(allSongs.get(i).getSongTitle() + " - " + allSongs.get(i).getArtist()));
            songCardPlayButtonList.add(new Button("play"));
            songCardDownloadButtonList.add(new Button("download"));
            songCardQueueButtonList.add(new Button("queue"));
            int finalI = i;
            songCardPlayButtonList.get(i).setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    try {
                        songQueue.add(0, allSongs.get(finalI));
                        for (Song song : songQueue)
                            System.out.println("queue: " + song.getSongTitle());
                        searchASong(allSongs.get(finalI).getSongTitle());
                        Thread.sleep(265);
                        titleOfSongCurrentlyPlayingLabel.setText(allSongs.get(finalI).getSongTitle());
                        artistOfSongCurrentlyPlayingLabel.setText(allSongs.get(finalI).getArtist());
                        playSong();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            songCardDownloadButtonList.get(i).setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    try {
                        searchASong(allSongs.get(finalI).getSongTitle());
                        File songFileToDownload = new File("dummy2.mp3");
                        songFileToDownload.renameTo(new File(allSongs.get(finalI).getSongTitle() + "download.mp3"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            songCardQueueButtonList.get(i).setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    songQueue.add(allSongs.get(finalI));
                    for (Song song : songQueue)
                        System.out.println(song.getSongTitle());

                }
            });
            songCards.add(new HBox(songCardTitleAndArtistLabelList.get(i), songCardPlayButtonList.get(i), songCardDownloadButtonList.get(i), songCardQueueButtonList.get(i)));
            songCards.get(i).setSpacing(5);
            //songCards.get(i).setStyle("-fx-border-color: blue");
        }

        VBox vbox = new VBox();

        for (HBox songCard : songCards) {
            vbox.getChildren().add(songCard);
        }

        vbox.setPadding(new Insets(10));
        vbox.setSpacing(20);


        viewAllSongsScrollPane.setContent(vbox);

        Scene viewAllSongsScene = new Scene(viewAllSongsScrollPane);

        viewALlSongsStage.setScene(viewAllSongsScene);
        viewALlSongsStage.setWidth(400);
        viewALlSongsStage.setHeight(400);
        viewALlSongsStage.setResizable(false);
        viewALlSongsStage.show();
    }

    // ===========================================================================================================================

    public void searchASongMenuCreator() {
        Stage searchASongStage = new Stage();
        searchASongStage.setTitle("search a song");

        TextField searchBar = new TextField();
        Label messageLabel = new Label();
        searchSongMessageLabel = new Label();
        Button searchButton = new Button("search");
        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    searchASong(searchBar.getText());
                } catch (Exception e) {
                    messageLabel.setText("song could not be searched!");
                    searchSongMessageLabel.setText("song could not be searched!");
                    throw new RuntimeException(e);
                }
            }
        });

        VBox searchVbox = new VBox(searchBar, searchButton, searchSongMessageLabel);

        Scene searchASongScene = new Scene(searchVbox);
        searchASongStage.setScene(searchASongScene);

        searchASongStage.setWidth(400);
        searchASongStage.setHeight(400);
        searchASongStage.setResizable(false);
        searchASongStage.show();
    }

    // ===========================================================================================================================

    public void searchASong(String search){
        stringOutputStream.println(search);
        System.out.println("searching: "  + search);
        stringOutputStream.flush();
        receiveASong();
    }

    // ===========================================================================================================================

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

        allSongs.add(song);

    }

    // ===========================================================================================================================

    public void receiveASong() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes = 0;
                File fileReceivedFromServer = new File("dummy2.mp3"); // create dummy file to store song in // delete after use?
                FileOutputStream fileOutputStreamToMakeMp3files;
                try {
                    fileOutputStreamToMakeMp3files = new FileOutputStream(fileReceivedFromServer);
                    size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
                    System.out.println(size);
                    if (size == -1) {
                        Platform.runLater(new Runnable() { // runs this code on main thread where JavaFx stuff is
                            @Override
                            public void run() {
                                searchSongMessageLabel.setText("song not in library!");
                            }
                        });
                        return;
                        //dataInputStreamToReceiveFiles.reset();

                    }
                    byte[] buffer = new byte[4 * 1024];
                    while (size > 0 && (bytes = dataInputStreamToReceiveFiles.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                        fileOutputStreamToMakeMp3files.write(buffer, 0, bytes);
                        size -= bytes;
                    }
                    fileOutputStreamToMakeMp3files.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }).start();
    }

    // ===========================================================================================================================

    public void queueHandler() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.setOnEndOfMedia(new Runnable() {
                    @Override
                    public void run() {
                        if (songQueue.size() > 1) {
                            searchASong(songQueue.get(1).getSongTitle());
                            songQueue.remove(0);
                        }
                    }
                });
            }
        }).start();
    }

}