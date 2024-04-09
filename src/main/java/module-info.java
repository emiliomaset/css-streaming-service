module com.example.clientserverfinalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires org.apache.commons.io;


    opens com.example.clientserverfinalproject to javafx.fxml;
    exports com.example.clientserverfinalproject;
}