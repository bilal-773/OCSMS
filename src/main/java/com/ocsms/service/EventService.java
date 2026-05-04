package com.ocsms.service;

import com.ocsms.model.*;
import com.ocsms.enums.EventState;
import com.ocsms.enums.RegistrationStatus;
import com.ocsms.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * EventService — core business logic for event management.
 * Implements the Sequence Diagram logic: deadline check, capacity check, waitlist.
 */
public class EventService {

    private final EventRepository     eventRepo;
    private final NotificationService notificationService;

    public EventService() {
        this.eventRepo           = new EventRepository();
        this.notificationService = new NotificationService();
    }

    /**
     * Creates a new event in DRAFT state.
     */
    public Event createEvent(String title, String venue, LocalDateTime dateTime,
                             LocalDateTime regDeadline, int capacity, String description,
                             String eventType, Society organizer) {
        String id = "EVT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Event event = new Event(id, title, dateTime, venue, capacity, regDeadline, organizer);
        event.setDescription(description);
        event.setEventType(eventType);
        event.setState(EventState.REGISTRATION_OPEN); // Simplified for demo: skip approval chain
        eventRepo.save(event);
        organizer.addEvent(event);
        return event;
    }

    /**
     * Registers a Student for an Event.
     * Implements full sequence diagram logic:
     *   1. Deadline check
     *   2. Duplicate check
     *   3. Capacity check / waitlist
     *   4. Save registration
     *   5. Trigger notification
     */
    public RegistrationResult registerStudentForEvent(Student student, Event event) {

        // Step 1: Check registration deadline
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            return RegistrationResult.error("Registration is closed — deadline has passed.");
        }

        // Step 2: Check if event state allows registration
        if (event.getState() != EventState.REGISTRATION_OPEN) {
            return RegistrationResult.error("Event registration is not open at this time.");
        }

        // Step 3: Check if already registered
        if (eventRepo.isAlreadyRegistered(student.getUserId(), event.getEventId())) {
            return RegistrationResult.error("You are already registered for this event.");
        }

        // Step 4: Capacity check
        if (!event.hasCapacity()) {
            return handleWaitlist(student, event);
        }

        // Step 5: Confirm registration
        EventRegistration reg = new EventRegistration(student, event);
        reg.confirm();
        event.decrementCapacity();
        event.addRegistration(reg);
        eventRepo.saveRegistration(reg);

        // Step 6: Notify student
        notificationService.notify(student.getName(), "Event Registration Confirmed",
            "You are registered for: " + event.getTitle());

        return RegistrationResult.success(reg);
    }

    private RegistrationResult handleWaitlist(Student s, Event e) {
        e.addToWaitlist(s);

        EventRegistration reg = new EventRegistration(s, e);
        reg.setOnWaitlist(true);
        reg.setStatus(RegistrationStatus.WAITLISTED);
        e.addRegistration(reg);
        eventRepo.saveRegistration(reg);

        notificationService.notify(s.getName(), "Added to Waitlist",
            "You have been added to the waitlist for: " + e.getTitle());

        return RegistrationResult.waitlisted();
    }

    public List<Event> getAllEvents() {
        return eventRepo.findAll();
    }

    public List<Event> getEventsBySociety(String societyId) {
        return eventRepo.findBySociety(societyId);
    }

    public List<EventRegistration> getRegistrationsForEvent(String eventId) {
        return eventRepo.findRegistrationsByEvent(eventId);
    }

    /**
     * Inner class for registration result — wraps success/failure/waitlist outcome.
     */
    public static class RegistrationResult {
        public enum Type { SUCCESS, ERROR, WAITLISTED }

        private final Type   type;
        private final String message;
        private final EventRegistration registration;

        private RegistrationResult(Type type, String message, EventRegistration reg) {
            this.type         = type;
            this.message      = message;
            this.registration = reg;
        }

        public static RegistrationResult success(EventRegistration reg) {
            return new RegistrationResult(Type.SUCCESS, "Registration confirmed!", reg);
        }

        public static RegistrationResult error(String msg) {
            return new RegistrationResult(Type.ERROR, msg, null);
        }

        public static RegistrationResult waitlisted() {
            return new RegistrationResult(Type.WAITLISTED, "Added to waitlist.", null);
        }

        public boolean isSuccess()    { return type == Type.SUCCESS; }
        public boolean isWaitlisted() { return type == Type.WAITLISTED; }
        public boolean isError()      { return type == Type.ERROR; }
        public String getMessage()    { return message; }
        public Type getType()         { return type; }
        public EventRegistration getRegistration() { return registration; }
    }
}
