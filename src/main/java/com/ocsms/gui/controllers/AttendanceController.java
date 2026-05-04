package com.ocsms.gui.controllers;

import com.ocsms.model.*;
import com.ocsms.repository.EventRepository;
import com.ocsms.service.AttendanceService;
import com.ocsms.service.CertificateService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.PDFGenerator;
import com.ocsms.util.SessionManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.*;

/**
 * AttendanceController — UC20 (Mark Attendance) + UC17 (Generate Certificates).
 * Admin/Advisor: see attendance table, mark checkboxes, save, download PDF.
 * Student: see their own registrations and certificate status.
 */
public class AttendanceController implements Initializable {

    // ── Shared ──
    @FXML private Label pageSubtitleLabel;
    @FXML private VBox  instructionsCard;
    @FXML private ComboBox<Event> eventSelector;
    @FXML private Label eventStateLabel;
    @FXML private HBox  eventDetailsBar;
    @FXML private Label eventDateLabel;
    @FXML private Label eventCapLabel;
    @FXML private Label eventRegLabel;

    // ── Admin/Advisor ──
    @FXML private VBox  attendanceTablePane;
    @FXML private Label presentCount;
    @FXML private Label absentCount;
    @FXML private Label attendanceSummary;
    @FXML private TableView<AttendanceRow>              attendanceTable;
    @FXML private TableColumn<AttendanceRow, Boolean>   colAttPresent;
    @FXML private TableColumn<AttendanceRow, String>    colAttName;
    @FXML private TableColumn<AttendanceRow, String>    colAttRoll;
    @FXML private TableColumn<AttendanceRow, String>    colAttStatus;
    @FXML private TableColumn<AttendanceRow, String>    colAttCert;
    @FXML private Button markAllPresentBtn;
    @FXML private Button clearAllBtn;
    @FXML private Button saveAttendanceBtn;
    @FXML private Label  certStatusLabel;
    @FXML private Button downloadPdfBtn;

    // ── Student ──
    @FXML private VBox  myRegistrationsPane;
    @FXML private TableView<EventRegistration>           myRegsTable;
    @FXML private TableColumn<EventRegistration, String> colMyEvent;
    @FXML private TableColumn<EventRegistration, String> colMyDate;
    @FXML private TableColumn<EventRegistration, String> colMyStatus;
    @FXML private TableColumn<EventRegistration, String> colMyAttended;
    @FXML private TableColumn<EventRegistration, String> colMyCert;

    private final AttendanceService  attendanceService  = new AttendanceService();
    private final CertificateService certificateService = new CertificateService();
    private final EventRepository    eventRepo          = new EventRepository();
    private User currentUser;

    // ── Inner row model ────────────────────────────────────────────────────────
    public static class AttendanceRow {
        private final EventRegistration registration;
        private final SimpleBooleanProperty present;

        public AttendanceRow(EventRegistration reg, boolean initialPresent) {
            this.registration = reg;
            this.present = new SimpleBooleanProperty(initialPresent);
        }
        public EventRegistration getRegistration() { return registration; }
        public boolean isPresent() { return present.get(); }
        public void setPresent(boolean v) { present.set(v); }
        public SimpleBooleanProperty presentProperty() { return present; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (isAdminOrAdvisor()) {
            setupAdminMode();
        } else {
            setupStudentMode();
        }
    }

    // ── Admin Mode ─────────────────────────────────────────────────────────────

    private boolean isAdminOrAdvisor() {
        String role = currentUser.getRole().name();
        return role.equals("SOCIETY_ADMIN") || role.equals("FACULTY_ADVISOR") ||
               role.equals("UNIVERSITY_ADMIN") || role.equals("TREASURER");
    }

