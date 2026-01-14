package com.clubsportif.app;

import com.clubsportif.websocket.ClubWebSocketServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void init() throws Exception {
        super.init();
        // Start WebSocket server before UI loads
        ClubWebSocketServer.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(
                FXMLLoader.load(getClass().getResource("/com/clubsportif/fxml/Login.fxml"))
        );
        stage.setTitle("Club Sportif");
        stage.setScene(scene);
        
        // Stop WebSocket server when window closes
        stage.setOnCloseRequest(event -> {
            ClubWebSocketServer.stop();
        });
        
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Ensure WebSocket server is stopped
        ClubWebSocketServer.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
