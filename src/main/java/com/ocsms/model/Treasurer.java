package com.ocsms.model;

import com.ocsms.enums.UserRole;

/**
 * Treasurer — extends User.
 * Logs income/expenses, uploads receipts, generates finance reports.
 */
public class Treasurer extends User {

    private Society assignedSociety;

    public Treasurer() {
        super();
        this.role = UserRole.TREASURER;
    }

    public Treasurer(String userId, String rollNumber, String name, String email, String passwordHash) {
        super(userId, rollNumber, name, email, passwordHash, UserRole.TREASURER);
    }

    public Society getAssignedSociety()              { return assignedSociety; }
    public void    setAssignedSociety(Society s)    { this.assignedSociety = s; }
}
