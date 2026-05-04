package com.ocsms.gui.controllers;

import com.ocsms.enums.MembershipStatus;
import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.repository.SocietyRepository;
import com.ocsms.service.MembershipService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * MembershipController — UC7 (Approve/Reject Memberships), UC8 (View My Memberships).
 */
public class MembershipController implements Initializable {

    @FXML private Label membershipSubtitle;
    @FXML private TabPane membershipTabPane;

    // Pending tab
    @FXML private TableView<Membership> pendingTable;
    @FXML private TableColumn<Membership, String> colPendName;
    @FXML private TableColumn<Membership, String> colPendRoll;
    @FXML private TableColumn<Membership, String> colPendSociety;
    @FXML private TableColumn<Membership, String> colPendDate;
    @FXML private VBox  pendingActionPane;
    @FXML private Label pendApplicantName;
    @FXML private Label pendApplicantRoll;
    @FXML private Label pendAppliedDate;
    @FXML private TextArea pendMotivation;
    @FXML private VBox  rejectionReasonPane;
    @FXML private TextField rejectionReasonField;
    @FXML private Label rejectErrorLabel;
    @FXML private Button confirmRejectBtn;

    // Approved tab
    @FXML private TableView<Membership> approvedTable;
    @FXML private TableColumn<Membership, String> colApprName;
    @FXML private TableColumn<Membership, String> colApprRoll;
    @FXML private TableColumn<Membership, String> colApprSociety;
    @FXML private TableColumn<Membership, String> colApprDate;

    // Rejected tab
    @FXML private TableView<Membership> rejectedTable;
    @FXML private TableColumn<Membership, String> colRejName;
    @FXML private TableColumn<Membership, String> colRejRoll;
    @FXML private TableColumn<Membership, String> colRejSociety;
    @FXML private TableColumn<Membership, String> colRejReason;

    // Student: My memberships
    @FXML private VBox myMembershipsPane;
    @FXML private TableView<Membership> myMembershipsTable;
    @FXML private TableColumn<Membership, String> colMyMembSociety;
    @FXML private TableColumn<Membership, String> colMyMembStatus;
    @FXML private TableColumn<Membership, String> colMyMembDate;
    @FXML private TableColumn<Membership, String> colMyMembReason;

    private final MembershipService membershipService = new MembershipService();
    private final SocietyRepository societyRepo       = new SocietyRepository();
    private Membership selectedMembership = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getInstance().getCurrentUser();
        setupColumns();
        configureForRole(user);
    }

    private void setupColumns() {
        // Pending
        colPendName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getName()));
        colPendRoll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getRollNumber()));
        colPendSociety.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSociety().getName()));
        colPendDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAppliedDate().toString()));

        // Approved
        colApprName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getName()));
        colApprRoll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getRollNumber()));
        colApprSociety.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSociety().getName()));
        colApprDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAppliedDate().toString()));

        // Rejected
        colRejName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getName()));
        colRejRoll.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudent().getRollNumber()));
        colRejSociety.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSociety().getName()));
        colRejReason.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRejectionReason() != null ? d.getValue().getRejectionReason() : "—"));

        // My memberships
        colMyMembSociety.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSociety().getName()));
        colMyMembStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colMyMembDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAppliedDate().toString()));
        colMyMembReason.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRejectionReason() != null ? d.getValue().getRejectionReason() : "—"));
    }

    private void configureForRole(User user) {
        if (user.getRole() == UserRole.STUDENT) {
            // Hide admin tabs, show My Memberships
            membershipTabPane.setVisible(false);
            membershipTabPane.setManaged(false);
            myMembershipsPane.setVisible(true);
            myMembershipsPane.setManaged(true);
            membershipSubtitle.setText("View the status of your membership applications.");
            loadMyMemberships((Student) user);
        } else {
            // Admin/Advisor/UniAdmin: show tabs with all memberships
            loadAllMembershipsForAdmin();
            pendingTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> { if (newVal != null) showPendingDetail(newVal); });
        }
    }

    private void loadAllMembershipsForAdmin() {
        List<Membership> all = societyRepo.findAllMemberships();

        pendingTable.setItems(FXCollections.observableArrayList(
            all.stream().filter(m -> m.getStatus() == MembershipStatus.PENDING).toList()));
        approvedTable.setItems(FXCollections.observableArrayList(
            all.stream().filter(m -> m.getStatus() == MembershipStatus.APPROVED).toList()));
        rejectedTable.setItems(FXCollections.observableArrayList(
            all.stream().filter(m -> m.getStatus() == MembershipStatus.REJECTED).toList()));
    }

    private void loadMyMemberships(Student student) {
        myMembershipsTable.setItems(FXCollections.observableArrayList(student.getMemberships()));
    }

    private void showPendingDetail(Membership m) {
        selectedMembership = m;
        pendApplicantName.setText(m.getStudent().getName());
        pendApplicantRoll.setText(m.getStudent().getRollNumber());
        pendAppliedDate.setText(m.getAppliedDate().toString());
        pendMotivation.setText(m.getMotivation());
        rejectionReasonPane.setVisible(false);
        rejectionReasonPane.setManaged(false);
        confirmRejectBtn.setVisible(false);
        confirmRejectBtn.setManaged(false);
        rejectErrorLabel.setText("");
        rejectionReasonField.clear();
        pendingActionPane.setVisible(true);
        pendingActionPane.setManaged(true);
    }

    @FXML private void handleApprove() {
        if (selectedMembership == null) return;
        try {
            membershipService.approveMembership(selectedMembership);
            AlertUtil.showInfo("Approved", selectedMembership.getStudent().getName() + " has been approved!");
            pendingActionPane.setVisible(false);
            pendingActionPane.setManaged(false);
            loadAllMembershipsForAdmin();
        } catch (Exception ex) {
            AlertUtil.showError("Error", ex.getMessage());
        }
    }

    @FXML private void handleRejectClick() {
        // Show rejection reason field (from spec: visible only after reject button click)
        rejectionReasonPane.setVisible(true);
        rejectionReasonPane.setManaged(true);
        confirmRejectBtn.setVisible(true);
        confirmRejectBtn.setManaged(true);
    }

    @FXML private void handleConfirmReject() {
        String reason = rejectionReasonField.getText().trim();
        if (reason.isEmpty()) {
            rejectErrorLabel.setText("Rejection reason is required.");
            rejectionReasonField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 1.5px;");
            return;
        }
        try {
            membershipService.rejectMembership(selectedMembership, reason);
            AlertUtil.showInfo("Rejected", "Application has been rejected.");
            pendingActionPane.setVisible(false);
            pendingActionPane.setManaged(false);
            loadAllMembershipsForAdmin();
        } catch (Exception ex) {
            AlertUtil.showError("Error", ex.getMessage());
        }
    }
}
