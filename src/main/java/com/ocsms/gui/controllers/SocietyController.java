package com.ocsms.gui.controllers;

import com.ocsms.enums.MembershipStatus;
import com.ocsms.enums.SocietyStatus;
import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.repository.SocietyRepository;
import com.ocsms.service.MembershipService;
import com.ocsms.service.SocietyService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.LogoResizeUtil;
import com.ocsms.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SocietyController — role-aware.
 *
 * SOCIETY_ADMIN (President):
 *   - Can create their own society (one per president)
 *   - Upload and auto-resize society logo
 *   - See all approved members of their society
 *
 * UNIVERSITY_ADMIN:
 *   - Full control: delete, archive any society
 *
 * STUDENT:
 *   - Browse, search, apply for membership
 *   - Cannot re-apply if already an active/pending member
 */
public class SocietyController implements Initializable {

    // ── Table & Search ────────────────────────────────────────────────────────
    @FXML private TextField          searchBar;
    @FXML private ComboBox<String>   categoryFilter;
    @FXML private TableView<Society> societiesTable;
    @FXML private TableColumn<Society, String> colSocietyName;
    @FXML private TableColumn<Society, String> colSocietyCategory;
    @FXML private TableColumn<Society, String> colMemberCount;
    @FXML private TableColumn<Society, String> colSocietyStatus;

    // ── Detail Panel ──────────────────────────────────────────────────────────
    @FXML private VBox      societyDetailPane;
    @FXML private Label     detailSocietyName;
    @FXML private Label     detailCategory;
    @FXML private Label     detailAdvisor;      // now shows Status
    @FXML private Label     detailMemberCount;
    @FXML private TextArea  detailDescription;
    @FXML private ListView<String> detailEventList;
    @FXML private ImageView logoImageView;
    @FXML private Label     logoStatusLabel;
    @FXML private Button    uploadLogoBtn;

    // ── Members panel (President only) ────────────────────────────────────────
    @FXML private VBox            membersPane;
    @FXML private ListView<String> membersList;

    // ── Action Buttons ────────────────────────────────────────────────────────
    @FXML private Button applyMembershipBtn;
    @FXML private Button archiveSocietyBtn;
    @FXML private Button deleteSocietyBtn;
    @FXML private Button createSocietyBtn;

    // ── Create Society Form ───────────────────────────────────────────────────
    @FXML private VBox             createSocietyForm;
    @FXML private TextField        newSocietyName;
    @FXML private ComboBox<String> newSocietyCategory;
    @FXML private TextArea         newSocietyDesc;
    @FXML private Spinner<Integer> memberLimitSpinner;
    @FXML private Label            createSocietyError;

    // ── Apply Membership Form ─────────────────────────────────────────────────
    @FXML private VBox     applyMembershipForm;
    @FXML private Label    applyingSocietyLabel;
    @FXML private TextArea motivationArea;
    @FXML private Label    applyErrorLabel;

    private final SocietyService    societyService    = new SocietyService();
    private final MembershipService membershipService = new MembershipService();
    private final SocietyRepository societyRepo       = new SocietyRepository();
    private Society selectedSociety = null;

    // Logo dimensions (pixels)
    private static final int LOGO_TARGET_W = 200;
    private static final int LOGO_TARGET_H = 200;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupCategories();
        loadSocietiesAsync();
        setupRoleControls();

        societiesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> { if (newVal != null) showSocietyDetail(newVal); });
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colSocietyName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colSocietyCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colMemberCount.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getActiveCount() + " / " + d.getValue().getMemberLimit()));
        colSocietyStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
    }

    private void setupCategories() {
        categoryFilter.setItems(FXCollections.observableArrayList(
            "All", "Technology", "Arts & Culture", "Academic", "Sports", "Social"));
        if (newSocietyCategory != null)
            newSocietyCategory.setItems(FXCollections.observableArrayList(
                "Technology", "Arts & Culture", "Academic", "Sports", "Social", "Other"));
    }

    private void loadSocietiesAsync() {
        Task<List<Society>> task = new Task<>() {
            @Override protected List<Society> call() { return societyService.getAllSocieties(); }
        };
        task.setOnSucceeded(ev ->
            Platform.runLater(() -> societiesTable.setItems(
                FXCollections.observableArrayList(task.getValue()))));
        new Thread(task, "soc-load").start();
    }

    private void loadSocieties(List<Society> list) {
        societiesTable.setItems(FXCollections.observableArrayList(list));
    }

    private void setupRoleControls() {
        User user = SessionManager.getInstance().getCurrentUser();
        UserRole role = user.getRole();

        // Create Society button: President or Uni Admin
        boolean canCreate = (role == UserRole.SOCIETY_ADMIN || role == UserRole.UNIVERSITY_ADMIN);
        if (createSocietyBtn != null) {
            createSocietyBtn.setVisible(canCreate);
            createSocietyBtn.setManaged(canCreate);
        }

        // Apply Membership: Students only
        boolean canApply = role == UserRole.STUDENT;
        if (applyMembershipBtn != null) {
            applyMembershipBtn.setVisible(canApply);
            applyMembershipBtn.setManaged(canApply);
        }

        // Upload Logo: Society President only
        boolean canUploadLogo = role == UserRole.SOCIETY_ADMIN;
        if (uploadLogoBtn != null) {
            uploadLogoBtn.setVisible(canUploadLogo);
            uploadLogoBtn.setManaged(canUploadLogo);
        }

        // Members list: Society President only
        if (membersPane != null) {
            boolean showMembers = role == UserRole.SOCIETY_ADMIN;
            membersPane.setVisible(showMembers);
            membersPane.setManaged(showMembers);
        }

        // Archive / Delete: Uni Admin only
        boolean canAdmin = role == UserRole.UNIVERSITY_ADMIN;
        if (archiveSocietyBtn != null) { archiveSocietyBtn.setVisible(canAdmin); archiveSocietyBtn.setManaged(canAdmin); }
        if (deleteSocietyBtn  != null) { deleteSocietyBtn.setVisible(canAdmin);  deleteSocietyBtn.setManaged(canAdmin); }
    }

    private void showSocietyDetail(Society s) {
        selectedSociety = s;
        detailSocietyName.setText(s.getName());
        detailCategory.setText(s.getCategory());
        if (detailAdvisor != null) detailAdvisor.setText(s.getStatus().name());
        detailMemberCount.setText(s.getActiveCount() + " / " + s.getMemberLimit());
        detailDescription.setText(s.getDescription() != null ? s.getDescription() : "");

        detailEventList.getItems().clear();
        s.getUpcomingEvents().forEach(e -> detailEventList.getItems().add(e.getTitle()));
        if (detailEventList.getItems().isEmpty()) detailEventList.getItems().add("No upcoming events.");

        // Show logo if available
        if (logoImageView != null) {
            if (s.getLogoPath() != null && !s.getLogoPath().isBlank()) {
                try {
                    File f = new File(s.getLogoPath());
                    if (f.exists()) {
                        logoImageView.setImage(new Image(f.toURI().toString()));
                        if (logoStatusLabel != null)
                            logoStatusLabel.setText("Logo: " + f.getName());
                    }
                } catch (Exception ignored) {}
            } else {
                logoImageView.setImage(null);
                if (logoStatusLabel != null) logoStatusLabel.setText("No logo uploaded.");
            }
        }

        societyDetailPane.setVisible(true);
        societyDetailPane.setManaged(true);

        User user = SessionManager.getInstance().getCurrentUser();

        // ── Apply button: disable if society inactive OR student already applied ──
        if (applyMembershipBtn != null && user instanceof Student student) {
            boolean societyInactive = s.getStatus() != SocietyStatus.ACTIVE;
            boolean alreadyApplied  = societyRepo.hasActiveMembership(student.getUserId(), s.getSocietyId());
            applyMembershipBtn.setDisable(societyInactive || alreadyApplied);
            applyMembershipBtn.setText(alreadyApplied ? "Already Applied" : "Apply for Membership");
        }

        // ── Members list for President ────────────────────────────────────────
        if (membersPane != null && user instanceof SocietyAdmin admin) {
            Society managed = admin.getManagedSociety();
            // Show members pane only for their own society
            boolean isOwnSociety = managed != null && managed.getSocietyId().equals(s.getSocietyId());
            membersPane.setVisible(isOwnSociety);
            membersPane.setManaged(isOwnSociety);
            if (isOwnSociety) {
                loadMembersAsync(s);
            }
        }
    }

    /** Loads approved members of the given society into membersList asynchronously. */
    private void loadMembersAsync(Society society) {
        if (membersList == null) return;
        membersList.getItems().clear();
        Task<List<Membership>> task = new Task<>() {
            @Override protected List<Membership> call() {
                return membershipService.getApprovedMemberships(society);
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            List<Membership> approved = task.getValue();
            membersList.getItems().clear();
            if (approved.isEmpty()) {
                membersList.getItems().add("No approved members yet.");
            } else {
                approved.forEach(m -> membersList.getItems().add(
                    m.getStudent().getName() + "  —  " + m.getStudent().getRollNumber()));
            }
        }));
        new Thread(task, "members-load").start();
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML private void handleSearch() {
        String q = searchBar.getText().trim();
        loadSocieties(q.isEmpty() ? societyService.getAllSocieties() : societyService.searchSocieties(q));
    }

    @FXML private void handleCategoryFilter() {
        loadSocieties(societyService.filterByCategory(categoryFilter.getValue()));
    }

    // ── Logo Upload ───────────────────────────────────────────────────────────

    @FXML private void handleUploadLogo() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof SocietyAdmin admin)) return;
        if (admin.getManagedSociety() == null) {
            AlertUtil.showError("No Society", "You must create your society before uploading a logo.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Society Logo");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File chosen = fc.showOpenDialog(uploadLogoBtn.getScene().getWindow());
        if (chosen == null) return;

        if (logoStatusLabel != null) logoStatusLabel.setText("Processing logo...");

        Task<File> resizeTask = new Task<>() {
            @Override protected File call() throws Exception {
                return LogoResizeUtil.resizeToFit(chosen, LOGO_TARGET_W, LOGO_TARGET_H);
            }
        };
        resizeTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            File resized = resizeTask.getValue();
            Society soc = admin.getManagedSociety();
            soc.setLogoPath(resized.getAbsolutePath());
            new SocietyRepository().updateLogoPath(soc.getSocietyId(), resized.getAbsolutePath());
            if (logoImageView != null)
                logoImageView.setImage(new Image(resized.toURI().toString()));
            if (logoStatusLabel != null)
                logoStatusLabel.setText("Logo saved (" + LOGO_TARGET_W + "x" + LOGO_TARGET_H + "px)");
            AlertUtil.showInfo("Logo Updated",
                "Society logo resized to " + LOGO_TARGET_W + "x" + LOGO_TARGET_H + "px and saved.");
        }));
        resizeTask.setOnFailed(ev -> Platform.runLater(() -> {
            String msg = resizeTask.getException() != null
                ? resizeTask.getException().getMessage() : "Resize failed.";
            if (logoStatusLabel != null) logoStatusLabel.setText("Error: " + msg);
            AlertUtil.showError("Logo Error", "Could not process the logo image:\n" + msg);
        }));
        new Thread(resizeTask, "logo-resize").start();
    }

    // ── Create Society ────────────────────────────────────────────────────────

    @FXML private void handleCreateSociety() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user.getRole() == UserRole.SOCIETY_ADMIN) {
            if (societyRepo.findByPresidentId(user.getUserId()).isPresent()) {
                AlertUtil.showError("Already Created",
                    "You have already created your society. You can only manage one society.");
                return;
            }
        }
        createSocietyForm.setVisible(true);
        createSocietyForm.setManaged(true);
        if (applyMembershipForm != null) {
            applyMembershipForm.setVisible(false);
            applyMembershipForm.setManaged(false);
        }
    }

    @FXML private void handleSubmitCreateSociety() {
        createSocietyError.setText("");
        String name  = newSocietyName.getText().trim();
        String cat   = newSocietyCategory.getValue();
        String desc  = newSocietyDesc.getText().trim();
        int    limit = memberLimitSpinner.getValue();

        if (name.isEmpty())     { createSocietyError.setText("Society name is required.");             return; }
        if (cat == null)        { createSocietyError.setText("Please select a category.");             return; }
        if (desc.length() < 10) { createSocietyError.setText("Description too short (min 10 chars)."); return; }

        User user = SessionManager.getInstance().getCurrentUser();

        Task<Society> task = new Task<>() {
            @Override protected Society call() throws Exception {
                Society created = societyService.createSociety(name, cat, desc, limit, null);
                if (user.getRole() == UserRole.SOCIETY_ADMIN) {
                    societyRepo.assignPresident(created.getSocietyId(), user.getUserId());
                    ((SocietyAdmin) user).setManagedSociety(created);
                }
                return created;
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            AlertUtil.showInfo("Society Created", "'" + name + "' has been created successfully!");
            handleCancelCreate();
            loadSocietiesAsync();
        }));
        task.setOnFailed(ev -> Platform.runLater(() -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Failed to create society.";
            createSocietyError.setText(msg);
        }));
        new Thread(task, "create-soc").start();
    }

    @FXML private void handleCancelCreate() {
        createSocietyForm.setVisible(false);
        createSocietyForm.setManaged(false);
        newSocietyName.clear();
        newSocietyDesc.clear();
    }

    // ── Apply Membership ──────────────────────────────────────────────────────

    @FXML private void handleApplyMembership() {
        if (selectedSociety == null) return;
        User user = SessionManager.getInstance().getCurrentUser();
        if (user instanceof Student student) {
            // Guard: already a member/pending?
            if (societyRepo.hasActiveMembership(student.getUserId(), selectedSociety.getSocietyId())) {
                AlertUtil.showError("Already Applied",
                    "You already have an active or pending application for " + selectedSociety.getName() + ".");
                return;
            }
        }
        applyMembershipForm.setVisible(true);
        applyMembershipForm.setManaged(true);
        applyingSocietyLabel.setText("Society: " + selectedSociety.getName());
        createSocietyForm.setVisible(false);
        createSocietyForm.setManaged(false);
    }

    @FXML private void handleSubmitApplication() {
        applyErrorLabel.setText("");
        String motivation = motivationArea.getText().trim();
        if (motivation.length() < 20) {
            applyErrorLabel.setText("Motivation must be at least 20 characters.");
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Student student)) {
            applyErrorLabel.setText("Only students can apply for membership.");
            return;
        }
        try {
            membershipService.applyForMembership(student, selectedSociety, motivation);
            AlertUtil.showInfo("Application Submitted",
                "Your application for '" + selectedSociety.getName() + "' has been submitted!");
            handleCancelApply();
            // Refresh button state
            showSocietyDetail(selectedSociety);
        } catch (IllegalStateException ex) {
            applyErrorLabel.setText(ex.getMessage());
        }
    }

    @FXML private void handleCancelApply() {
        applyMembershipForm.setVisible(false);
        applyMembershipForm.setManaged(false);
        motivationArea.clear();
    }

    // ── Archive / Delete (Uni Admin only) ─────────────────────────────────────

    @FXML private void handleArchiveSociety() {
        if (selectedSociety == null) return;
        boolean confirmed = AlertUtil.confirm("Archive Society",
            "Archive '" + selectedSociety.getName() + "'? Members will no longer be able to join.");
        if (!confirmed) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                societyService.archiveSociety(selectedSociety, "Archived by University Admin");
                return null;
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            AlertUtil.showInfo("Archived", selectedSociety.getName() + " has been archived.");
            loadSocietiesAsync();
            societyDetailPane.setVisible(false);
            societyDetailPane.setManaged(false);
        }));
        new Thread(task, "archive-soc").start();
    }

    @FXML private void handleDeleteSociety() {
        if (selectedSociety == null) return;
        boolean confirmed = AlertUtil.confirm("Delete Society",
            "PERMANENTLY delete '" + selectedSociety.getName() + "'?\n" +
            "This will remove all society data and cannot be undone.");
        if (!confirmed) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                new SocietyRepository().delete(selectedSociety.getSocietyId());
                return null;
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            AlertUtil.showInfo("Deleted", selectedSociety.getName() + " has been permanently deleted.");
            loadSocietiesAsync();
            societyDetailPane.setVisible(false);
            societyDetailPane.setManaged(false);
        }));
        new Thread(task, "delete-soc").start();
    }
}
