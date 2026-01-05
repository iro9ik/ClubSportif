package com.clubsportif.controller;

import com.clubsportif.config.DatabaseInitializer;
import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.Request;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HomeController {

    // ================= UI COMPONENTS =================
    @FXML private VBox mainContent;
    @FXML private HBox pricingBox;
    @FXML private VBox welcomeBox;
    @FXML private VBox memberStatusBox;
    @FXML private VBox confirmDialog;
    @FXML private Label confirmMessage;
    
    // Member status labels
    @FXML private Label membershipStatusLabel;
    @FXML private Label membershipPlanLabel;
    @FXML private Label membershipEndDateLabel;

    // Plan buttons
    @FXML private Button monthlyPlanButton;
    @FXML private Button threeMonthsPlanButton;
    @FXML private Button annualPlanButton;

    // ================= STATE =================
    private String selectedPlan;
    private RequestDAO requestDAO;
    private MemberDAO memberDAO;
    private int currentUserId;

    // ================= INITIALIZATION =================
    @FXML
    public void initialize() {
        requestDAO = new RequestDAO();
        memberDAO = new MemberDAO();
        
        // Initialize database tables
        DatabaseInitializer.initializeTables();
        
        // Get current user ID from session
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            
            // Home page is ONLY for USER role (visitors)
            // Members should be routed to Member.fxml by LoginController
            
            // Check if user has a pending request
            if (requestDAO.hasActiveRequest(currentUserId)) {
                showWelcomeMessage();
            }
            // Otherwise show pricing plans
            else {
                showPricingPlans();
            }
        }
    }

    // ================= DISPLAY METHODS =================
    private void showPricingPlans() {
        pricingBox.setVisible(true);
        welcomeBox.setVisible(false);
        if (memberStatusBox != null) {
            memberStatusBox.setVisible(false);
        }
        confirmDialog.setVisible(false);
    }

    private void showWelcomeMessage() {
        pricingBox.setVisible(false);
        welcomeBox.setVisible(true);
        if (memberStatusBox != null) {
            memberStatusBox.setVisible(false);
        }
        confirmDialog.setVisible(false);
    }

    private void showMemberStatus(Member member) {
        // This method should NOT be called on Home page
        // Members are routed to Member.fxml
        pricingBox.setVisible(false);
        welcomeBox.setVisible(false);
        if (memberStatusBox != null) {
            memberStatusBox.setVisible(true);
            confirmDialog.setVisible(false);
            
            // Update member status labels
            membershipStatusLabel.setText(member.getStatus());
            membershipPlanLabel.setText(member.getSubscription());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            membershipEndDateLabel.setText(member.getDateEnd().format(formatter));
            
            // Update status color
            if ("ACTIVE".equals(member.getStatus())) {
                membershipStatusLabel.getStyleClass().clear();
                membershipStatusLabel.getStyleClass().add("status-active");
            } else {
                membershipStatusLabel.getStyleClass().clear();
                membershipStatusLabel.getStyleClass().add("status-expired");
            }
        }
    }

    // ================= PLAN SELECTION =================
    @FXML
    public void choosePlan(ActionEvent event) {
        // Check if user already has a pending request
        if (requestDAO.hasActiveRequest(currentUserId)) {
            // User already has a pending request, don't allow another
            return;
        }
        
        Button clickedButton = (Button) event.getSource();
        selectedPlan = (String) clickedButton.getUserData();
        
        String price = switch (selectedPlan) {
            case "1 month" -> "300 DH";
            case "3 months" -> "800 DH";
            case "1 year" -> "2500 DH";
            default -> "";
        };
        
        confirmMessage.setText("You selected: " + selectedPlan + "\nPrice: " + price);
        confirmDialog.setVisible(true);
    }

    @FXML
    public void confirmPlanSelection() {
        User currentUser = Session.getCurrentUser();
        if (currentUser != null && selectedPlan != null) {
            Request request = new Request(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getUsername(),
                selectedPlan
            );
            
            requestDAO.createRequest(request);
            confirmDialog.setVisible(false);
            showWelcomeMessage();
        }
    }

    @FXML
    public void cancelDialog() {
        confirmDialog.setVisible(false);
    }

    @FXML
    public void changePlan() {
        // Hide welcome box and show pricing plans
        showPricingPlans();
    }

    @FXML
    public void cancelRequest() {
        // Delete the request from database (not just mark as CANCELED)
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            // Get the latest pending request and delete it
            var requests = requestDAO.getRequestsByUserId(currentUserId);
            if (!requests.isEmpty()) {
                Request latestRequest = requests.get(0); // Most recent
                if ("PENDING".equals(latestRequest.getStatus())) {
                    // Delete the request completely
                    requestDAO.deleteRequest(latestRequest.getId());
                }
            }
        }
        
        // Hide welcome box and show pricing plans
        welcomeBox.setVisible(false);
        pricingBox.setVisible(true);
    }

    @FXML
    public void renewMembership() {
        // Show pricing plans for renewal
        if (memberStatusBox != null) {
            memberStatusBox.setVisible(false);
        }
        pricingBox.setVisible(true);
    }

    @FXML
    public void logout(ActionEvent event) {
        try {
            // Clear session
            Session.clearSession();
            
            // Load login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/clubsportif/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
