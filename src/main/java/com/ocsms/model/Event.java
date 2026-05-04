package com.ocsms.model;

import com.ocsms.enums.EventState;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Event — core entity.
 * Represents a student society event with a full state machine lifecycle.
 */
public class Event {

    private String            eventId;
    private String            title;
    private LocalDateTime     dateTime;
    private String            venue;
    private int               capacity;
    private int               registeredCount;
    private LocalDateTime     registrationDeadline;
    private EventState        state;
    private Society           organizer;
    private List<EventRegistration> registrations;
    private Queue<Student>    waitlist;
    private String            description;
    private String            eventType;

    public Event() {
        this.registrations   = new ArrayList<>();
        this.waitlist        = new LinkedList<>();
        this.state           = EventState.DRAFT;
        this.registeredCount = 0;
    }

    public Event(String eventId, String title, LocalDateTime dateTime, String venue,
                 int capacity, LocalDateTime registrationDeadline, Society organizer) {
        this.eventId              = eventId;
        this.title                = title;
        this.dateTime             = dateTime;
        this.venue                = venue;
        this.capacity             = capacity;
        this.registrationDeadline = registrationDeadline;
        this.organizer            = organizer;
        this.registrations        = new ArrayList<>();
        this.waitlist             = new LinkedList<>();
        this.state                = EventState.DRAFT;
        this.registeredCount      = 0;
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public boolean isRegistrationOpen() {
        return state == EventState.REGISTRATION_OPEN &&
               LocalDateTime.now().isBefore(registrationDeadline);
    }

    public boolean hasCapacity() {
        return registeredCount < capacity;
    }

    public void addToWaitlist(Student s) {
        waitlist.offer(s);
    }

    public void transitionState(EventState next) {
        this.state = next;
    }

    public void decrementCapacity() {
        this.registeredCount++;
    }

    public int getRemainingCapacity() {
        return Math.max(0, capacity - registeredCount);
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getEventId()               { return eventId; }
    public void   setEventId(String id)      { this.eventId = id; }

    public String getTitle()                 { return title; }
    public void   setTitle(String title)     { this.title = title; }

    public LocalDateTime getDateTime()                  { return dateTime; }
    public void          setDateTime(LocalDateTime dt)  { this.dateTime = dt; }

    public String getVenue()                 { return venue; }
    public void   setVenue(String venue)     { this.venue = venue; }

    public int    getCapacity()              { return capacity; }
    public void   setCapacity(int cap)       { this.capacity = cap; }

    public int    getRegisteredCount()             { return registeredCount; }
    public void   setRegisteredCount(int count)    { this.registeredCount = count; }

    public LocalDateTime getRegistrationDeadline()                  { return registrationDeadline; }
    public void          setRegistrationDeadline(LocalDateTime dl)  { this.registrationDeadline = dl; }

    public EventState getState()               { return state; }
    public void       setState(EventState s)   { this.state = s; }

    public Society getOrganizer()                { return organizer; }
    public void    setOrganizer(Society org)     { this.organizer = org; }

    public List<EventRegistration> getRegistrations()             { return registrations; }
    public void                    setRegistrations(List<EventRegistration> r) { this.registrations = r; }
    public void                    addRegistration(EventRegistration r) { this.registrations.add(r); }

    public Queue<Student> getWaitlist()             { return waitlist; }
    public void           setWaitlist(Queue<Student> q) { this.waitlist = q; }

    public String getDescription()              { return description; }
    public void   setDescription(String desc)   { this.description = desc; }

    public String getEventType()                { return eventType; }
    public void   setEventType(String type)     { this.eventType = type; }

    @Override
    public String toString() {
        return title + " @ " + venue + " [" + state + "]";
    }
}
