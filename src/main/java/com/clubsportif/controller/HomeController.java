package com.clubsportif.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;

public class HomeController {

    // ================= PLANS =================
    @FXML private HBox pricingBox; // container of the 3 plans
    @FXML private VBox welcomeBox; // hidden container for welcome message
    @FXML private Label welcomeLabel;
    @FXML private Label instructionLabel;

    // Buttons inside pricing cards
    @FXML private Button monthlyPlanButton;
    @FXML private Button threeMonthsPlanButton;
    @FXML private Button annualPlanButton;

    // ================= LOGOUT =================
    @FXML
    public void logout(ActionEvent event) {
        System.out.println("User logged out");

        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/fxml/Login.fxml")));
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CHOOSE PLAN =================
    @FXML
    public void choosePlan(ActionEvent event) {
        // Hide pricing plans
        pricingBox.setVisible(false);

        // Show welcome message
        welcomeBox.setVisible(true);
        welcomeLabel.setText("Welcome to the club!");
        instructionLabel.setText("Please visit the club's administration desk to complete your registration.");
    }

    @FXML
    public void initialize() {
        // Initially hide welcome message
        if (welcomeBox != null) {
            welcomeBox.setVisible(false);
        }
    }
}
