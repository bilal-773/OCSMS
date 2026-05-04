package com.ocsms.model;

import com.ocsms.enums.UserRole;

/**
 * SocietyAdmin (President) — extends User.
 * Manages a single society: approves memberships, creates events, marks attendance.
 */
public class SocietyAdmin extends User {

    private Society managedSociety;
    private String  adminTitle;

    public SocietyAdmin() {
        super();
        this.role = UserRole.SOCIETY_ADMIN;
    }

    public SocietyAdmin(String userId, String rollNumber, String name, String email, String passwordHash) {
        super(userId, rollNumber, name, email, passwordHash, UserRole.SOCIETY_ADMIN);
        this.adminTitle = "President";
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public void approveMembership(Membership m) {
        m.approve();
    }

    public void rejectMembership(Membership m, String reason) {
        m.reject(reason);
    }

    public void markAttendance(Event e, Student s, boolean present) {
        e.getRegistrations().stream()
            .filter(r -> r.getStudent().getUserId().equals(s.getUserId()))
            .findFirst()
            .ifPresent(r -> r.setAttended(present));
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public Society getManagedSociety()              { return managedSociety; }
    public void    setManagedSociety(Society s)    { this.managedSociety = s; }

    public String  getAdminTitle()                 { return adminTitle; }
    public void    setAdminTitle(String title)     { this.adminTitle = title; }
}
