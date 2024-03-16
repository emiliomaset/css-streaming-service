module com.example.clientserverfinalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens com.example.clientserverfinalproject to javafx.fxml;
    exports com.example.clientserverfinalproject;
}