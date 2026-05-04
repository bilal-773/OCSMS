package com.ocsms.service;

import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.repository.UserRepository;
import com.ocsms.util.SessionManager;

import java.util.UUID;

/**
 * AuthService — login, student self-registration, and Uni Admin president creation.
 *
 * Registration rules:
 *   STUDENT       → self-register allowed
 *   SOCIETY_ADMIN → created ONLY by University Admin
 *   Others        → hardcoded, no registration
 */
public class AuthService {

    private final UserRepository userRepo;

    public AuthService() {
        this.userRepo = new UserRepository();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public User login(String rollNumber, String password) {
        if (rollNumber == null || rollNumber.isBlank() || password == null || password.isBlank()) return null;
        return userRepo.findByRollNumber(rollNumber)
                .filter(u -> u.getPasswordHash().equals(password) && u.isActive())
                .orElse(null);
    }

    // ── Student Self-Registration ─────────────────────────────────────────────

    public Student registerStudent(String rollNumber, String name, String email, String password) {
        validateRegistration(rollNumber, name, email, password);
        String userId = "U-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Student student = new Student(userId, rollNumber, name, email, password);
        userRepo.save(student);
        return student;
    }

    // ── Uni Admin: Create Society President ───────────────────────────────────

    /**
     * University Admin creates a Society President account.
     * The admin sets both the roll number and password.
     * President can login immediately after creation.
     */
    public SocietyAdmin createPresidentAccount(String rollNumber, String name, String email,
                                               String password, String societyName) {
        // Only Uni Admin can call this
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.UNIVERSITY_ADMIN) {
            throw new IllegalStateException("Only University Admin can create President accounts.");
        }

        validateRegistration(rollNumber, name, email, password);

        String userId = "P-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        SocietyAdmin president = new SocietyAdmin(userId, rollNumber, name, email, password);
        userRepo.save(president);
        return president;
    }

    /** University Admin deletes a President account. */
    public void deletePresidentAccount(String userId) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.UNIVERSITY_ADMIN) {
            throw new IllegalStateException("Only University Admin can delete President accounts.");
        }
        userRepo.delete(userId);
    }

    /** University Admin enables/disables a President account. */
    public void setPresidentActive(String userId, boolean active) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.UNIVERSITY_ADMIN) {
            throw new IllegalStateException("Only University Admin can change account status.");
        }
        userRepo.setActive(userId, active);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateRegistration(String rollNumber, String name, String email, String password) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (!isValidRollNumber(rollNumber))
            throw new IllegalArgumentException("Invalid roll number format. Use: 21F-3456");
        if (userRepo.existsByRollNumber(rollNumber))
            throw new IllegalArgumentException("Roll number already registered.");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Valid email address is required.");
        if (userRepo.existsByEmail(email))
            throw new IllegalArgumentException("Email address already in use.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
    }

    private boolean isValidRollNumber(String roll) {
        return roll != null && roll.matches("\\d{2}[A-Za-z]-\\d{3,5}");
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
