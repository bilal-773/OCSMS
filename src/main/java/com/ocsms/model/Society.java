package com.ocsms.model;

import com.ocsms.enums.SocietyStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Society — core entity.
 * Represents a student society at FAST-NUCES Peshawar.
 */
public class Society {

    private String       societyId;
    private String       name;
    private String       category;
    private String       description;
    private int          memberLimit;
    private SocietyStatus status;
    private FacultyAdvisor advisor;
    private List<Membership> memberships;
    private List<Event>      events;
    private String       archiveReason;
    private String       logoPath;      // absolute path to society logo image

    public Society() {
        this.memberships = new ArrayList<>();
        this.events      = new ArrayList<>();
        this.status      = SocietyStatus.ACTIVE;
    }

    public Society(String societyId, String name, String category, String description, int memberLimit) {
        this.societyId   = societyId;
        this.name        = name;
        this.category    = category;
        this.description = description;
        this.memberLimit = memberLimit;
        this.status      = SocietyStatus.ACTIVE;
        this.memberships = new ArrayList<>();
        this.events      = new ArrayList<>();
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public int getActiveCount() {
        return (int) memberships.stream()
            .filter(m -> m.getStatus().name().equals("APPROVED"))
            .count();
    }

    public boolean isFull() {
        return getActiveCount() >= memberLimit;
    }

    public List<Event> getUpcomingEvents() {
        return events.stream()
            .filter(e -> e.getState().name().equals("REGISTRATION_OPEN") ||
                         e.getState().name().equals("PUBLISHED"))
            .collect(Collectors.toList());
    }

    public void archive(String reason) {
        this.status = SocietyStatus.ARCHIVED;
        this.archiveReason = reason;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getSocietyId()               { return societyId; }
    public void   setSocietyId(String id)      { this.societyId = id; }

    public String getName()                    { return name; }
    public void   setName(String name)         { this.name = name; }

    public String getCategory()                { return category; }
    public void   setCategory(String cat)      { this.category = cat; }

    public String getDescription()             { return description; }
    public void   setDescription(String desc)  { this.description = desc; }

    public int    getMemberLimit()             { return memberLimit; }
    public void   setMemberLimit(int lim)      { this.memberLimit = lim; }

    public SocietyStatus getStatus()                   { return status; }
    public void          setStatus(SocietyStatus s)    { this.status = s; }

    public FacultyAdvisor getAdvisor()                 { return advisor; }
    public void           setAdvisor(FacultyAdvisor a) { this.advisor = a; }

    public List<Membership> getMemberships()             { return memberships; }
    public void             setMemberships(List<Membership> m) { this.memberships = m; }
    public void             addMembership(Membership m)  { this.memberships.add(m); }

    public List<Event> getEvents()               { return events; }
    public void        setEvents(List<Event> e)  { this.events = e; }
    public void        addEvent(Event e)          { this.events.add(e); }

    public String getArchiveReason()              { return archiveReason; }

    public String getLogoPath()                   { return logoPath; }
    public void   setLogoPath(String path)        { this.logoPath = path; }

    @Override
    public String toString() {
        return name + " [" + category + "]";
    }
}
