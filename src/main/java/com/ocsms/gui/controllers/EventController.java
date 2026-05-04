package com.ocsms.gui.controllers;

import com.ocsms.enums.EventState;
import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.service.EventService;
import com.ocsms.service.EventStateManager;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * EventController — UC9 (Create Events), UC10 (Register for Events).
 * Also supports state machine transitions for Society Admins.
 */
public class EventController implements Initializable {

    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, String> colEventTitle;
    @FXML private TableColumn<Event, String> colEventSociety;
    @FXML private TableColumn<Event, String> colEventDate;
    @FXML private TableColumn<Event, String> colEventVenue;
    @FXML private TableColumn<Event, String> colEventCapacity;
    @FXML private TableColumn<Event, String> colEventState;

    // Detail panel
    @FXML private VBox  eventDetailPane;
    @FXML private Label detailEventTitle;
    @FXML private Label detailEventSociety;
    @FXML private Label detailEventState;
    @FXML private Label detailEventDate;
    @FXML private Label detailEventVenue;
    @FXML private Label detailEventCapacity;
    @FXML private Label detailEventDeadline;
    @FXML private TextArea detailEventDesc;
    @FXML private Button registerEventBtn;
    @FXML private Button adminStateBtn;
    @FXML private Button cancelEventBtn;
    @FXML private Label  eventActionStatus;

    // Create form
    @FXML private Button createEventBtn;
    @FXML private VBox   createEventForm;
    @FXML private TextField  newEventTitle;
    @FXML private TextField  newEventVenue;
    @FXML private DatePicker eventDatePicker;
    @FXML private DatePicker regDeadlinePicker;
    @FXML private Spinner<Integer> capacitySpinner;
    @FXML private ComboBox<String> eventTypeCombo;
    @FXML private TextArea   newEventDesc;
    @FXML private Label      createEventError;

    private final EventService      eventService      = new EventService();
    private final EventStateManager stateManager      = new EventStateManager();
    private Event selectedEvent = null;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadEvents();
        setupRoleControls();

        eventTypeCombo.setItems(FXCollections.observableArrayList(
            "Competition", "Seminar", "Workshop", "Cultural", "Sports", "Social", "Other"));

        eventTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> { if (newVal != null) showEventDetail(newVal); });
    }

    private void setupColumns() {
        colEventTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colEventSociety.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getOrganizer() != null ? d.getValue().getOrganizer().getName() : "—"));
        colEventDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDateTime() != null ? d.getValue().getDateTime().format(FMT) : "—"));
        colEventVenue.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVenue()));
        colEventCapacity.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getRegisteredCount() + "/" + d.getValue().getCapacity()));
        colEventState.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getState().name().replace("_", " ")));
    }

    private void loadEvents() {
        eventTable.setItems(FXCollections.observableArrayList(eventService.getAllEvents()));
    }

    private void setupRoleControls() {
        User user = SessionManager.getInstance().getCurrentUser();
        UserRole role = user.getRole();

        boolean isAdmin = role == UserRole.SOCIETY_ADMIN;
        createEventBtn.setVisible(isAdmin);
        createEventBtn.setManaged(isAdmin);
    }

    private void showEventDetail(Event e) {
        selectedEvent = e;
        detailEventTitle.setText(e.getTitle());
        detailEventSociety.setText(e.getOrganizer() != null ? "by " + e.getOrganizer().getName() : "");
        detailEventState.setText(e.getState().name().replace("_", " "));
        detailEventDate.setText(e.getDateTime() != null ? e.getDateTime().format(FMT) : "—");
        detailEventVenue.setText(e.getVenue());
        detailEventCapacity.setText(e.getRegisteredCount() + " / " + e.getCapacity() +
            " (" + e.getRemainingCapacity() + " remaining)");
        detailEventDeadline.setText(e.getRegistrationDeadline() != null ?
            e.getRegistrationDeadline().format(FMT) : "—");
        detailEventDesc.setText(e.getDescription() != null ? e.getDescription() : "");

        applyStateChipStyle(e.getState());
        eventActionStatus.setText("");

        User user = SessionManager.getInstance().getCurrentUser();
        UserRole role = user.getRole();

        // Show register button for students when registration is open
        boolean canRegister = role == UserRole.STUDENT && e.isRegistrationOpen();
        registerEventBtn.setVisible(true);
        registerEventBtn.setManaged(true);
        registerEventBtn.setDisable(!canRegister);
        if (!canRegister) registerEventBtn.setTooltip(new Tooltip("Registration is not open for this event."));

        // Admin controls
        boolean isAdmin = role == UserRole.SOCIETY_ADMIN;
        adminStateBtn.setVisible(isAdmin);
        adminStateBtn.setManaged(isAdmin);
        cancelEventBtn.setVisible(isAdmin);
        cancelEventBtn.setManaged(isAdmin);

        if (isAdmin) {
            Set<EventState> allowed = stateManager.getAllowedTransitions(e.getState());
            adminStateBtn.setDisable(allowed.isEmpty() ||
                (allowed.size() == 1 && allowed.contains(EventState.CANCELLED)));
        }

        eventDetailPane.setVisible(true);
        eventDetailPane.setManaged(true);
    }

    private void applyStateChipStyle(EventState state) {
        String baseStyle = "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12; -fx-font-weight: bold; ";
        switch (state) {
            case REGISTRATION_OPEN  -> detailEventState.setStyle(baseStyle + "-fx-background-color: #14532d; -fx-text-fill: #86efac;");
            case DRAFT, VALIDATING  -> detailEventState.setStyle(baseStyle + "-fx-background-color: #1e3a5f; -fx-text-fill: #93c5fd;");
            case COMPLETED          -> detailEventState.setStyle(baseStyle + "-fx-background-color: #3b0764; -fx-text-fill: #d8b4fe;");
            case CANCELLED          -> detailEventState.setStyle(baseStyle + "-fx-background-color: #450a0a; -fx-text-fill: #fca5a5;");
            default                 -> detailEventState.setStyle(baseStyle + "-fx-background-color: #1c1f26; -fx-text-fill: #94a3b8;");
        }
    }

    @FXML private void handleRegisterForEvent() {
        if (selectedEvent == null) return;
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Student student)) {
            AlertUtil.showError("Not Allowed", "Only students can register for events.");
            return;
        }

        EventService.RegistrationResult result = eventService.registerStudentForEvent(student, selectedEvent);
        if (result.isSuccess()) {
            eventActionStatus.setStyle("-fx-text-fill: #86efac;");
            eventActionStatus.setText("✔ Registered successfully!");
            registerEventBtn.setDisable(true);
            loadEvents();
        } else if (result.isWaitlisted()) {
            eventActionStatus.setStyle("-fx-text-fill: #fbbf24;");
            eventActionStatus.setText("⏳ Added to waitlist.");
            AlertUtil.showWarning("Capacity Full", result.getMessage());
        } else {
            eventActionStatus.setStyle("-fx-text-fill: #ef4444;");
            eventActionStatus.setText("✘ " + result.getMessage());
            AlertUtil.showError("Registration Failed", result.getMessage());
        }
    }

    @FXML private void handleAdvanceState() {
        if (selectedEvent == null) return;
        Set<EventState> allowed = stateManager.getAllowedTransitions(selectedEvent.getState());
        EventState[] validNext = allowed.stream()
            .filter(s -> s != EventState.CANCELLED).toArray(EventState[]::new);

        if (validNext.length == 0) {
            AlertUtil.showInfo("No Transitions", "This event has no further state transitions.");
            return;
        }
        EventState next = validNext[0];
        boolean confirmed = AlertUtil.confirm("Advance State",
            "Transition event to: " + next.name().replace("_", " ") + "?");
        if (confirmed) {
            try {
                stateManager.transition(selectedEvent, next);
                AlertUtil.showInfo("State Updated", "Event is now: " + next.name().replace("_", " "));
                loadEvents();
                showEventDetail(selectedEvent);
            } catch (IllegalStateException ex) {
                AlertUtil.showError("Transition Error", ex.getMessage());
            }
        }
    }

    @FXML private void handleCancelEvent() {
        if (selectedEvent == null) return;
        boolean confirmed = AlertUtil.confirm("Cancel Event",
            "Cancel '" + selectedEvent.getTitle() + "'? All registrants will be notified.");
        if (confirmed) {
            try {
                stateManager.transition(selectedEvent, EventState.CANCELLED);
                AlertUtil.showInfo("Event Cancelled", "Event has been cancelled.");
                loadEvents();
                eventDetailPane.setVisible(false);
                eventDetailPane.setManaged(false);
            } catch (IllegalStateException ex) {
                AlertUtil.showError("Cannot Cancel", ex.getMessage());
            }
        }
    }

    @FXML private void handleCreateEvent() {
        createEventForm.setVisible(true);
        createEventForm.setManaged(true);
    }

    @FXML private void handleSubmitCreateEvent() {
        createEventError.setText("");
        String title = newEventTitle.getText().trim();
        String venue = newEventVenue.getText().trim();
        LocalDate eDate = eventDatePicker.getValue();
        LocalDate rDate = regDeadlinePicker.getValue();
        int capacity = capacitySpinner.getValue();
        String type = eventTypeCombo.getValue();
        String desc = newEventDesc.getText().trim();

        if (title.isEmpty()) { createEventError.setText("Title is required."); return; }
        if (venue.isEmpty()) { createEventError.setText("Venue is required."); return; }
        if (eDate == null)   { createEventError.setText("Event date is required."); return; }
        if (rDate == null)   { createEventError.setText("Registration deadline is required."); return; }
        if (rDate.isAfter(eDate)) { createEventError.setText("Deadline must be before event date."); return; }
        if (type == null)    { createEventError.setText("Please select event type."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        Society society = null;
        if (user instanceof SocietyAdmin admin) {
            society = admin.getManagedSociety();
            if (society == null) {
                // Assign first society from repo for demo
                society = new com.ocsms.repository.SocietyRepository().findAll().get(0);
                admin.setManagedSociety(society);
            }
        }

        try {
            Event created = eventService.createEvent(title, venue,
                eDate.atTime(17, 0), rDate.atTime(23, 59), capacity, desc, type, society);
            AlertUtil.showInfo("Event Created", "Event '" + title + "' created successfully!");
            handleCancelCreateEvent();
            loadEvents();
        } catch (Exception ex) {
            createEventError.setText("Error: " + ex.getMessage());
        }
    }

    @FXML private void handleCancelCreateEvent() {
        createEventForm.setVisible(false);
        createEventForm.setManaged(false);
        newEventTitle.clear(); newEventVenue.clear(); newEventDesc.clear();
        eventDatePicker.setValue(null); regDeadlinePicker.setValue(null);
    }
}
