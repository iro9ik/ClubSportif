module com.clubsportif {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    // Reactor
    requires reactor.core;
    requires org.reactivestreams;
    
    // WebSocket (tyrus-standalone-client provides all)
    requires jakarta.websocket;
    requires org.glassfish.tyrus.core;
    requires org.glassfish.tyrus.client;
    requires org.glassfish.tyrus.spi;
    
    // JSON
    requires com.google.gson;
    
    opens com.clubsportif.app to javafx.fxml;
    opens com.clubsportif.controller to javafx.fxml;
    opens com.clubsportif.model to javafx.fxml, com.google.gson;
    opens com.clubsportif.websocket to com.google.gson, jakarta.websocket, org.glassfish.tyrus.core, org.glassfish.tyrus.client, org.glassfish.tyrus.server;
    
    exports com.clubsportif.app;
    exports com.clubsportif.controller;
    exports com.clubsportif.model;
    exports com.clubsportif.websocket;
    exports com.clubsportif.dao;
    exports com.clubsportif.service;
}
