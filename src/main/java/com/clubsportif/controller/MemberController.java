package com.clubsportif.controller;

import com.clubsportif.dao.MemberDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MemberController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Label statusIndicator;
    @FXML private Label planLabel;
    @FXML private Label endDateLabel;
    @FXML private Label priceLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private Label visitsLabel;
    @FXML private Label memberSinceLabel;

    private MemberDAO memberDAO;
    private Member currentMember;
    private int currentUserId;

    @FXML
    public void initialize() {
        memberDAO = new MemberDAO();
        
        // Get current user from session
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            welcomeLabel.setText("Welcome, " + currentUser.getUsername());
            
            // Load member data
            loadMemberData();
        }
    }

    private void loadMemberData() {
        currentMember = memberDAO.getMemberByUserId(currentUserId);
        
        if (currentMember != null) {
            // Update status
            String status = currentMember.getStatus();
            statusLabel.setText(status);
            
            if ("ACTIVE".equals(status)) {
                statusLabel.getStyleClass().clear();
                statusLabel.getStyleClass().add("status-text-active");
                statusIndicator.getStyleClass().clear();
                statusIndicator.getStyleClass().add("status-dot-active");
            } else {
                statusLabel.getStyleClass().clear();
                statusLabel.getStyleClass().add("status-text-expired");
                statusIndicator.getStyleClass().clear();
                statusIndicator.getStyleClass().add("status-dot-expired");
            }
            
            // Update plan details
            planLabel.setText(formatPlanName(currentMember.getSubscription()));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            endDateLabel.setText("Valid until " + currentMember.getDateEnd().format(formatter));
            
            // Update price
            String price = getPriceForPlan(currentMember.getSubscription());
            priceLabel.setText(price);
            
            // Calculate days remaining
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), currentMember.getDateEnd());
            if (daysRemaining > 0) {
                daysRemainingLabel.setText(daysRemaining + " days remaining");
            } else {
                daysRemainingLabel.setText("Expired");
                daysRemainingLabel.getStyleClass().add("expired-text");
            }
            
            // Set member since (using current date as placeholder)
            memberSinceLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        }
    }

    private String formatPlanName(String subscription) {
        return switch (subscription) {
            case "1 month" -> "Monthly Plan";
            case "3 months" -> "3 Months Plan";
            case "1 year" -> "Annual Plan";
            default -> subscription;
        };
    }

    private String getPriceForPlan(String subscription) {
        return switch (subscription) {
            case "1 month" -> "300 DH";
            case "3 months" -> "800 DH";
            case "1 year" -> "2500 DH";
            default -> "N/A";
        };
    }

    @FXML
    public void changePlan() {
        // Navigate to Home page to change plan
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/clubsportif/fxml/Home.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void logout() {
        try {
            // Clear session
            Session.clearSession();
            
            // Navigate to login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/clubsportif/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
