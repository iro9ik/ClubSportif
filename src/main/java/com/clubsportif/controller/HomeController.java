package com.clubsportif.controller;

import com.clubsportif.config.DatabaseInitializer;
import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.Request;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HomeController {

    // ================= UI COMPONENTS =================
    @FXML private VBox mainContent;
    @FXML private javafx.scene.layout.HBox pricingBox;
    @FXML private VBox welcomeBox;
    @FXML private VBox memberStatusBox;
    @FXML private VBox confirmDialog;
    @FXML private Label confirmMessage;
    @FXML private Pagination featuresPagination;

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

    // carousel autoplay
    private Timeline featuresAutoPlay;

    // ================= INITIALIZATION =================
    @FXML
    public void initialize() {
        requestDAO = new RequestDAO();
        memberDAO = new MemberDAO();

        // Initialize database tables
        DatabaseInitializer.initializeTables();

        // Setup the features carousel
        setupFeaturesCarousel();

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
        } else {
            // No user -> show pricing as default
            showPricingPlans();
        }
    }

    // ================= FEATURE CAROUSEL =================
    private void setupFeaturesCarousel() {
        // Page factory to create three feature slides
        featuresPagination.setPageFactory(pageIndex -> {
            VBox slide = new VBox(12);
            slide.getStyleClass().add("feature-slide");
            slide.setPrefWidth(920);
            slide.setMaxWidth(920);

            Label title = new Label();
            title.getStyleClass().add("feature-title");
            Label desc = new Label();
            desc.getStyleClass().add("feature-desc");
            desc.setWrapText(true);
            desc.setMaxWidth(880);

            switch (pageIndex) {
                case 0 -> {
                    title.setText("State-of-the-art Equipment");
                    desc.setText("Cutting-edge machines and free weights, maintained daily and designed for all fitness levels.");
                }
                case 1 -> {
                    title.setText("Expert Coaches & Classes");
                    desc.setText("Certified trainers run small group classes and personalized coaching to help you meet goals.");
                }
                default -> {
                    title.setText("Community & Wellness");
                    desc.setText("A friendly, motivating atmosphere with nutrition advice, workshops and member events.");
                }
            }

            slide.getChildren().addAll(title, desc);
            slide.setPadding(new javafx.geometry.Insets(18));
            return slide;
        });

        // autoplay carousel (rotate every 4s)
        featuresAutoPlay = new Timeline(new KeyFrame(Duration.seconds(4), ev -> {
            int next = (featuresPagination.getCurrentPageIndex() + 1) % featuresPagination.getPageCount();
            featuresPagination.setCurrentPageIndex(next);
        }));
        featuresAutoPlay.setCycleCount(Timeline.INDEFINITE);
        featuresAutoPlay.play();

        // pause on hover
        featuresPagination.setOnMouseEntered(e -> featuresAutoPlay.pause());
        featuresPagination.setOnMouseExited(e -> featuresAutoPlay.play());
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
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
