package com.clubsportif.controller;

import com.clubsportif.config.DatabaseInitializer;
import com.clubsportif.dao.MemberDAO;
import com.clubsportif.dao.RequestDAO;
import com.clubsportif.dao.UserDAO;
import com.clubsportif.model.Member;
import com.clubsportif.model.Request;
import com.clubsportif.model.User;
import com.clubsportif.service.Session;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private ObservableList<Member> membersList;
    private ObservableList<Request> requestsList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    // ================= INITIALIZATION =================
    @FXML
    public void initialize() {
        memberDAO = new MemberDAO();
        requestDAO = new RequestDAO();
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
        memberDAO.updateMemberStatuses(); // Update statuses first
        
        List<Member> allMembers = memberDAO.getAllMembers();
        totalMembersLabel.setText(String.valueOf(allMembers.size()));
        
        long activeCount = allMembers.stream()
            .filter(m -> "ACTIVE".equals(m.getStatus()))
            .count();
        activeMembersLabel.setText(String.valueOf(activeCount));
        
        int dailyRequests = requestDAO.getDailyRequestsCount();
        dailyRequestsLabel.setText(String.valueOf(dailyRequests));
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
        memberDAO.updateMemberStatuses();
        membersList.clear();
        membersList.addAll(memberDAO.getAllMembers());
    }

    @FXML
    public void refreshMembers() {
        loadMembers();
    }

    @FXML
    public void addMember() {
        showAddMemberDialog();
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
                memberDAO.updateMember(member);
                loadMembers();
                loadDashboardStats();
            } else if (response.getButtonData() == ButtonBar.ButtonData.OTHER) {
                // Cancel subscription
                member.setStatus("EXPIRED");
                memberDAO.updateMember(member);
                loadMembers();
                loadDashboardStats();
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
                
                LocalDate endDate = calculateEndDate(subscriptionCombo.getValue());
                Member newMember = new Member(
                    nomField.getText(),
                    prenomField.getText(),
                    subscriptionCombo.getValue(),
                    endDate
                );
                memberDAO.createMember(newMember);
                loadMembers();
                loadDashboardStats();
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
        requestsList.clear();
        requestsList.addAll(requestDAO.getAllRequests());
    }

    @FXML
    public void refreshRequests() {
        loadRequests();
        loadDashboardStats();
    }

    private void handleAcceptRequest(Request request) {
        requestDAO.updateRequestStatus(request.getId(), "ACCEPTED");
        
        // Check if member already exists for this user
        Member existingMember = memberDAO.getMemberByUserId(request.getUserId());
        
        if (existingMember == null) {
            // Create member from request only if doesn't exist
            LocalDate endDate = calculateEndDate(request.getSubscription());
            Member newMember = new Member(
                request.getNom(),
                request.getPrenom(),
                request.getSubscription(),
                endDate
            );
            memberDAO.createMember(newMember);
        }
        
        // Update user role to MEMBER
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.updateUserRole(request.getUserId(), "MEMBER");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        loadRequests();
        loadMembers();
        loadDashboardStats();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Accepted");
        alert.setHeaderText(null);
        alert.setContentText("Request accepted and member added successfully!");
        alert.showAndWait();
    }

    private void handleDeclineRequest(Request request) {
        requestDAO.updateRequestStatus(request.getId(), "DECLINED");
        loadRequests();
        loadDashboardStats();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Declined");
        alert.setHeaderText(null);
        alert.setContentText("Request has been declined.");
        alert.showAndWait();
    }
}
