module com.clubsportif {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    opens com.clubsportif.app to javafx.fxml;
    opens com.clubsportif.controller to javafx.fxml;
    opens com.clubsportif.model to javafx.fxml;
    
    exports com.clubsportif.app;
    exports com.clubsportif.controller;
    exports com.clubsportif.model;
}
