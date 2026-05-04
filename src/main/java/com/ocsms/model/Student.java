package com.ocsms.model;

import com.ocsms.enums.UserRole;
import java.util.ArrayList;
import java.util.List;

/**
 * Student — extends User.
 * Can apply for society memberships, register for events, vote, and download certificates.
 */
public class Student extends User {

    private List<Membership> memberships;
    private List<EventRegistration> registrations;
    private List<Certificate> certificates;

    public Student() {
        super();
        this.role = UserRole.STUDENT;
        this.memberships  = new ArrayList<>();
        this.registrations = new ArrayList<>();
        this.certificates  = new ArrayList<>();
    }

    public Student(String userId, String rollNumber, String name, String email, String passwordHash) {
        super(userId, rollNumber, name, email, passwordHash, UserRole.STUDENT);
        this.memberships   = new ArrayList<>();
        this.registrations = new ArrayList<>();
        this.certificates  = new ArrayList<>();
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public void applyForMembership(Society s, String motivation) {
        // Creates a Membership object — actual persistence done by MembershipService
        Membership m = new Membership(this, s, motivation);
        memberships.add(m);
    }

    public void registerForEvent(Event e) {
        // Actual capacity/deadline check done by EventService
        EventRegistration reg = new EventRegistration(this, e);
        registrations.add(reg);
    }

    public List<Certificate> getMyCertificates() {
        return new ArrayList<>(certificates);
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public List<Membership>         getMemberships()             { return memberships; }
    public void                     setMemberships(List<Membership> m) { this.memberships = m; }

    public List<EventRegistration>  getRegistrations()           { return registrations; }
    public void                     setRegistrations(List<EventRegistration> r) { this.registrations = r; }

    public List<Certificate>        getCertificates()            { return certificates; }
    public void                     setCertificates(List<Certificate> c) { this.certificates = c; }

    public void addCertificate(Certificate c) { this.certificates.add(c); }
}
