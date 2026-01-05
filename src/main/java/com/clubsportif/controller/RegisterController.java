package com.clubsportif.controller;

import com.clubsportif.dao.UserDAO;
import com.clubsportif.model.User;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "/fxml/Login.fxml");
    }

    @FXML
    public void register(ActionEvent event) {

        if (usernameField.getText().isEmpty() ||
            passwordField.getText().isEmpty() ||
            confirmPasswordField.getText().isEmpty()) {

            showAlert("Error", "Please fill all required fields");
            return;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Error", "Passwords do not match");
            return;
        }

        User user = new User(
            usernameField.getText(),
            emailField.getText(),
            phoneField.getText(),
            passwordField.getText(), 
            "VISITOR"
        );

        try {
            userDAO.save(user);
            showAlert("Success", "Account created successfully!");
            switchScene(event, "/fxml/Login.fxml");

        } catch (Exception e) {
            showAlert("Error", "Username already exists");
        }
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
