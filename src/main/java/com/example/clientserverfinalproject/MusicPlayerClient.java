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
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
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
        }
        catch (ClassNotFoundException e) {
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
        playButton.setFont(new Font("Helvetica", 14));
        playButton.setOnAction(e -> playSong());
        playButton.setStyle(getButtonStyling());

        Button pauseButton = new Button("Pause");
        pauseButton.setFont(new Font("Helvetica", 14));
        pauseButton.setOnAction(e -> pauseSong());
        pauseButton.setStyle(getButtonStyling());

        Button skipButton = new Button("Skip");
        skipButton.setFont(new Font("Helvetica", 14));
        skipButton.setOnAction(e -> skipSong());
        skipButton.setStyle(getButtonStyling());

        Button addSongButton = new Button("Add song");
        addSongButton.setFont(new Font("Helvetica", 14));
        addSongButton.setOnAction(e -> addSongMenuCreator());
        addSongButton.setStyle(getButtonStyling());

        Button viewAllSongsButton = new Button("View all songs in library");
        viewAllSongsButton.setFont(new Font("Helvetica", 14));
        viewAllSongsButton.setOnAction(e -> viewAllSongsMenuCreator());
        viewAllSongsButton.setStyle(getButtonStyling());

        Slider volumeSlider = new Slider();
        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() { // anonymous inner class is listening for a change in
                                                                                // Number (of the value property of the slider)
                                                                                // this change will then run changed method
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                mediaPlayer.setVolume(volumeSlider.getValue() * 0.02);
            }
        });


        InputStream stream = new FileInputStream("volumeicon.png/");
        Image img = new Image(stream);
        ImageView imageView = new ImageView(img);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        HBox buttonsHbox = new HBox(playButton, pauseButton, skipButton, addSongButton, viewAllSongsButton, imageView, volumeSlider);
        buttonsHbox.setSpacing(5);
        buttonsHbox.setAlignment(Pos.CENTER);
        //buttonsHbox.setBackground(Background.fill(Color.rgb(37, 150, 190)));


        titleOfSongCurrentlyPlayingLabel = new Label("welcome back to your mp3 player!");
        titleOfSongCurrentlyPlayingLabel.setFont(new Font("Helvetica", 30));

        artistOfSongCurrentlyPlayingLabel = new Label("(◠‿◠✿)");
        artistOfSongCurrentlyPlayingLabel.setFont(new Font("Helvetica", 22));

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
        labelsVbox.setSpacing(5);
        labelsVbox.setPadding(new Insets(20));
        labelsVbox.setAlignment(Pos.CENTER);

        currentSongPosLabel = new Label();
        currentSongPosLabel.setTextAlignment(TextAlignment.LEFT);
        songTotalDurationLabel = new Label();
        songTotalDurationLabel.setTextAlignment(TextAlignment.LEFT);
        HBox songDurationAndSongScrubberHbox = new HBox(currentSongPosLabel, songScrubber, songTotalDurationLabel);
        songDurationAndSongScrubberHbox.setAlignment(Pos.CENTER);
        songDurationAndSongScrubberHbox.setSpacing(12);

        VBox progressBarAndButtonsVBox = new VBox(songDurationAndSongScrubberHbox, buttonsHbox);
        progressBarAndButtonsVBox.setSpacing(28);

        VBox vbox = new VBox(labelsVbox, progressBarAndButtonsVBox);
        vbox.setBackground(Background.fill(Color.rgb(179, 220, 234)));
        vbox.setSpacing(0);

        scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(225);
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
        titleOfSongCurrentlyPlayingLabel.setText("♫ " + songQueue.get(0).getSongTitle() + " ♪");
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
        songTitleLabel.setFont(Font.font("Helvetica", 14));
        songTitleLabel.setPrefWidth(80);
        Label artistNameLabel = new Label("artist name: ");
        artistNameLabel.setFont(Font.font("Helvetica", 14));
        artistNameLabel.setPrefWidth(80);
        Label mp3FileLabel = new Label("mp3 file: ");
        mp3FileLabel.setFont(Font.font("Helvetica", 14));
        mp3FileLabel.setPrefWidth(80);


        Button addSongButton = new Button("add");
        addSongButton.setStyle(getButtonStyling());
        Label messageSentLabel = new Label();

        TextField songTitleTextField = new TextField();
        TextField artistNameTextField = new TextField();
        TextField mp3FileTextField = new TextField();
        mp3FileTextField.setEditable(false);
        mp3FileTextField.setOnMouseClicked(mouseEvent -> {
            mp3FileTextField.setText("");
            FileChooser mp3FileChooser = new FileChooser();
            mp3FileChosenByUser = mp3FileChooser.showOpenDialog(addSongStage);
            if (!mp3FileChosenByUser.toString().contains(".mp3")) {
                messageSentLabel.setBorder(Border.stroke(Color.RED));
                messageSentLabel.setText("file must be an .mp3!");
                addSongButton.setDisable(true);
                return;
            }
            addSongButton.setDisable(false);
            messageSentLabel.setBorder(Border.EMPTY);
            messageSentLabel.setText("");
            mp3FileTextField.setText(mp3FileChosenByUser.getName());
        });

        HBox songTitleLabelAndButtonHbox = new HBox(songTitleLabel, songTitleTextField);
        HBox artistNameLabelandTextFieldHbox = new HBox(artistNameLabel, artistNameTextField);
        HBox mp3FileLabelAndTextFieldHbox = new HBox(mp3FileLabel, mp3FileTextField);


        addSongButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    if (songTitleTextField.getText().isEmpty() || artistNameTextField.getText().isEmpty()
                            || mp3FileTextField.getText().isEmpty()) {
                        messageSentLabel.setText("all fields must be filled out!");
                        messageSentLabel.setBorder(Border.stroke(Color.RED));
                        return;
                    }
                    sendSong(new Song(songTitleTextField.getText().trim(), artistNameTextField.getText().trim(), mp3FileChosenByUser));
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

        VBox vBox = new VBox(songTitleLabelAndButtonHbox, artistNameLabelandTextFieldHbox, mp3FileLabelAndTextFieldHbox,
                addSongButton, messageSentLabel);

        vBox.setPadding(new Insets(15));
        vBox.setSpacing(20);
        vBox.setAlignment(Pos.CENTER);
        vBox.setBackground(Background.fill(Color.valueOf("#E8DFD6")));
        Scene addSongScene = new Scene(vBox);

        addSongStage.setScene(addSongScene);
        addSongStage.setWidth(300);
        addSongStage.setHeight(300);
        addSongStage.setResizable(false);
        addSongStage.show();
    }

    // ===========================================================================================================================

    public void viewAllSongsMenuCreator() {
        Stage viewALlSongsStage = new Stage();
        viewALlSongsStage.setTitle("all songs in library");

        ScrollPane viewAllSongsScrollPane = new ScrollPane();
        ArrayList<HBox> songCards = new ArrayList<>();
        ArrayList<HBox> songCardButtonsHBox = new ArrayList<>();
        ArrayList<Label> songCardTitleAndArtistLabelList = new ArrayList<>();
        ArrayList<Button> songCardPlayButtonList = new ArrayList<>();
        ArrayList<Button> songCardDownloadButtonList = new ArrayList<>();
        ArrayList<Button> songCardQueueButtonList = new ArrayList<>();

        allSongs.sort(new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getSongTitle().toLowerCase().compareTo(o2.getSongTitle().toLowerCase());
            }
        });


        for (int i = 0; i < allSongs.size(); i++) {
            songCardTitleAndArtistLabelList.add(new Label(allSongs.get(i).getSongTitle() + " — " + allSongs.get(i).getArtist()));
            songCardTitleAndArtistLabelList.get(i).setFont(Font.font("Helvetica", 14));
            songCardTitleAndArtistLabelList.get(i).setPrefWidth(270);

            songCardPlayButtonList.add(new Button("play"));
            songCardPlayButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardPlayButtonList.get(i).setStyle(getButtonStyling());

            songCardDownloadButtonList.add(new Button("download"));
            songCardDownloadButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardDownloadButtonList.get(i).setStyle(getButtonStyling());

            songCardQueueButtonList.add(new Button("queue"));
            songCardQueueButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardQueueButtonList.get(i).setStyle(getButtonStyling());

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
                        titleOfSongCurrentlyPlayingLabel.setText("♫ " + songQueue.get(0).getSongTitle() + " ♪");
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


            songCardButtonsHBox.add(new HBox(songCardPlayButtonList.get(i),
                    songCardDownloadButtonList.get(i), songCardQueueButtonList.get(i)));
            songCardButtonsHBox.get(i).setSpacing(9);
            songCards.add(new HBox(songCardTitleAndArtistLabelList.get(i), songCardButtonsHBox.get(i)));
            songCards.get(i).setSpacing(20);

        }

        VBox vbox = new VBox();


        for (HBox songCard : songCards) {
            vbox.getChildren().add(songCard);
            vbox.getChildren().add(new Rectangle(475, 2, Color.rgb(179, 220, 234)));
        }

        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);

        viewAllSongsScrollPane.setContent(vbox);

        Scene viewAllSongsScene = new Scene(viewAllSongsScrollPane);

        viewALlSongsStage.setScene(viewAllSongsScene);
        viewALlSongsStage.setWidth(500);
        viewALlSongsStage.setHeight(500);
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

        dataOutputStream.writeLong(file.length());
        dataOutputStream.flush();
        byte[] buffer = new byte[5000];
        while ((bytes = fileInputStream.read(buffer)) != -1) { // reads 5k bytes from the file at a time (until EOF) and places them in buffer,
                                                               // bytes tracks how many bytes are read into buffer,
            dataOutputStream.write(buffer, 0, bytes);      // dataOutputStream.write writes however many bytes are read into buffer to dataOutputStream
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
                    byte[] buffer = new byte[5000];
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
                    titleOfSongCurrentlyPlayingLabel.setText("♫ " + songQueue.get(0).getSongTitle() + " ♪");
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

    public String getButtonStyling() {
        return "-fx-background-color: \n" +
                "        linear-gradient(#f2f2f2, #d6d6d6),\n" +
                "        linear-gradient(#fcfcfc 0%, #d9d9d9 20%, #d6d6d6 100%),\n" +
                "        linear-gradient(#dddddd 0%, #f6f6f6 50%);\n" +
                "    -fx-background-radius: 8,7,6;\n" +
                "    -fx-background-insets: 0,1,2;\n" +
                "    -fx-text-fill: black;\n" +
                "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );";
    }
}
