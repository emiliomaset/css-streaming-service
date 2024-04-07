package com.example.clientserverfinalproject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class MusicPlayerClient extends Application {

    private static final int PORT1 = 1455;
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
    private Label currentSongPosLabel;
    private Label songTotalDurationLabel;
    private MediaPlayer mediaPlayer;
    private File mp3FileChosenByUser;
    private ProgressBar songScrubber;
    private Timer timer;
    private TimerTask timerTask;
    private double currentPlayingPos;

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
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ===========================================================================================================================

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException {

        Button playButton = new Button("Play");
        playButton.setOnAction(e -> playSong());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseSong());

        Button skipButton = new Button("Skip");
        skipButton.setOnAction(e -> skipSong());

        Button addSongButton = new Button("Add song");
        addSongButton.setOnAction(e -> addSongMenuCreator());

        Button viewAllSongs = new Button("View all songs in library");
        viewAllSongs.setOnAction(e -> viewAllSongsMenuCreator());

        Slider volumeSlider = new Slider();
        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() { // anonymous inner class is listening for a change in
                                                                                // Number (of the value property of the slider)
                                                                                // this change will then run changed method
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
            }
        });


        InputStream stream = new FileInputStream("/Users/emiliomaset/IdeaProjects/ClientServerFinalProject/volumeicon.png/");
        Image img = new Image(stream);
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        HBox buttonsHbox = new HBox(playButton, pauseButton, skipButton, addSongButton, viewAllSongs, imageView, volumeSlider);
        buttonsHbox.setSpacing(5);
        buttonsHbox.setAlignment(Pos.CENTER);


        titleOfSongCurrentlyPlayingLabel = new Label();
        titleOfSongCurrentlyPlayingLabel.setFont(new Font(28));

        artistOfSongCurrentlyPlayingLabel = new Label();
        artistOfSongCurrentlyPlayingLabel.setFont(new Font(20));

        songScrubber = new ProgressBar();
        songScrubber.setPrefWidth(600);
        songScrubber.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mediaPlayer == null)
                    return;

                Bounds boundsOfSongScrubber = songScrubber.getBoundsInLocal();
                double xOfSongScrubberMouseClick = mouseEvent.getSceneX();
                double posToMoveScrubberProgressTo = (((xOfSongScrubberMouseClick - boundsOfSongScrubber.getMinX() ) * 100)
                                                        / boundsOfSongScrubber.getMaxX());
                posToMoveScrubberProgressTo -= 7.75;
                posToMoveScrubberProgressTo /= 100;
                songScrubber.setProgress(posToMoveScrubberProgressTo);
                mediaPlayer.seek(Duration.millis( posToMoveScrubberProgressTo * mediaPlayer.getTotalDuration().toMillis()));
                currentPlayingPos = posToMoveScrubberProgressTo * mediaPlayer.getTotalDuration().toMillis(); // adjust currentPlayingPos for timer
            }
        });


        VBox labelsVbox = new VBox(titleOfSongCurrentlyPlayingLabel, artistOfSongCurrentlyPlayingLabel);
        labelsVbox.setSpacing(7);
        labelsVbox.setPadding(new Insets(20));
        labelsVbox.setAlignment(Pos.CENTER);

        currentSongPosLabel = new Label();
        currentSongPosLabel.setTextAlignment(TextAlignment.LEFT);
        songTotalDurationLabel = new Label();
        songTotalDurationLabel.setTextAlignment(TextAlignment.LEFT);
        HBox songDurationAndSongScrubberHbox = new HBox(currentSongPosLabel, songScrubber, songTotalDurationLabel);
        songDurationAndSongScrubberHbox.setAlignment(Pos.CENTER);
        songDurationAndSongScrubberHbox.setSpacing(12);

        VBox vbox = new VBox(labelsVbox, songDurationAndSongScrubberHbox, buttonsHbox);
        vbox.setSpacing(32);

        scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(250);
        primaryStage.setWidth(700);
        primaryStage.setResizable(false);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                try {
                    dataOutputStream.close();
                    objectOutputStreamToServer.close();
                    objectInputStreamFromServer.close();
                    stringOutputStream.close();
                    dataInputStreamToReceiveFiles.close();
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Platform.exit();
                    System.exit(1);
                }
            }
        });


    }

    // ===========================================================================================================================

    public void setMediaPlayer(String mp3FileName) {
        if (mediaPlayer != null) // making sure songs dont overlap
            mediaPlayer.pause();
        Media hit = new Media(new File(mp3FileName).toURI().toString());
        mediaPlayer = new MediaPlayer(hit);
    }

    // ===========================================================================================================================

    public void setTimer() {
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {

                currentPlayingPos = mediaPlayer.getCurrentTime().toSeconds();
                double endTime = mediaPlayer.getTotalDuration().toSeconds();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        songScrubber.setProgress(currentPlayingPos / endTime);
                        songTotalDurationLabel.setText( (int) (mediaPlayer.getTotalDuration().toSeconds() / 60) +  ":"
                                + String.format("%02d", (int) (mediaPlayer.getTotalDuration().toSeconds() % 60)));

                        currentSongPosLabel.setText( (int) (mediaPlayer.getCurrentTime().toSeconds() / 60) +  ":"
                                + String.format("%02d", (int) (mediaPlayer.getCurrentTime().toSeconds() % 60)));
                    }
                });

                if (currentPlayingPos / endTime == 1) { // once song plays all the way through

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            currentSongPosLabel.setText("0:00");
                            songScrubber.setProgress(0);
                        }
                    });
                    timer.cancel();
                }
            }
        };

        timer.schedule(timerTask, 0, 500); // this performs the timerTask every 500ms, as indicated by the third parm

    }

    // ===========================================================================================================================

    public void setMediaPlayer() {
        if (new File("dummy2.mp3").exists()) {
            setMediaPlayer("dummy2.mp3");
        }
    }

    // ===========================================================================================================================

    public void playSong() {
        mediaPlayer.play();
    }

    // ===========================================================================================================================

    public void pauseSong() {
        mediaPlayer.pause();
    }

    // ===========================================================================================================================

    public void skipSong() {
        if (songQueue.size() == 1) { // prevents song overlap
            mediaPlayer.pause();
            mediaPlayer.seek(Duration.ZERO);
            return;
        }
        songQueue.remove(0);
        searchASong(songQueue.get(0).getSongTitle());
        titleOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getSongTitle());
        artistOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getArtist());
        setMediaPlayer();
        playSong();
        queueHandler();
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
                    if (songTitleTextField.getText().isEmpty() || artistNameTextField.getText().isEmpty()
                            || mp3FileTextField.getText().isEmpty()) {
                        messageSentLabel.setText("all fields must be filled out!");
                        return;
                    }
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

        VBox addSongTextFieldsAndFileChooserVbox = new VBox(songTitleTextField, artistNameTextField, mp3FileTextField, messageSentLabel);
        addSongTextFieldsAndFileChooserVbox.setAlignment(Pos.CENTER_RIGHT);

        HBox hbox = new HBox(addSongLabels, addSongTextFieldsAndFileChooserVbox, addSongButton);
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
                        if (songQueue.size() == 1)
                            songQueue.set(0, allSongs.get(finalI));
                        else
                            songQueue.add(0, allSongs.get(finalI));
                        searchASong(allSongs.get(finalI).getSongTitle());
                        Thread.sleep(265); // ensure time to download file
                        titleOfSongCurrentlyPlayingLabel.setText(allSongs.get(finalI).getSongTitle());
                        artistOfSongCurrentlyPlayingLabel.setText(allSongs.get(finalI).getArtist());
                        setMediaPlayer();
                        playSong();
                        setTimer();
                        queueHandler();
                    } catch (Exception e) {
                       e.printStackTrace();
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
                }
            });


            songCards.add(new HBox(songCardTitleAndArtistLabelList.get(i), songCardPlayButtonList.get(i),
                    songCardDownloadButtonList.get(i), songCardQueueButtonList.get(i)));
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

    public void searchASong(String search){
        stringOutputStream.println(search);
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
                    long size = dataInputStreamToReceiveFiles.readLong(); // get song file size from client
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

    public void queueHandler() { // needs work
        mediaPlayer.setOnEndOfMedia(new Runnable() {
            @Override
            public void run() {
                if (songQueue.size() > 1) {
                    searchASong(songQueue.get(1).getSongTitle());
                    songQueue.remove(0);
                    titleOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getSongTitle());
                    artistOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getArtist());
                    setMediaPlayer();
                    queueHandler();
                    playSong();
                    return;
                }

                if (songQueue.size() == 1) {
                    mediaPlayer.seek(new Duration(0));
                    mediaPlayer.pause();
                }
            }
        });
    }

    // ===========================================================================================================================

}
