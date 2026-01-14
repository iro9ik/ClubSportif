package com.clubsportif.controller;

import com.clubsportif.config.DatabaseInitializer;
import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.ReactiveRequestDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.Request;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;
import com.clubsportif.websocket.ClubServerEndpoint;
import com.clubsportif.websocket.WebSocketClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private ReactiveRequestDAO reactiveRequestDAO;
    private MemberDAO memberDAO;
    private int currentUserId;
    
    // WebSocket client for receiving notifications
    private WebSocketClientService wsClient;

    // ================= INITIALIZATION =================
    @FXML
    public void initialize() {
        requestDAO = new RequestDAO();
        reactiveRequestDAO = new ReactiveRequestDAO(requestDAO);
        memberDAO = new MemberDAO();

        // Initialize database tables
        DatabaseInitializer.initializeTables();

        // Get current user ID from session
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();

            // Initialize WebSocket for visitors (to receive acceptance notifications)
            initializeWebSocket(currentUser);

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

    /**
     * Initialize WebSocket client for real-time notifications.
     */
    private void initializeWebSocket(User user) {
        wsClient = new WebSocketClientService();

        // Handle request acceptance - redirect to Member page
        wsClient.setOnRequestAccepted(message -> {
            System.out.println("[Home] Request accepted notification received!");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🎉 Request Accepted!");
            alert.setHeaderText("Your membership request has been approved!");
            alert.setContentText("You are now a member! Redirecting to member page...");
            alert.showAndWait();

            // Update user role in session
            user.setRole("MEMBER");
            Session.setCurrentUser(user);
            
            // Cleanup and navigate to Member page
            cleanup();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/clubsportif/fxml/Member.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) pricingBox.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Handle request declined notification
        wsClient.setOnRequestDeclined(message -> {
            System.out.println("[Home] Request declined notification received");
            
            String reason = message.getPayloadString("reason");
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Request Declined");
            alert.setHeaderText("Your membership request was not approved");
            alert.setContentText(reason != null ? reason : "Please contact administration for more details.");
            alert.showAndWait();

            // Show pricing plans again
            Platform.runLater(this::showPricingPlans);
        });

        // Connect with USER role (visitor)
        wsClient.connect(user.getId(), "USER");
    }

    /**
     * Clean up WebSocket resources.
     */
    public void cleanup() {
        if (wsClient != null) {
            wsClient.shutdown();
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

            // Use reactive DAO
            reactiveRequestDAO.createRequest(request)
                .doOnSuccess(v -> Platform.runLater(() -> {
                    // Send WebSocket notification to admins
                    ClubServerEndpoint.notifyNewRequest(
                        request.getId(),
                        currentUser.getUsername(),
                        selectedPlan
                    );
                    ClubServerEndpoint.notifyDataRefresh("requests");
                    
                    confirmDialog.setVisible(false);
                    showWelcomeMessage();
                }))
                .doOnError(error -> System.err.println("[Home] Failed to create request: " + error.getMessage()))
                .subscribe();
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
                    // Delete the request using reactive DAO
                    reactiveRequestDAO.deleteRequest(latestRequest.getId())
                        .doOnSuccess(v -> Platform.runLater(() -> {
                            // Notify admins about data refresh
                            ClubServerEndpoint.notifyDataRefresh("requests");
                            
                            // Hide welcome box and show pricing plans
                            welcomeBox.setVisible(false);
                            pricingBox.setVisible(true);
                        })).subscribe();
                    return;
                }
            }
        }

        // Fallback: Hide welcome box and show pricing plans
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
        cleanup();
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