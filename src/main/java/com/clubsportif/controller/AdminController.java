package com.clubsportif.controller;

import com.clubsportif.config.DatabaseInitializer;
import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.ReactiveMemberDAO;
import com.clubsportif.dao.ReactiveRequestDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.dao.UserDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.Request;
import com.clubsportif.model.User;
import com.clubsportif.service.ReactiveStatsService;
import com.clubsportif.service.Session;
import com.clubsportif.websocket.ClubServerEndpoint;
import com.clubsportif.websocket.WebSocketClientService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.Node;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import reactor.core.Disposable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AdminController {

    // ================= UI COMPONENTS =================
    // Navigation
    @FXML private Label adminNameLabel;
    @FXML private Button dashboardBtn;
    @FXML private Button membersBtn;
    @FXML private Button requestsBtn;

    // Views
    @FXML private VBox dashboardView;
    @FXML private VBox membersView;
    @FXML private VBox requestsView;

    // Dashboard Stats
    @FXML private Label totalMembersLabel;
    @FXML private Label dailyRequestsLabel;
    @FXML private Label activeMembersLabel;

    // Members Table
    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, String> memberIdCol;
    @FXML private TableColumn<Member, String> memberNomCol;
    @FXML private TableColumn<Member, String> memberPrenomCol;
    @FXML private TableColumn<Member, String> memberSubscriptionCol;
    @FXML private TableColumn<Member, String> memberDateEndCol;
    @FXML private TableColumn<Member, String> memberStatusCol;
    @FXML private TableColumn<Member, Void> memberActionsCol;

    // Requests Table
    @FXML private TableView<Request> requestsTable;
    @FXML private TableColumn<Request, String> requestIdCol;
    @FXML private TableColumn<Request, String> requestNomCol;
    @FXML private TableColumn<Request, String> requestPrenomCol;
    @FXML private TableColumn<Request, String> requestSubscriptionCol;
    @FXML private TableColumn<Request, String> requestDateCol;
    @FXML private TableColumn<Request, String> requestStatusCol;
    @FXML private TableColumn<Request, Void> requestActionsCol;

    // ================= DATA =================
    private MemberDAO memberDAO;
    private RequestDAO requestDAO;
    private ReactiveMemberDAO reactiveMemberDAO;
    private ReactiveRequestDAO reactiveRequestDAO;
    private ReactiveStatsService statsService;
    private ObservableList<Member> membersList;
    private ObservableList<Request> requestsList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    // ================= REACTIVE & WEBSOCKET =================
    private WebSocketClientService wsClient;
    private Disposable statsSubscription;

    // ================= INITIALIZATION =================
    @FXML
    public void initialize() {
        memberDAO = new MemberDAO();
        requestDAO = new RequestDAO();
        reactiveMemberDAO = new ReactiveMemberDAO(memberDAO);
        reactiveRequestDAO = new ReactiveRequestDAO(requestDAO);
        statsService = new ReactiveStatsService(reactiveMemberDAO, reactiveRequestDAO);
        membersList = FXCollections.observableArrayList();
        requestsList = FXCollections.observableArrayList();

        // Initialize database
        DatabaseInitializer.initializeTables();

        // Set admin name
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            adminNameLabel.setText(currentUser.getUsername());
        }

        // Setup tables
        setupMembersTable();
        setupRequestsTable();

        // Load initial data
        loadDashboardStats();
        loadMembers();
        loadRequests();

        Platform.runLater(() -> {
            membersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        });

        // Initialize WebSocket client
        initializeWebSocket();

        // Start reactive stats subscription for auto-refresh
        startStatsSubscription();
    }

    /**
     * Initialize WebSocket client for real-time notifications.
     */
    private void initializeWebSocket() {
        wsClient = new WebSocketClientService();

        // Handle new membership requests - auto-refresh requests table
        wsClient.setOnNewMemberRequest(message -> {
            System.out.println("[Admin] New membership request received");
            loadRequests();
            loadDashboardStats();
        });

        // Handle data refresh messages
        wsClient.setOnDataRefresh(message -> {
            String tableType = message.getPayloadString("tableType");
            System.out.println("[Admin] Data refresh received for: " + tableType);
            if ("members".equals(tableType)) {
                loadMembers();
            } else if ("requests".equals(tableType)) {
                loadRequests();
            } else {
                loadMembers();
                loadRequests();
            }
            loadDashboardStats();
        });

        // Handle connection state changes
        wsClient.setOnConnectionStateChanged(connected -> {
            System.out.println("[Admin] WebSocket connection: " + (connected ? "connected" : "disconnected"));
        });

        // Connect with admin role
        User currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            wsClient.connect(currentUser.getId(), "ADMIN");
        }
    }

    /**
     * Start reactive stats subscription for periodic dashboard updates.
     */
    private void startStatsSubscription() {
        statsSubscription = statsService.watchStats(Duration.ofSeconds(30))
            .subscribe(stats -> Platform.runLater(() -> {
                totalMembersLabel.setText(String.valueOf(stats.getTotalMembers()));
                activeMembersLabel.setText(String.valueOf(stats.getActiveMembers()));
                dailyRequestsLabel.setText(String.valueOf(stats.getDailyRequests()));
            }), error -> {
                System.err.println("[Admin] Stats subscription error: " + error.getMessage());
            });
    }

    /**
     * Clean up resources when controller is destroyed.
     */
    public void cleanup() {
        if (statsSubscription != null && !statsSubscription.isDisposed()) {
            statsSubscription.dispose();
        }
        if (wsClient != null) {
            wsClient.shutdown();
        }
    }

    // ================= NAVIGATION =================
    @FXML
    public void showDashboard() {
        switchView(dashboardView);
        updateActiveButton(dashboardBtn);
        loadDashboardStats();
    }

    @FXML
    public void showMembers() {
        switchView(membersView);
        updateActiveButton(membersBtn);
        loadMembers();
    }

    @FXML
    public void showRequests() {
        switchView(requestsView);
        updateActiveButton(requestsBtn);
        loadRequests();
    }

    private void switchView(VBox targetView) {
        dashboardView.setVisible(false);
        membersView.setVisible(false);
        requestsView.setVisible(false);
        targetView.setVisible(true);
    }

    private void updateActiveButton(Button activeBtn) {
        dashboardBtn.getStyleClass().remove("active");
        membersBtn.getStyleClass().remove("active");
        requestsBtn.getStyleClass().remove("active");
        activeBtn.getStyleClass().add("active");
    }

    @FXML
    public void logout(ActionEvent event) {
        cleanup();
        Session.logout();
        
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/com/clubsportif/fxml/Login.fxml")));
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DASHBOARD =================
    private void loadDashboardStats() {
        reactiveMemberDAO.updateMemberStatuses()
            .then(statsService.getCurrentStats())
            .subscribe(stats -> Platform.runLater(() -> {
                totalMembersLabel.setText(String.valueOf(stats.getTotalMembers()));
                activeMembersLabel.setText(String.valueOf(stats.getActiveMembers()));
                dailyRequestsLabel.setText(String.valueOf(stats.getDailyRequests()));
            }), error -> {
                System.err.println("[Admin] Failed to load stats: " + error.getMessage());
            });
    }

    // ================= MEMBERS MANAGEMENT =================
    private void setupMembersTable() {
        memberIdCol.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%03d", data.getValue().getId())));
        memberNomCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNom()));
        memberPrenomCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getPrenom()));
        memberSubscriptionCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getSubscription()));
        memberDateEndCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDateEnd().format(dateFormatter)));
        memberStatusCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus()));

        // Style status column
        memberStatusCol.setCellFactory(column -> new TableCell<Member, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("ACTIVE".equals(status)) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: 600;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600;");
                    }
                }
            }
        });

        // Add action buttons
        memberActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button optionsBtn = new Button("Options");

            {
                optionsBtn.getStyleClass().add("table-options-button");
                optionsBtn.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    showMemberOptionsDialog(member);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : optionsBtn);
            }
        });

        membersTable.setItems(membersList);
    }

    private void loadMembers() {
        reactiveMemberDAO.updateMemberStatuses()
            .thenMany(reactiveMemberDAO.getAllMembers())
            .collectList()
            .subscribe(members -> Platform.runLater(() -> {
                membersList.clear();
                membersList.addAll(members);
            }), error -> {
                System.err.println("[Admin] Failed to load members: " + error.getMessage());
            });
    }

    @FXML
    public void refreshMembers() {
        loadMembers();
    }

    @FXML
    public void addMember() {
        showAddMemberDialog();
    }

    @FXML
    public void exportMembersToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Members to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("members_export_" + LocalDate.now().toString() + ".csv");
        
        File file = fileChooser.showSaveDialog(membersTable.getScene().getWindow());
        
        if (file != null) {
            reactiveMemberDAO.getAllMembers()
                .collectList()
                .subscribe(members -> {
                    try (FileWriter writer = new FileWriter(file)) {
                        // Write header
                        writer.write("ID,Last Name,First Name,Subscription,Start Date,End Date,Status\n");
                        
                        // Write data
                        for (Member member : members) {
                            writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                                member.getId(),
                                escapeCsv(member.getNom()),
                                escapeCsv(member.getPrenom()),
                                escapeCsv(member.getSubscription()),
                                member.getDateStart(),
                                member.getDateEnd(),
                                member.getStatus()
                            ));
                        }
                        
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Export Successful");
                            alert.setHeaderText(null);
                            alert.setContentText("Members exported successfully to " + file.getName());
                            alert.showAndWait();
                        });
                        
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Export Error");
                            alert.setHeaderText("Failed to export members");
                            alert.setContentText(e.getMessage());
                            alert.showAndWait();
                        });
                        e.printStackTrace();
                    }
                }, error -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Export Error");
                        alert.setHeaderText("Failed to fetch members");
                        alert.setContentText(error.getMessage());
                        alert.showAndWait();
                    });
                });
        }
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\"", "\"\"");
        if (escapedData.contains(",") || escapedData.contains("\n") || escapedData.contains("\"")) {
            data = "\"" + escapedData + "\"";
        }
        return data;
    }

    private void showMemberOptionsDialog(Member member) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Member Options");
        dialog.setHeaderText("Manage: " + member.getNom() + " " + member.getPrenom());

        // Create form
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        Label nameLabel = new Label("Name: " + member.getNom() + " " + member.getPrenom());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");

        ComboBox<String> subscriptionCombo = new ComboBox<>();
        subscriptionCombo.getItems().addAll("1 month", "3 months", "1 year");
        subscriptionCombo.setValue(member.getSubscription());
        subscriptionCombo.setPromptText("Select subscription");

        content.getChildren().addAll(
            nameLabel,
            new Label("Subscription Plan:"),
            subscriptionCombo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Renew", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Cancel Subscription", ButtonBar.ButtonData.OTHER),
            ButtonType.CANCEL
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                // Renew subscription
                String newPlan = subscriptionCombo.getValue();
                LocalDate newEndDate = calculateEndDate(newPlan);
                member.setSubscription(newPlan);
                member.setDateEnd(newEndDate);
                
                reactiveMemberDAO.updateMember(member)
                    .doOnSuccess(v -> Platform.runLater(() -> {
                        loadMembers();
                        loadDashboardStats();
                        // Notify status change
                        ClubServerEndpoint.notifyDataRefresh("members");
                    })).subscribe();
            } else if (response.getButtonData() == ButtonBar.ButtonData.OTHER) {
                // Cancel subscription
                member.setStatus("EXPIRED");
                
                reactiveMemberDAO.updateMember(member)
                    .doOnSuccess(v -> Platform.runLater(() -> {
                        loadMembers();
                        loadDashboardStats();
                        ClubServerEndpoint.notifyDataRefresh("members");
                    })).subscribe();
            }
        });
    }

    private void showAddMemberDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Member");
        dialog.setHeaderText("Enter member details");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        TextField nomField = new TextField();
        nomField.setPromptText("Last name");
        
        TextField prenomField = new TextField();
        prenomField.setPromptText("First name");

        ComboBox<String> subscriptionCombo = new ComboBox<>();
        subscriptionCombo.getItems().addAll("1 month", "3 months", "1 year");
        subscriptionCombo.setPromptText("Select subscription");

        content.getChildren().addAll(
            new Label("Nom:"), nomField,
            new Label("Prenom:"), prenomField,
            new Label("Subscription:"), subscriptionCombo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && !nomField.getText().isEmpty() 
                && !prenomField.getText().isEmpty() && subscriptionCombo.getValue() != null) {
                
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = calculateEndDate(subscriptionCombo.getValue());
                Member newMember = new Member(
                    nomField.getText(),
                    prenomField.getText(),
                    subscriptionCombo.getValue(),
                    startDate,
                    endDate
                );
                
                reactiveMemberDAO.createMember(newMember)
                    .doOnSuccess(v -> Platform.runLater(() -> {
                        loadMembers();
                        loadDashboardStats();
                        ClubServerEndpoint.notifyDataRefresh("members");
                    })).subscribe();
            }
        });
    }

    private LocalDate calculateEndDate(String subscription) {
        LocalDate now = LocalDate.now();
        switch (subscription) {
            case "1 month":
                return now.plusMonths(1);
            case "3 months":
                return now.plusMonths(3);
            case "1 year":
                return now.plusYears(1);
            default:
                return now.plusMonths(1);
        }
    }

    // ================= REQUESTS MANAGEMENT =================
    private void setupRequestsTable() {
        requestIdCol.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%02d", data.getValue().getId())));
        requestNomCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNom()));
        requestPrenomCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getPrenom()));
        requestSubscriptionCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getSubscription()));
        requestDateCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getRequestDate().format(dateFormatter)));
        requestStatusCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus()));

        // Style status column
        requestStatusCol.setCellFactory(column -> new TableCell<Request, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PENDING":
                            setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 600;");
                            break;
                        case "ACCEPTED":
                            setStyle("-fx-text-fill: #22c55e; -fx-font-weight: 600;");
                            break;
                        case "DECLINED":
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600;");
                            break;
                        case "CANCELED":
                            setStyle("-fx-text-fill: #6b7280; -fx-font-weight: 600;");
                            break;
                    }
                }
            }
        });

        // Add action buttons
        requestActionsCol.setCellFactory(param -> new TableCell<>() {
            private final HBox actionBox = new HBox(8);
            private final Button acceptBtn = new Button("Accept");
            private final Button declineBtn = new Button("Decline");

            {
                acceptBtn.getStyleClass().add("table-action-primary");
                declineBtn.getStyleClass().add("table-action-secondary");

                acceptBtn.setOnAction(event -> {
                    Request request = getTableView().getItems().get(getIndex());
                    handleAcceptRequest(request);
                });

                declineBtn.setOnAction(event -> {
                    Request request = getTableView().getItems().get(getIndex());
                    handleDeclineRequest(request);
                });

                actionBox.getChildren().addAll(acceptBtn, declineBtn);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Request request = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(request.getStatus())) {
                        setGraphic(actionBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        requestsTable.setItems(requestsList);
    }

    private void loadRequests() {
        reactiveRequestDAO.getAllRequests()
            .collectList()
            .subscribe(requests -> Platform.runLater(() -> {
                requestsList.clear();
                requestsList.addAll(requests);
            }), error -> {
                System.err.println("[Admin] Failed to load requests: " + error.getMessage());
            });
    }

    @FXML
    public void refreshRequests() {
        loadRequests();
        loadDashboardStats();
    }

    private void handleAcceptRequest(Request request) {
        reactiveRequestDAO.updateRequestStatus(request.getId(), "ACCEPTED")
            .then(reactiveMemberDAO.getMemberByUserId(request.getUserId()))
            .defaultIfEmpty(new Member(0, 0, "", "", "", LocalDate.now(), LocalDate.now(), ""))
            .doOnSuccess(existingMemberOrEmpty -> Platform.runLater(() -> {
                LocalDate endDate;
                
                // Check if this is our empty placeholder (id=0) or actual member
                if (existingMemberOrEmpty.getId() == 0) {
                    // NEW MEMBER: Create member from request with start from today
                    LocalDate startDate = LocalDate.now();
                    endDate = calculateEndDate(request.getSubscription());
                    Member newMember = new Member(
                        request.getUserId(),
                        request.getNom(),
                        request.getPrenom(),
                        request.getSubscription(),
                        startDate,
                        endDate
                    );
                    memberDAO.createMember(newMember);
                    System.out.println("[Admin] Created new member with end date: " + endDate);
                } else {
                    // EXISTING MEMBER (RENEWAL): Add months to current end date
                    // If current subscription hasn't expired, add to existing end date
                    // If expired, start from today
                    LocalDate baseDate = existingMemberOrEmpty.getDateEnd();
                    if (baseDate.isBefore(LocalDate.now())) {
                        baseDate = LocalDate.now(); // Start fresh if expired
                    }
                    
                    endDate = addSubscriptionToDate(baseDate, request.getSubscription());
                    existingMemberOrEmpty.setSubscription(request.getSubscription());
                    existingMemberOrEmpty.setDateEnd(endDate);
                    existingMemberOrEmpty.setStatus("ACTIVE");
                    memberDAO.updateMember(existingMemberOrEmpty);
                    System.out.println("[Admin] Renewed member subscription. New end date: " + endDate);
                }

                // Update user role
                UserDAO userDAO = new UserDAO();
                try {
                    userDAO.updateUserRole(request.getUserId(), "MEMBER");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                loadRequests();
                loadMembers();
                loadDashboardStats();

                // Send WebSocket notification to member
                ClubServerEndpoint.notifyRequestAccepted(
                    request.getUserId(),
                    request.getId(),
                    request.getSubscription(),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                );
                ClubServerEndpoint.notifyDataRefresh("all");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Request Accepted");
                alert.setHeaderText(null);
                alert.setContentText("Request accepted and member subscription updated!");
                alert.showAndWait();
            }))
            .doOnError(error -> System.err.println("[Admin] Failed to accept request: " + error.getMessage()))
            .subscribe();
    }

    /**
     * Add subscription duration to a base date.
     */
    private LocalDate addSubscriptionToDate(LocalDate baseDate, String subscription) {
        return switch (subscription) {
            case "1 month" -> baseDate.plusMonths(1);
            case "3 months" -> baseDate.plusMonths(3);
            case "1 year" -> baseDate.plusYears(1);
            default -> baseDate.plusMonths(1);
        };
    }


    private void handleDeclineRequest(Request request) {
        reactiveRequestDAO.updateRequestStatus(request.getId(), "DECLINED")
            .doOnSuccess(v -> Platform.runLater(() -> {
                loadRequests();
                loadDashboardStats();

                // Send WebSocket notification to member
                ClubServerEndpoint.notifyRequestDeclined(
                    request.getUserId(),
                    request.getId(),
                    null
                );
                ClubServerEndpoint.notifyDataRefresh("requests");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Request Declined");
                alert.setHeaderText(null);
                alert.setContentText("Request has been declined.");
                alert.showAndWait();
            }))
            .doOnError(error -> System.err.println("[Admin] Failed to decline request: " + error.getMessage()))
            .subscribe();
    }
}
