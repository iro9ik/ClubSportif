package com.clubsportif.controller;

import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.ReactiveMemberDAO;
import com.clubsportif.dao.ReactiveRequestDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.dao.UserDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;
import com.clubsportif.websocket.ClubServerEndpoint;
import com.clubsportif.websocket.WebSocketClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class MemberController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Label statusIndicator;
    @FXML private Label planLabel;
    @FXML private Label endDateLabel;
    @FXML private Label priceLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private Label memberSinceLabel;

    private MemberDAO memberDAO;
    private RequestDAO requestDAO;
    private UserDAO userDAO;
    private ReactiveMemberDAO reactiveMemberDAO;
    private ReactiveRequestDAO reactiveRequestDAO;
    private Member currentMember;
    private int currentUserId;

    // WebSocket client for real-time notifications
    private WebSocketClientService wsClient;

    @FXML
    public void initialize() {
        memberDAO = new MemberDAO();
        requestDAO = new RequestDAO();
        userDAO = new UserDAO();
        reactiveMemberDAO = new ReactiveMemberDAO(memberDAO);
        reactiveRequestDAO = new ReactiveRequestDAO(requestDAO);

        // Get current user from session
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            welcomeLabel.setText("Welcome, " + currentUser.getUsername());

            // Load member data
            loadMemberData();
            
            // Initialize WebSocket client
            initializeWebSocket(currentUser);
        }
    }

    /**
     * Initialize WebSocket client for real-time notifications.
     */
    private void initializeWebSocket(User user) {
        wsClient = new WebSocketClientService();

        // Handle request acceptance notification
        wsClient.setOnRequestAccepted(message -> {
            System.out.println("[Member] Request accepted notification received!");
            
            // Reload member data to show updated subscription
            loadMemberData();
            
            // Show notification alert
            String subscription = message.getPayloadString("subscription");
            String endDate = message.getPayloadString("endDate");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🎉 Request Accepted!");
            alert.setHeaderText("Your membership request has been approved!");
            alert.setContentText("Plan: " + formatPlanName(subscription) + 
                    "\nValid until: " + endDate);
            alert.showAndWait();
        });

        // Handle request declined notification
        wsClient.setOnRequestDeclined(message -> {
            System.out.println("[Member] Request declined notification received");
            
            String reason = message.getPayloadString("reason");
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Request Declined");
            alert.setHeaderText("Your membership request was not approved");
            alert.setContentText(reason != null ? reason : "Please contact administration for more details.");
            alert.showAndWait();
        });

        // Handle data refresh
        wsClient.setOnDataRefresh(message -> {
            loadMemberData();
        });

        // Handle connection state changes
        wsClient.setOnConnectionStateChanged(connected -> {
            System.out.println("[Member] WebSocket connection: " + (connected ? "connected" : "disconnected"));
        });

        // Connect with member role
        wsClient.connect(user.getId(), "MEMBER");
    }

    /**
     * Clean up resources when controller is destroyed.
     */
    public void cleanup() {
        if (wsClient != null) {
            wsClient.shutdown();
        }
    }

    private void loadMemberData() {
        System.out.println("[Member] Loading member data for userId: " + currentUserId);
        
        reactiveMemberDAO.getMemberByUserId(currentUserId)
            .doOnNext(member -> {
                System.out.println("[Member] Found member: " + member.getNom() + " " + member.getPrenom());
                currentMember = member;
                Platform.runLater(() -> updateMemberUI(member));
            })
            .doOnError(error -> {
                System.err.println("[Member] Failed to load member data: " + error.getMessage());
                Platform.runLater(this::showNoMembershipUI);
            })
            .switchIfEmpty(Mono.defer(() -> {
                System.out.println("[Member] No member found for userId: " + currentUserId);
                Platform.runLater(this::showNoMembershipUI);
                return Mono.empty();
            }))
            .subscribe();
    }

    private void updateMemberUI(Member member) {
        if (member != null) {
            // Update status
            String status = member.getStatus();
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
            planLabel.setText(formatPlanName(member.getSubscription()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            endDateLabel.setText("Valid until " + member.getDateEnd().format(formatter));

            // Update price
            String price = getPriceForPlan(member.getSubscription());
            priceLabel.setText(price);

            // Calculate days remaining
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), member.getDateEnd());
            if (daysRemaining > 0) {
                daysRemainingLabel.setText(daysRemaining + " days remaining");
                daysRemainingLabel.getStyleClass().remove("expired-text");
            } else {
                daysRemainingLabel.setText("Expired");
                daysRemainingLabel.getStyleClass().add("expired-text");
            }

            // Set member since using stored dateStart
            DateTimeFormatter monthYear = DateTimeFormatter.ofPattern("MMM yyyy");
            if (member.getDateStart() != null) {
                memberSinceLabel.setText(member.getDateStart().format(monthYear));
            } else {
                memberSinceLabel.setText(LocalDate.now().format(monthYear));
            }
        } else {
            showNoMembershipUI();
        }
    }

    private void showNoMembershipUI() {
        statusLabel.setText("NO MEMBERSHIP");
        daysRemainingLabel.setText("-");
        planLabel.setText("No plan");
        endDateLabel.setText("N/A");
        priceLabel.setText("-");
        memberSinceLabel.setText("-");
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

    // New: Manage Plan -> opens dialog to choose a plan and send a request to admin
    @FXML
    public void openManagePlan() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Plan");
        dialog.setHeaderText("Choose a plan to request an upgrade / change");

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20;");

        ToggleGroup group = new ToggleGroup();
        RadioButton rb1 = new RadioButton("1 month — 300 DH");
        rb1.setUserData("1 month");
        rb1.setToggleGroup(group);

        RadioButton rb3 = new RadioButton("3 months — 800 DH");
        rb3.setUserData("3 months");
        rb3.setToggleGroup(group);

        RadioButton rby = new RadioButton("1 year — 2500 DH");
        rby.setUserData("1 year");
        rby.setToggleGroup(group);

        // default selection: next plan or 3 months
        group.selectToggle(rb3);

        content.getChildren().addAll(new Label("Select Plan:"), rb1, rb3, rby);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // style buttons
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("dialog-card");
        Node okBtn = pane.lookupButton(ButtonType.OK);
        if (okBtn instanceof Button) ((Button) okBtn).getStyleClass().add("button-primary");
        Node cancelBtn = pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn instanceof Button) ((Button) cancelBtn).getStyleClass().add("button-ghost");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Toggle selected = group.getSelectedToggle();
                if (selected != null) {
                    String plan = (String) selected.getUserData();

                    User currentUser = Session.getCurrentUser();
                    if (currentUser != null) {
                        // create a request for admin approval
                        com.clubsportif.model.Request req = new com.clubsportif.model.Request(
                                currentUser.getId(),
                                currentUser.getUsername(),
                                currentUser.getUsername(),
                                plan
                        );
                        
                        // Use reactive DAO
                        reactiveRequestDAO.createRequest(req)
                            .doOnSuccess(v -> Platform.runLater(() -> {
                                // Send WebSocket notification to admins
                                ClubServerEndpoint.notifyNewRequest(
                                    req.getId(),
                                    currentUser.getUsername(),
                                    plan
                                );
                                ClubServerEndpoint.notifyDataRefresh("requests");

                                Alert a = new Alert(Alert.AlertType.INFORMATION);
                                a.setTitle("Request Sent");
                                a.setHeaderText(null);
                                a.setContentText("Your plan change request was sent to the administration.");
                                a.showAndWait();
                            }))
                            .doOnError(error -> System.err.println("[Member] Failed to create request: " + error.getMessage()))
                            .subscribe();
                    }
                }
            }
        });
    }

    // New: Cancel Plan -> confirm, expire membership, update role, and return to home (visitor)
    @FXML
    public void confirmCancelPlan() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cancel Subscription");
        dialog.setHeaderText("Are you sure you want to cancel your subscription?");
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 18;");
        content.getChildren().add(new Label("Cancelling will deactivate your membership and revert your account to a visitor."));

        dialog.getDialogPane().setContent(content);

        ButtonType confirm = new ButtonType("Yes, Cancel", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(confirm, no);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("dialog-card");
        Node confirmNode = pane.lookupButton(confirm);
        if (confirmNode instanceof Button) ((Button) confirmNode).getStyleClass().add("button-danger");
        Node cancelNode = pane.lookupButton(no);
        if (cancelNode instanceof Button) ((Button) cancelNode).getStyleClass().add("button-ghost");

        dialog.showAndWait().ifPresent(response -> {
            if (response == confirm) {
                // expire membership
                if (currentMember != null) {
                    currentMember.setStatus("EXPIRED");
                    currentMember.setDateEnd(LocalDate.now()); // end today
                    
                    reactiveMemberDAO.updateMember(currentMember)
                        .doOnSuccess(v -> Platform.runLater(() -> {
                            // Notify about status change
                            ClubServerEndpoint.notifyDataRefresh("members");
                        })).subscribe();
                }

                // update user role to VISITOR
                User currentUser = Session.getCurrentUser();
                if (currentUser != null) {
                    UserDAO userDAO = new UserDAO();
                    userDAO.updateUserRole(currentUser.getId(), "VISITOR");
                    // update session object
                    currentUser.setRole("VISITOR");
                    Session.setCurrentUser(currentUser);
                }

                // Cleanup WebSocket before navigating
                cleanup();

                // go back to Home (visitor)
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
        });
    }

    @FXML
    public void changePlan() {
        // Keep compatibility — navigate to Home page to change plan
        cleanup();
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
        cleanup();
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
