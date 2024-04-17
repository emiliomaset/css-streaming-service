A streaming service written in Java that uses socket programming and JavaFX.
On the server side, a directory of mp3 files and a file containing serialized Song objects are accessible to enable the delivery of mp3 files and Song objects to the client.
On the client side, the user receives the mp3 file and Song object of the song they wish to play. Only the song they are playing is stored on their computer, so there is not a huge storage overhead on the client side.
The user can also add songs to the library (and then delete the mp3 files on their computer if they choose since the server now stores the files), and download the mp3 files to their computers via requesting them from the server if they need to.
The functionalities of the streamer include playing, pausing, and skipping songs, volume control, a song queue, and adding and downloading songs.
