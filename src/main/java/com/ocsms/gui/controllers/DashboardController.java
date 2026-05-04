package com.ocsms.gui.controllers;

import com.ocsms.Main;
import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.repository.BudgetRepository;
import com.ocsms.repository.SocietyRepository;
import com.ocsms.repository.UserRepository;
import com.ocsms.service.AuthService;
import com.ocsms.service.MembershipService;
import com.ocsms.service.NotificationService;
import com.ocsms.service.SocietyService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * DashboardController — main navigation hub, role-aware.
 * Faculty Advisor removed. Includes Manage Presidents for Uni Admin.
 * Professional Recent Activity feed (no emojis).
 */
public class DashboardController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Navbar ────────────────────────────────────────────────────────────────
    @FXML private Label navUserLabel;
    @FXML private Label navRoleLabel;
    @FXML private Label notificationBadge;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Label  sidebarUserName;
    @FXML private Label  sidebarRoll;
    @FXML private Label  sidebarRole;
    @FXML private Button navDashboard;
    @FXML private Button navSocieties;
    @FXML private Button navEvents;
    @FXML private Button navMembership;
    @FXML private Button navAttendance;
    @FXML private Button navFinance;
    @FXML private Button navCertificates;
    @FXML private Button navManagePresidents;

    // ── Content panes ─────────────────────────────────────────────────────────
    @FXML private VBox dashboardHomePane;
    @FXML private VBox societiesPane;
    @FXML private VBox eventsPane;
    @FXML private VBox membershipPane;
    @FXML private VBox attendancePane;
    @FXML private VBox financePane;
    @FXML private VBox certificatesPane;
    @FXML private VBox managePresidentsPane;

    // ── Dashboard stats ───────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label statSocieties;
    @FXML private Label statEvents;
    @FXML private Label statMembers;
    @FXML private Label statMembersLabel;
    @FXML private Label statCerts;
    @FXML private Label roleDescLabel;
    @FXML private VBox  pendingApprovalsPane;
    @FXML private ListView<String> pendingApprovalsList;
    @FXML private ListView<String> notificationsList;

    // ── Manage Presidents ─────────────────────────────────────────────────────
    @FXML private TextField  presNameField;
    @FXML private TextField  presRollField;
    @FXML private TextField  presEmailField;
    @FXML private TextField  presPasswordField;
    @FXML private Label      presErrorLabel;
    @FXML private Button     addPresidentBtn;
    @FXML private Button     removePresidentBtn;
    @FXML private TableView<SocietyAdmin>       presidentsTable;
    @FXML private TableColumn<SocietyAdmin, String> colPresName;
    @FXML private TableColumn<SocietyAdmin, String> colPresRoll;
    @FXML private TableColumn<SocietyAdmin, String> colPresEmail;
    @FXML private TableColumn<SocietyAdmin, String> colPresSociety;
    @FXML private TableColumn<SocietyAdmin, String> colPresStatus;

    private final SocietyService      societyService    = new SocietyService();
    private final MembershipService   membershipService = new MembershipService();
    private final NotificationService notifService      = new NotificationService();
    private final AuthService         authService       = new AuthService();
    private final UserRepository      userRepo          = new UserRepository();
    private final BudgetRepository    budgetRepo        = BudgetRepository.getInstance();

    private VBox[]   allPanes;
    private Button[] allNavBtns;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        allPanes = new VBox[]{ dashboardHomePane, societiesPane, eventsPane,
                               membershipPane, attendancePane, financePane,
                               certificatesPane, managePresidentsPane };
        allNavBtns = new Button[]{ navDashboard, navSocieties, navEvents,
                                   navMembership, navAttendance, navFinance,
                                   navCertificates, navManagePresidents };

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        setupNavBar(user);
        setupSidebar(user);
        configureNavForRole(user);
        loadDashboardStats(user);
        loadRecentActivity(user);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupNavBar(User user) {
        navUserLabel.setText(user.getName());
        navRoleLabel.setText(user.getRole().name().replace("_", " "));
        long notifCount = notifService.getNotificationsFor(user.getName()).size();
        notificationBadge.setText(notifCount > 0 ? String.valueOf(notifCount) : "");
        notificationBadge.setVisible(notifCount > 0);
    }

    private void setupSidebar(User user) {
        sidebarUserName.setText(user.getName());
        sidebarRoll.setText(user.getRollNumber());
        sidebarRole.setText(user.getRole().name().replace("_", " "));
    }

    private void configureNavForRole(User user) {
        UserRole role = user.getRole();
        switch (role) {
            case STUDENT -> {
                navMembership.setText("My Memberships");
                navAttendance.setVisible(false); navAttendance.setManaged(false);
                navFinance.setVisible(false);    navFinance.setManaged(false);
                navManagePresidents.setVisible(false); navManagePresidents.setManaged(false);
                roleDescLabel.setText("Browse societies, apply for memberships, register for events, and earn certificates.");
            }
            case SOCIETY_ADMIN -> {
                navMembership.setText("Manage Members");
                navAttendance.setText("Attendance & Certificates");
                navCertificates.setVisible(false); navCertificates.setManaged(false);
                navManagePresidents.setVisible(false); navManagePresidents.setManaged(false);
                roleDescLabel.setText("Manage your society, approve memberships, organize events, mark attendance, and track your budget.");
            }
            case TREASURER -> {
                navAttendance.setVisible(false);   navAttendance.setManaged(false);
                navCertificates.setVisible(false); navCertificates.setManaged(false);
                navMembership.setVisible(false);   navMembership.setManaged(false);
                navSocieties.setVisible(false);    navSocieties.setManaged(false);
                navEvents.setVisible(false);       navEvents.setManaged(false);
                navManagePresidents.setVisible(false); navManagePresidents.setManaged(false);
                navFinance.setText("Budget Allocation");
                roleDescLabel.setText("Allocate and track budgets for all societies. Generate comprehensive financial reports.");
            }
            case UNIVERSITY_ADMIN -> {
                navMembership.setText("All Memberships");
                navManagePresidents.setVisible(true); navManagePresidents.setManaged(true);
                roleDescLabel.setText("Full administrative control: manage societies, events, budgets, and society presidents.");
                setupPresidentsTableColumns();
            }
        }
    }

    private void loadDashboardStats(User user) {
        welcomeLabel.setText("Welcome back, " + user.getName().split(" ")[0]);

        // Load societies count async
        Task<Integer> socTask = new Task<>() {
            @Override protected Integer call() { return societyService.getAllSocieties().size(); }
        };
        socTask.setOnSucceeded(ev -> Platform.runLater(() ->
            statSocieties.setText(String.valueOf(socTask.getValue()))));
        new Thread(socTask, "dash-soc").start();

        switch (user.getRole()) {
            case STUDENT -> {
                Student student = (Student) user;
                statMembers.setText(String.valueOf(student.getMemberships().size()));
                statMembersLabel.setText("My Memberships");
                statEvents.setText("—");
                // Load certificate count from DB asynchronously so it stays up-to-date
                Task<Integer> certTask = new Task<>() {
                    @Override protected Integer call() {
                        return student.getCertificates().size();
                    }
                };
                certTask.setOnSucceeded(ev -> Platform.runLater(() ->
                    statCerts.setText(String.valueOf(certTask.getValue()))));
                new Thread(certTask, "dash-certs").start();
            }
            case SOCIETY_ADMIN -> {
                SocietyAdmin admin = (SocietyAdmin) user;
                statMembersLabel.setText("Pending Approvals");
                if (admin.getManagedSociety() != null) {
                    List<Membership> pending = membershipService.getPendingMemberships(admin.getManagedSociety());
                    statMembers.setText(String.valueOf(pending.size()));
                    if (pendingApprovalsPane != null) {
                        pendingApprovalsPane.setVisible(true);
                        pendingApprovalsPane.setManaged(true);
                        pending.forEach(m -> pendingApprovalsList.getItems().add(
                            m.getStudent().getName() + "  —  " + m.getSociety().getName()));
                    }
                }
                statEvents.setText("—");
            }
            case TREASURER -> {
                statMembersLabel.setText("Budget Allocations");
                statMembers.setText(String.valueOf(budgetRepo.findAll().size()));
                statEvents.setText("—");
                statCerts.setText("—");
            }
            case UNIVERSITY_ADMIN -> {
                statMembersLabel.setText("Total Accounts");
                statMembers.setText("—");
                statEvents.setText("—");
                statCerts.setText("—");
            }
        }
    }

    // ── Recent Activity ───────────────────────────────────────────────────────

    private void loadRecentActivity(User user) {
        if (notificationsList == null) return;
        notificationsList.getItems().clear();

        String now = LocalDateTime.now().format(TIME_FMT);
        List<String> activities = buildActivityFeed(user, now);

        if (activities.isEmpty()) {
            notificationsList.getItems().add("  No recent activity to display.");
        } else {
            notificationsList.getItems().addAll(activities);
        }

        // Style each row as an activity card — always dark bg + readable text
        notificationsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                setText(item);
                int idx = getIndex();
                String bg = idx % 2 == 0 ? "#0d1433" : "#0a1028";
                setStyle("-fx-background-color: " + bg + ";"
                       + "-fx-text-fill: #94a3b8;"
                       + "-fx-font-size: 12px;"
                       + "-fx-padding: 10 14;"
                       + "-fx-border-color: transparent transparent #1a2240 transparent;"
                       + "-fx-border-width: 0 0 1 0;");
            }
        });
    }

    private List<String> buildActivityFeed(User user, String now) {
        List<String> items = new ArrayList<>();
        UserRole role = user.getRole();

        items.add(now + "   Session started  —  Logged in as " + role.name().replace("_", " "));

        // Load activity feed directly (safe, non-async, no Supabase blocking here)
        List<String> feedLines = new ArrayList<>();
        try {
            List<Society> societies = new SocietyRepository().findAll();
            feedLines.add(now + "   System  —  " + societies.size() + " registered societ" +
                    (societies.size() == 1 ? "y" : "ies"));

            if (role == UserRole.TREASURER || role == UserRole.UNIVERSITY_ADMIN) {
                int allocCount = budgetRepo.findAll().size();
                feedLines.add(now + "   Finance  —  " + allocCount + " budget allocation" +
                        (allocCount != 1 ? "s" : "") + " on record");
            }
            if (role == UserRole.SOCIETY_ADMIN) {
                SocietyAdmin admin = (SocietyAdmin) user;
                if (admin.getManagedSociety() != null) {
                    int bills = budgetRepo.findBillsBySociety(
                        admin.getManagedSociety().getSocietyId()).size();
                    feedLines.add(now + "   Budget  —  Budget available for " +
                            admin.getManagedSociety().getName());
                    feedLines.add(now + "   Expenses  —  " + bills + " bill" +
                            (bills != 1 ? "s" : "") + " uploaded");
                }
            }
            societies.stream().limit(3).forEach(s ->
                feedLines.add(now + "   Society  —  " + s.getName() +
                        " [" + s.getCategory() + "]  status: " + s.getStatus().name()));
        } catch (Exception ex) {
            feedLines.add(now + "   System  —  Could not load activity data");
        }
        items.addAll(feedLines);
        return items;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML public void showDashboardHome()    { switchTo(dashboardHomePane,   navDashboard); }
    @FXML public void showSocieties()        { switchTo(societiesPane,       navSocieties); }
    @FXML public void showEvents()           { switchTo(eventsPane,          navEvents); }
    @FXML public void showMembership()       { switchTo(membershipPane,      navMembership); }
    @FXML public void showAttendance()       { switchTo(attendancePane,      navAttendance); }
    @FXML public void showFinance()          { switchTo(financePane,         navFinance); }
    @FXML public void showCertificates()     { switchTo(certificatesPane,    navCertificates); }
    @FXML public void showManagePresidents() {
        switchTo(managePresidentsPane, navManagePresidents);
        loadPresidentsTableAsync();
    }

    private void switchTo(VBox target, Button activeBtn) {
        for (VBox pane : allPanes) { pane.setVisible(false); pane.setManaged(false); }
        target.setVisible(true); target.setManaged(true);
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 13px;" +
                          "-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-radius: 8; -fx-cursor: hand;";
        String active   = "-fx-background-color: #1a2040; -fx-text-fill: #818cf8; -fx-font-weight: bold;" +
                          "-fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 10 14;" +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #2d3a6a;" +
                          "-fx-border-width: 1; -fx-border-radius: 8;";
        for (Button btn : allNavBtns) if (btn != null) btn.setStyle(inactive);
        if (activeBtn != null) activeBtn.setStyle(active);
    }

    @FXML private void handleLogout() {
        boolean confirmed = AlertUtil.confirm("Logout", "Are you sure you want to sign out?");
        if (confirmed) {
            SessionManager.getInstance().logout();
            try { Main.showLogin(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // ── Manage Presidents ─────────────────────────────────────────────────────

    private void setupPresidentsTableColumns() {
        if (colPresName == null) return;
        colPresName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colPresRoll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRollNumber()));
        colPresEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colPresSociety.setCellValueFactory(d -> {
            Society soc = d.getValue().getManagedSociety();
            return new SimpleStringProperty(soc != null ? soc.getName() : "Not Assigned");
        });
        colPresStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Disabled"));
    }

    private void loadPresidentsTableAsync() {
        if (presidentsTable == null) return;
        Task<List<SocietyAdmin>> task = new Task<>() {
            @Override protected List<SocietyAdmin> call() {
                List<User> byRole = userRepo.findByRole(UserRole.SOCIETY_ADMIN);
                List<SocietyAdmin> admins = new ArrayList<>();
                SocietyRepository socRepo = new SocietyRepository();
                for (User u : byRole) {
                    if (u instanceof SocietyAdmin sa) {
                        socRepo.findByPresidentId(sa.getUserId()).ifPresent(sa::setManagedSociety);
                        admins.add(sa);
                    }
                }
                return admins;
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() ->
            presidentsTable.setItems(FXCollections.observableArrayList(task.getValue()))));
        new Thread(task, "load-pres").start();
    }

    @FXML private void handleAddPresident() {
        if (presErrorLabel != null) presErrorLabel.setText("");
        String name  = presNameField.getText().trim();
        String roll  = presRollField.getText().trim();
        String email = presEmailField.getText().trim();
        String pass  = presPasswordField.getText();

        if (name.isEmpty())       { presErrorLabel.setText("Name is required.");        return; }
        if (roll.isEmpty())       { presErrorLabel.setText("Roll number is required."); return; }
        if (!email.contains("@")) { presErrorLabel.setText("Valid email required.");    return; }
        if (pass.length() < 6)    { presErrorLabel.setText("Password min 6 chars.");   return; }

        addPresidentBtn.setDisable(true);
        Task<SocietyAdmin> task = new Task<>() {
            @Override protected SocietyAdmin call() throws Exception {
                return authService.createPresidentAccount(roll, name, email, pass, "");
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            addPresidentBtn.setDisable(false);
            AlertUtil.showInfo("President Created",
                "Account created for " + name + ".\nRoll: " + roll + "  |  Password: " + pass);
            presNameField.clear(); presRollField.clear();
            presEmailField.clear(); presPasswordField.clear();
            loadPresidentsTableAsync();
        }));
        task.setOnFailed(ev -> Platform.runLater(() -> {
            addPresidentBtn.setDisable(false);
            String msg = task.getException() != null ? task.getException().getMessage() : "Failed.";
            presErrorLabel.setText(msg);
        }));
        new Thread(task, "add-pres").start();
    }

    @FXML private void handleRemovePresident() {
        SocietyAdmin selected = presidentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.showError("No Selection", "Select a president to remove."); return; }
        boolean confirmed = AlertUtil.confirm("Remove President",
            "Remove " + selected.getName() + " (" + selected.getRollNumber() + ")?\n" +
            "Their society will remain but will have no president assigned.");
        if (!confirmed) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                authService.deletePresidentAccount(selected.getUserId());
                return null;
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            AlertUtil.showInfo("Removed", selected.getName() + " has been removed.");
            loadPresidentsTableAsync();
        }));
        new Thread(task, "rm-pres").start();
    }
}