    private void setupAdminMode() {
        instructionsCard.setVisible(true); instructionsCard.setManaged(true);
        downloadPdfBtn.setVisible(true);   downloadPdfBtn.setManaged(true);
        myRegistrationsPane.setVisible(false); myRegistrationsPane.setManaged(false);

        // Populate event selector
        List<Event> events = eventRepo.findAll();
        eventSelector.setItems(FXCollections.observableArrayList(events));
        eventSelector.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Event e)     { return e == null ? "" : e.getTitle() + "  [" + e.getState().name() + "]"; }
            public Event fromString(String s)   { return null; }
        });

        // Setup admin table columns
        colAttPresent.setCellValueFactory(d -> d.getValue().presentProperty());
        colAttPresent.setCellFactory(CheckBoxTableCell.forTableColumn(colAttPresent));
        colAttPresent.setEditable(true);
        attendanceTable.setEditable(true);

        colAttName.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRegistration().getStudent().getName()));
        colAttRoll.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRegistration().getStudent().getRollNumber()));
        colAttStatus.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRegistration().getStatus().name()));
        colAttCert.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRegistration().isAttended() ? "✔ Issued" : "—"));

        // Live count update
        attendanceTable.getItems().addListener((javafx.collections.ListChangeListener<AttendanceRow>) c -> updateCounts());
    }

    @FXML private void handleEventSelected() {
        Event event = eventSelector.getValue();
        if (event == null) return;

        eventStateLabel.setText("⬤  " + event.getState().name());
        eventDetailsBar.setVisible(true);
        eventDetailsBar.setManaged(true);
        eventDateLabel.setText("📅  " + event.getDateTime().toString());
        eventCapLabel.setText("👥  Capacity: " + event.getCapacity());

        List<EventRegistration> regs = eventRepo.getRegistrationsForEvent(event.getEventId());
        eventRegLabel.setText("Registrations: " + regs.size() + " / " + event.getCapacity());

        if (isAdminOrAdvisor()) {
            ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
            for (EventRegistration reg : regs) {
                rows.add(new AttendanceRow(reg, reg.isAttended()));
            }
            attendanceTable.setItems(rows);
            attendanceTable.setEditable(true);
            attendanceTablePane.setVisible(true);
            attendanceTablePane.setManaged(true);
            updateCounts();
        }
    }

    private void updateCounts() {
        long present = attendanceTable.getItems().stream().filter(AttendanceRow::isPresent).count();
        long total   = attendanceTable.getItems().size();
        long absent  = total - present;
        presentCount.setText("Present: " + present);
        absentCount.setText("Absent: " + absent);
        attendanceSummary.setText(present + " / " + total + " marked present");
    }

    @FXML private void handleMarkAllPresent() {
        attendanceTable.getItems().forEach(r -> r.setPresent(true));
        attendanceTable.refresh();
        updateCounts();
    }

    @FXML private void handleClearAll() {
        attendanceTable.getItems().forEach(r -> r.setPresent(false));
        attendanceTable.refresh();
        updateCounts();
    }

    @FXML private void handleSaveAttendance() {
        Event event = eventSelector.getValue();
        if (event == null) { AlertUtil.showError("No Event", "Please select an event first."); return; }

        List<AttendanceRow> rows = attendanceTable.getItems();
        if (rows.isEmpty()) { AlertUtil.showError("No Students", "No students to mark attendance for."); return; }

        int presentCount = 0;
        List<Certificate> newCerts = new ArrayList<>();
        for (AttendanceRow row : rows) {
            EventRegistration reg = row.getRegistration();
            if (row.isPresent()) {
                attendanceService.markPresent(reg);
                // Generate certificate if not already done
                if (!certificateService.hasCertificate(reg.getStudent().getUserId(), event.getEventId())) {
                    Certificate cert = certificateService.issueCertificate(reg.getStudent(), event);
                    if (cert != null) newCerts.add(cert);
                }
                presentCount++;
            } else {
                attendanceService.markAbsent(reg);
            }
        }

        // Refresh table
        handleEventSelected();

        String msg = String.format("Attendance saved!\n\n✔  %d student(s) marked present\n✗  %d marked absent\n🎓  %d new certificate(s) generated.",
            presentCount, rows.size() - presentCount, newCerts.size());
        certStatusLabel.setText("✔  Saved — " + presentCount + " present, " + newCerts.size() + " certificates issued.");
        AlertUtil.showInfo("Attendance Saved", msg);
    }

    @FXML private void handleDownloadAttendancePdf() {
        Event event = eventSelector.getValue();
        if (event == null) { AlertUtil.showError("No Event", "Please select an event first."); return; }
        try {
            List<AttendanceRow> rows = attendanceTable.getItems();
            String path = PDFGenerator.generateAttendanceReport(event, rows); // event.getDateTime() used internally
            AlertUtil.showInfo("PDF Downloaded",
                "Attendance report saved to:\n" + path);
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", "Could not generate PDF:\n" + ex.getMessage());
        }
    }

    // ── Student Mode ───────────────────────────────────────────────────────────

    private void setupStudentMode() {
        instructionsCard.setVisible(false); instructionsCard.setManaged(false);
        attendanceTablePane.setVisible(false); attendanceTablePane.setManaged(false);
        eventSelector.setVisible(false); eventSelector.setManaged(false);
        eventStateLabel.setVisible(false);

        pageSubtitleLabel.setText("View the events you have registered for and your attendance status.");

        myRegistrationsPane.setVisible(true); myRegistrationsPane.setManaged(true);

        colMyEvent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvent().getTitle()));
        colMyDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvent().getDateTime().toString()));
        colMyStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colMyAttended.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isAttended() ? "✔ Yes" : "—"));
        colMyCert.setCellValueFactory(d -> {
            String studentId = currentUser.getUserId();
            String eventId   = d.getValue().getEvent().getEventId();
            boolean hasCert  = certificateService.hasCertificate(studentId, eventId);
            return new SimpleStringProperty(hasCert ? "🎓 Issued" : "—");
        });

        if (currentUser instanceof Student student) {
            List<EventRegistration> regs = eventRepo.getRegistrationsForStudent(student.getUserId());
            myRegsTable.setItems(FXCollections.observableArrayList(regs));
        }
    }
}
