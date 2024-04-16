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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

public class MusicPlayerClient extends Application {
    private static final int PORT1 = 1500;
    private static final int PORT2 = 2100;
    private static final int PORT3 = 3000;
    private Socket socket1;
    private Socket socket2;
    private Socket socket3;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStreamToReceiveFiles;
    private ObjectOutputStream objectOutputStreamToServer;
    private ObjectInputStream objectInputStreamFromServer;
    private FileInputStream fileInputStream;
    private PrintWriter stringOutputStream;

    private ComboBox<String> viewQueueComboBox;
    private Button addSongButton;
    private Button viewAllSongsButton;
    private Label titleOfSongCurrentlyPlayingLabel;
    private Label artistOfSongCurrentlyPlayingLabel;
    private Label currentSongPosLabel;
    private Label songTotalDurationLabel;
    private MediaPlayer mediaPlayer;
    private File mp3FileChosenByUser;
    private ProgressBar songScrubber;
    private Timer timer;
    private double currentPlayingPos;
    private Stage viewAllSongsStage;

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

            allSongs = (ArrayList<Song>) objectInputStreamFromServer.readObject(); /* the server retrieves all songs from the songlibrary.ser file
                                                                                    at time of client connection and sends them to client */
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

    public void setMediaPlayer() {
        if (mediaPlayer != null) // making sure songs dont overlap
            mediaPlayer.pause();

        if (new File("dummy2.mp3").exists()) {
            try {
                FileUtils.copyFile(new File("dummy2.mp3"), new File("songcurrentlyplaying.mp3")); // creates a file with current song data in it
                Media hit = new Media(new File("songcurrentlyplaying.mp3").toURI().toString());
                mediaPlayer = new MediaPlayer(hit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        viewQueueComboBox.getItems().remove(1);
        searchASong(songQueue.get(0).getSongTitle());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        prepareAndPlay();
    }

    // ===========================================================================================================================

    public void prepareAndPlay() {
        titleOfSongCurrentlyPlayingLabel.setText("♫ " + songQueue.get(0).getSongTitle() + " ♪");
        artistOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getArtist());
        setMediaPlayer();
        playSong();
        setTimer();
        queueHandler();
    }

    // ===========================================================================================================================

    public void searchASong(String search){
        stringOutputStream.println(search);
        stringOutputStream.flush();
        receiveASong();
    }

    // ===========================================================================================================================

    public void sendSong(Song song) throws Exception {
        int numOfBytesReadIntoBuffer;
        File file = song.getMp3File();
        fileInputStream = new FileInputStream(file);

        dataOutputStream.writeLong(file.length());
        dataOutputStream.flush();
        byte[] buffer = new byte[5000];
        while ((numOfBytesReadIntoBuffer = fileInputStream.read(buffer)) != -1) { // reads 5k bytes from the file at a time (until EOF) and places them in buffer,
            // dataOutputStream.write writes however many bytes are read into buffer
            // to dataOutputStream
            dataOutputStream.write(buffer, 0, numOfBytesReadIntoBuffer);
            dataOutputStream.flush();
        }

        objectOutputStreamToServer.writeObject(song); // write object to send to be placed in songlibrary.ser
        objectOutputStreamToServer.flush();

        allSongs.add(song);
    }

    // ===========================================================================================================================

    public void receiveASong() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes = 0;
                File fileReceivedFromServer = new File("dummy2.mp3"); // create dummy file to store song in
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

    public void queueHandler() {
        mediaPlayer.setOnEndOfMedia(new Runnable() {
            @Override
            public void run() {
                if (songQueue.size() > 1) {
                    searchASong(songQueue.get(1).getSongTitle());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    songQueue.remove(0);
                    viewQueueComboBox.getItems().remove(1);
                    titleOfSongCurrentlyPlayingLabel.setText("♪ " + songQueue.get(0).getSongTitle() + " ♫");
                    artistOfSongCurrentlyPlayingLabel.setText(songQueue.get(0).getArtist());
                    prepareAndPlay();
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

    public void setTimer() {
        timer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                currentPlayingPos = mediaPlayer.getCurrentTime().toSeconds();
                double endTime = mediaPlayer.getTotalDuration().toSeconds();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        songScrubber.setProgress(currentPlayingPos / endTime);
                        songTotalDurationLabel.setText((int) (mediaPlayer.getTotalDuration().toSeconds() / 60) + ":"
                                + String.format("%02d", (int) (mediaPlayer.getTotalDuration().toSeconds() % 60)));

                        currentSongPosLabel.setText((int) (mediaPlayer.getCurrentTime().toSeconds() / 60) + ":"
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

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException {

        Button playButton = new Button("play");
        playButton.setFont(new Font("Helvetica", 14));
        playButton.setStyle(getButtonStyling());
        playButton.setOnAction(e -> playSong());

        Button pauseButton = new Button("pause");
        pauseButton.setFont(new Font("Helvetica", 14));
        pauseButton.setStyle(getButtonStyling());
        pauseButton.setOnAction(e -> pauseSong());

        Button skipButton = new Button("skip");
        skipButton.setFont(new Font("Helvetica", 14));
        skipButton.setStyle(getButtonStyling());
        skipButton.setOnAction(e -> skipSong());

        addSongButton = new Button("add song");
        addSongButton.setFont(new Font("Helvetica", 14));
        addSongButton.setStyle(getButtonStyling());
        addSongButton.setOnAction(e -> addSongMenuCreator());


        viewQueueComboBox = new ComboBox<>();
        viewQueueComboBox.getItems().add("view queue");
        viewQueueComboBox.getSelectionModel().selectFirst(); // make "view queue" the face of the combo box
        viewQueueComboBox.setStyle("-fx-font: 14px \"Helvetica\";" + getButtonStyling());
        viewQueueComboBox.setPrefWidth(125);
        viewQueueComboBox.setMaxWidth(125);
        viewQueueComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    if (viewQueueComboBox.getSelectionModel().isSelected(0)) { /* since "view queue" is selected again at the end of the handle method,
                                                                                   this just makes sure nothing is done when that happens */
                        return;
                    }

                    int indexOfSongSkippedTo = 0;
                    while (allSongs.get(indexOfSongSkippedTo).getSongTitle()
                            != viewQueueComboBox.getSelectionModel().getSelectedItem()) // find index of song skipped to in allSongs
                        indexOfSongSkippedTo++;

                    searchASong(allSongs.get(indexOfSongSkippedTo).getSongTitle());
                    Thread.sleep(200); // ensure time to download file

                    for (int i = 0; i < viewQueueComboBox.getSelectionModel().getSelectedIndex(); i++) { // remove songs in queue up to chosen one
                        songQueue.remove(0);
                    }

                    prepareAndPlay();

                    int selectedComboBoxIndexWithoutChange = viewQueueComboBox.getSelectionModel().getSelectedIndex(); /* since the index of the selected item changes
                                                                                                                          as you remove items from the combo box,
                                                                                                                          i had to keep it constant to remove items */

                    for (int i = 0; i < selectedComboBoxIndexWithoutChange; i++) { // adjust combo box items accordingly
                        viewQueueComboBox.getItems().remove(1);
                    }

                    viewQueueComboBox.getSelectionModel().selectFirst();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        viewAllSongsButton = new Button("view all songs in library");
        viewAllSongsButton.setFont(new Font("Helvetica", 14));
        viewAllSongsButton.setStyle(getButtonStyling());
        viewAllSongsButton.setOnAction(e -> viewAllSongsMenuCreator());

        Slider volumeSlider = new Slider();
        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() { /* anonymous inner class is listening for a change in
                                                                                   Number (of the value property of the slider)
                                                                                   this change will then run changed method */
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

        HBox buttonsHbox = new HBox(playButton, pauseButton, skipButton, addSongButton, viewQueueComboBox, viewAllSongsButton, imageView, volumeSlider);
        buttonsHbox.setSpacing(5);
        buttonsHbox.setAlignment(Pos.CENTER);

        titleOfSongCurrentlyPlayingLabel = new Label("welcome back to your mp3 player!");
        titleOfSongCurrentlyPlayingLabel.setFont(new Font("Helvetica", 30));

        artistOfSongCurrentlyPlayingLabel = new Label("(◠‿◠✿)");
        artistOfSongCurrentlyPlayingLabel.setFont(new Font("Helvetica", 22));

        songScrubber = new ProgressBar();
        songScrubber.setPrefWidth(600);
        songScrubber.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) { // allows user to seek song with progress bar
                if (mediaPlayer == null)
                    return;

                Bounds boundsOfSongScrubber = songScrubber.getBoundsInLocal();

                double xOfSongScrubberMouseClick = mouseEvent.getSceneX();
                double posToMoveScrubberProgressTo = (((xOfSongScrubberMouseClick - boundsOfSongScrubber.getMinX() ) * 100)
                        / boundsOfSongScrubber.getMaxX());
                posToMoveScrubberProgressTo -= 11.95;
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
        currentSongPosLabel.setFont(new Font("Helvetica", 14));

        songTotalDurationLabel = new Label();
        songTotalDurationLabel.setTextAlignment(TextAlignment.LEFT);
        songTotalDurationLabel.setFont(new Font("Helvetica", 14));

        HBox songDurationAndSongScrubberHbox = new HBox(currentSongPosLabel, songScrubber, songTotalDurationLabel);
        songDurationAndSongScrubberHbox.setAlignment(Pos.CENTER);
        songDurationAndSongScrubberHbox.setSpacing(12);

        VBox progressBarAndButtonsVBox = new VBox(songDurationAndSongScrubberHbox, buttonsHbox);
        progressBarAndButtonsVBox.setSpacing(28);

        VBox vbox = new VBox(labelsVbox, progressBarAndButtonsVBox);
        vbox.setBackground(Background.fill(Color.rgb(179, 220, 234)));
        vbox.setSpacing(0);

        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.setHeight(225);
        primaryStage.setWidth(750);
        primaryStage.setResizable(false);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                try {
                    if (new File("dummy2.mp3").exists())
                        new File("dummy2.mp3").delete();

                    if (new File("songcurrentlyplaying.mp3").exists())
                        new File("songcurrentlyplaying.mp3").delete();

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

    // ===========================================================================================================================================

    public void addSongMenuCreator() {
        addSongButton.setDisable(true);

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

        Button addSongButtonInAddSongWindow = new Button("add");
        addSongButtonInAddSongWindow.setStyle(getButtonStyling());
        addSongButtonInAddSongWindow.setFont(Font.font("Helvetica", 14));

        Label messageSentLabel = new Label();
        messageSentLabel.setFont(Font.font("Helvetica", 16));

        TextField songTitleTextField = new TextField();
        TextField artistNameTextField = new TextField();

        TextField mp3FileTextField = new TextField();
        mp3FileTextField.setEditable(false);
        mp3FileTextField.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                mp3FileTextField.setText("");

                FileChooser mp3FileChooser = new FileChooser();
                mp3FileChosenByUser = mp3FileChooser.showOpenDialog(addSongStage);

                if (!mp3FileChosenByUser.toString().contains(".mp3")) {
                    messageSentLabel.setBorder(Border.stroke(Color.RED));
                    messageSentLabel.setText("file must be an .mp3!");
                    addSongButtonInAddSongWindow.setDisable(true);
                    return;
                }

                addSongButtonInAddSongWindow.setDisable(false);

                messageSentLabel.setBorder(Border.EMPTY);
                messageSentLabel.setText("");

                mp3FileTextField.setText(mp3FileChosenByUser.getName());
            }
        });

        HBox songTitleLabelAndButtonHbox = new HBox(songTitleLabel, songTitleTextField);
        HBox artistNameLabelandTextFieldHbox = new HBox(artistNameLabel, artistNameTextField);
        HBox mp3FileLabelAndTextFieldHbox = new HBox(mp3FileLabel, mp3FileTextField);

        addSongButtonInAddSongWindow.setOnAction(new EventHandler<ActionEvent>() {
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

                    if (viewAllSongsStage != null && viewAllSongsStage.isShowing()) { // close and reopen all songs window if its showing upon adding a new song to library
                        viewAllSongsStage.close();
                        viewAllSongsMenuCreator();
                    }

                } catch (Exception e) {
                    messageSentLabel.setText("song could not be added!");
                    e.printStackTrace();
                }
            }
        });

        VBox everythingVbox = new VBox(songTitleLabelAndButtonHbox, artistNameLabelandTextFieldHbox, mp3FileLabelAndTextFieldHbox,
                addSongButtonInAddSongWindow, messageSentLabel);
        everythingVbox.setPadding(new Insets(15));
        everythingVbox.setSpacing(20);
        everythingVbox.setAlignment(Pos.CENTER);
        everythingVbox.setBackground(Background.fill(Color.valueOf("#E8DFD6")));

        Scene addSongScene = new Scene(everythingVbox);

        addSongStage.setScene(addSongScene);
        addSongStage.setWidth(300);
        addSongStage.setHeight(300);
        addSongStage.setResizable(false);
        addSongStage.show();

        addSongStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                addSongButton.setDisable(false);
            }
        });
    }

    // ===========================================================================================================================

    public void viewAllSongsMenuCreator() {

        viewAllSongsButton.setDisable(true);
        viewAllSongsStage = new Stage();
        viewAllSongsStage.setTitle("all songs in library");

        ScrollPane viewAllSongsScrollPane = new ScrollPane();
        ArrayList<HBox> songCards = new ArrayList<>();
        ArrayList<HBox> songCardButtonsHBox = new ArrayList<>();
        ArrayList<Label> songCardTitleAndArtistLabelList = new ArrayList<>();
        ArrayList<Button> songCardPlayButtonList = new ArrayList<>();
        ArrayList<Button> songCardDownloadButtonList = new ArrayList<>();
        ArrayList<Button> songCardQueueButtonList = new ArrayList<>();

        allSongs.sort(new Comparator<Song>() { // sort songs alphabetically
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getSongTitle().toLowerCase().compareTo(o2.getSongTitle().toLowerCase());
            }
        });


        for (int i = 0; i < allSongs.size(); i++) {
            songCardTitleAndArtistLabelList.add(new Label(allSongs.get(i).getSongTitle() + " — " + allSongs.get(i).getArtist()));
            songCardTitleAndArtistLabelList.get(i).setFont(Font.font("Helvetica", 14));
            songCardTitleAndArtistLabelList.get(i).setPrefWidth(250);
            songCardTitleAndArtistLabelList.get(i).setMaxWidth(250);

            songCardPlayButtonList.add(new Button("play"));
            songCardPlayButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardPlayButtonList.get(i).setStyle(getButtonStyling());

            songCardDownloadButtonList.add(new Button("download"));
            songCardDownloadButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardDownloadButtonList.get(i).setStyle(getButtonStyling());

            songCardQueueButtonList.add(new Button("queue"));
            songCardQueueButtonList.get(i).setFont(Font.font("Helvetica", 13));
            songCardQueueButtonList.get(i).setStyle(getButtonStyling());

            int finalI = i; // redeclaration so that i can be accessed in inner classes
            songCardPlayButtonList.get(i).setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    try {
                        if (songQueue.isEmpty())
                            songQueue.add(0, allSongs.get(finalI));

                        else /* if you have songs queued, and you press play a song, you replace the one you are currently
                                playing with the chosen one, preserving rest of queue */
                            songQueue.set(0, allSongs.get(finalI));

                        searchASong(allSongs.get(finalI).getSongTitle());
                        Thread.sleep(200); // ensure time to download file
                        prepareAndPlay();
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
                        Thread.sleep(100);

                        File songFileToDownload = new File("dummy2.mp3");
                        File downloadedSongsDirectory = new File("songs-downloaded-from-mp3-player");

                        if (!downloadedSongsDirectory.exists()) {
                            downloadedSongsDirectory.mkdir();
                        }

                        songFileToDownload.renameTo(new File("songs-downloaded-from-mp3-player/" + allSongs.get(finalI).getSongTitle() + "download.mp3"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            songCardQueueButtonList.get(i).setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (mediaPlayer == null)
                        return;
                    songQueue.add(allSongs.get(finalI));
                    viewQueueComboBox.getItems().add(allSongs.get(finalI).getSongTitle());
                }
            });

            songCardButtonsHBox.add(new HBox(songCardPlayButtonList.get(i), songCardDownloadButtonList.get(i), songCardQueueButtonList.get(i)));
            songCardButtonsHBox.get(i).setSpacing(9);

            songCards.add(new HBox(songCardTitleAndArtistLabelList.get(i), songCardButtonsHBox.get(i)));
            songCards.get(i).setSpacing(20);
        }

        VBox holdEverythingOnViewAllSongsScreenVbox = new VBox();

        for (HBox songCard : songCards) {
            holdEverythingOnViewAllSongsScreenVbox.getChildren().add(songCard);
            holdEverythingOnViewAllSongsScreenVbox.getChildren().add(new Rectangle(462, 2, Color.rgb(179, 220, 234)));
        }

        holdEverythingOnViewAllSongsScreenVbox.setPadding(new Insets(10));
        holdEverythingOnViewAllSongsScreenVbox.setAlignment(Pos.CENTER);
        holdEverythingOnViewAllSongsScreenVbox.setSpacing(10);

        viewAllSongsScrollPane.setContent(holdEverythingOnViewAllSongsScreenVbox);
        viewAllSongsScrollPane.setFitToHeight(true);

        Scene viewAllSongsScene = new Scene(viewAllSongsScrollPane);

        viewAllSongsStage.setScene(viewAllSongsScene);
        viewAllSongsStage.setWidth(500);
        viewAllSongsStage.setHeight(500);
        viewAllSongsStage.setResizable(false);
        viewAllSongsStage.show();

        viewAllSongsStage.setOnCloseRequest(new EventHandler<WindowEvent>() {  // enable view all songs buttons once all songs window is closed
            @Override
            public void handle(WindowEvent windowEvent) {
                viewAllSongsButton.setDisable(false);
            }
        });
    }

    // ===========================================================================================================================

    public String getButtonStyling() { /* this portion of code copied from
                                          http://fxexperience.com/2011/12/styling-fx-buttons-with-css/ */
        return "-fx-background-color: \n" +
                "        linear-gradient(#f2f2f2, #d6d6d6),\n" +
                "        linear-gradient(#fcfcfc 0%, #d9d9d9 20%, #d6d6d6 100%),\n" +
                "        linear-gradient(#dddddd 0%, #f6f6f6 50%);\n" +
                "    -fx-background-radius: 8,7,6;\n" +
                "    -fx-background-insets: 0,1,2;\n" +
                "    -fx-text-fill: black;\n" +
                "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );";
    }

    // ===========================================================================================================================
}