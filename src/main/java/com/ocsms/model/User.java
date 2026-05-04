package com.ocsms.model;

import com.ocsms.enums.UserRole;

/**
 * Abstract base class for all users in the OCSMS system.
 * Implements the Inheritance Hierarchy: User ← Student, SocietyAdmin, Treasurer, FacultyAdvisor, UniversityAdmin
 */
public abstract class User {

    protected String userId;
    protected String rollNumber;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected UserRole role;
    protected boolean isActive;

    public User() {}

    public User(String userId, String rollNumber, String name, String email, String passwordHash, UserRole role) {
        this.userId = userId;
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
    }

    /**
     * Validates credentials against stored hash.
     */
    public boolean login(String roll, String pass) {
        return this.rollNumber.equals(roll) && this.passwordHash.equals(pass);
    }

    public void logout() {
        // Handled by SessionManager
    }

    public boolean hasPermission(String permission) {
        // Role-based permission check — extend as needed
        return isActive;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getUserId()             { return userId; }
    public void   setUserId(String id)   { this.userId = id; }

    public String getRollNumber()              { return rollNumber; }
    public void   setRollNumber(String roll)  { this.rollNumber = roll; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public String getPasswordHash()                     { return passwordHash; }
    public void   setPasswordHash(String passwordHash)  { this.passwordHash = passwordHash; }

    public UserRole getRole()              { return role; }
    public void     setRole(UserRole role) { this.role = role; }

    public boolean isActive()               { return isActive; }
    public void    setActive(boolean active){ this.isActive = active; }

    @Override
    public String toString() {
        return name + " (" + rollNumber + ") [" + role + "]";
    }
}
