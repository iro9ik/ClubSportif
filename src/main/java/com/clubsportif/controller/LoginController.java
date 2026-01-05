package com.clubsportif.controller;

import com.clubsportif.model.User;
import com.clubsportif.service.AuthService;
import com.clubsportif.util.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    public void goToRegister(ActionEvent event) {
        switchScene(event, "/fxml/Register.fxml");
    }

    @FXML
    public void login(ActionEvent event) {

        if (usernameField.getText().isEmpty() ||
            passwordField.getText().isEmpty()) {

            showAlert("Error", "Please enter username and password");
            return;
        }

        authService.authenticate(
                usernameField.getText(),
                passwordField.getText()
        ).ifPresentOrElse(user -> {

            Session.setCurrentUser(user);

            switch (user.getRole()) {
                case "ADMIN" -> switchScene(event, "/fxml/Admin.fxml");
                case "MEMBER" -> switchScene(event, "/fxml/Member.fxml");
                default -> switchScene(event, "/fxml/Home.fxml");
            }

        }, () -> showAlert("Error", "Invalid username or password"));
    }


    private void switchScene(ActionEvent event, String fxml) {
        try {
            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(
                    FXMLLoader.load(getClass().getResource(fxml))
            ));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
