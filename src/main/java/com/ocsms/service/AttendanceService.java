package com.ocsms.service;

import com.ocsms.model.*;
import com.ocsms.repository.EventRepository;

import java.util.List;

/**
 * AttendanceService — marks attendance for registered students at an event.
 * From Sequence Diagram: opt block — if present, certificate is auto-generated.
 */
public class AttendanceService {

    private final EventRepository    eventRepo;
    private final CertificateService certificateService;

    public AttendanceService() {
        this.eventRepo          = new EventRepository();
        this.certificateService = new CertificateService();
    }

    /**
     * Marks a student's attendance for an event.
     * If present=true, triggers certificate generation (opt block in sequence diagram).
     */
    public void markAttendance(Event event, Student student, boolean isPresent) {
        EventRegistration reg = eventRepo.findRegistration(student.getUserId(), event.getEventId())
            .orElseThrow(() -> new IllegalStateException(
                student.getName() + " is not registered for " + event.getTitle()));

        reg.setAttended(isPresent);
        eventRepo.updateRegistration(reg);

        // opt block: generate certificate only if present
        if (isPresent) {
            certificateService.generate(student, event);
        }
    }

    /**
     * Save attendance for all registrations at once (bulk save from UI checkboxes).
     */
    public void saveAttendanceBulk(Event event, List<EventRegistration> regs) {
        for (EventRegistration reg : regs) {
            eventRepo.updateRegistration(reg);
            if (reg.isAttended()) {
                certificateService.generate(reg.getStudent(), event);
            }
        }
    }

    public List<EventRegistration> getRegistrationsForEvent(Event event) {
        return eventRepo.findRegistrationsByEvent(event.getEventId());
    }

    /** Marks a registration as present (without certificate — caller handles that). */
    public void markPresent(EventRegistration reg) {
        reg.setAttended(true);
        eventRepo.updateRegistration(reg);
    }

    /** Marks a registration as absent. */
    public void markAbsent(EventRegistration reg) {
        reg.setAttended(false);
        eventRepo.updateRegistration(reg);
    }
}
