package com.ocsms.model;

import com.ocsms.enums.RegistrationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EventRegistration — entity.
 * Links a Student to an Event with attendance and waitlist tracking.
 */
public class EventRegistration {

    private String             regId;
    private Student            student;
    private Event              event;
    private LocalDateTime      registeredAt;
    private RegistrationStatus status;
    private boolean            onWaitlist;
    private boolean            attended;

    public EventRegistration() {
        this.regId        = UUID.randomUUID().toString();
        this.registeredAt = LocalDateTime.now();
        this.status       = RegistrationStatus.PENDING;
        this.onWaitlist   = false;
        this.attended     = false;
    }

    public EventRegistration(Student student, Event event) {
        this.regId        = UUID.randomUUID().toString();
        this.student      = student;
        this.event        = event;
        this.registeredAt = LocalDateTime.now();
        this.status       = RegistrationStatus.PENDING;
        this.onWaitlist   = false;
        this.attended     = false;
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public void confirm() {
        this.status     = RegistrationStatus.CONFIRMED;
        this.onWaitlist = false;
    }

    public void promoteFromWaitlist() {
        this.onWaitlist = false;
        this.status     = RegistrationStatus.CONFIRMED;
    }

    public boolean isConfirmed() {
        return status == RegistrationStatus.CONFIRMED;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getRegId()                   { return regId; }
    public void   setRegId(String id)          { this.regId = id; }

    public Student getStudent()                { return student; }
    public void    setStudent(Student s)       { this.student = s; }

    public Event getEvent()                    { return event; }
    public void  setEvent(Event e)             { this.event = e; }

    public LocalDateTime getRegisteredAt()             { return registeredAt; }
    public void          setRegisteredAt(LocalDateTime dt) { this.registeredAt = dt; }

    public RegistrationStatus getStatus()                  { return status; }
    public void               setStatus(RegistrationStatus s) { this.status = s; }

    public boolean isOnWaitlist()              { return onWaitlist; }
    public void    setOnWaitlist(boolean b)    { this.onWaitlist = b; }

    public boolean isAttended()                { return attended; }
    public void    setAttended(boolean b)      { this.attended = b; }
}
