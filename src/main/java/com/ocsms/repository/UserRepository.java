package com.ocsms.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.util.SupabaseClient;

import java.io.IOException;
import java.util.*;

/**
 * UserRepository — hybrid persistence.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  STUDENT         → Supabase  (self-registered)           │
 * │  SOCIETY_ADMIN   → Supabase  (created by Uni Admin)      │
 * │  TREASURER       → Hardcoded in-memory                   │
 * │  FACULTY_ADVISOR → Hardcoded in-memory                   │
 * │  UNIVERSITY_ADMIN→ Hardcoded in-memory                   │
 * └──────────────────────────────────────────────────────────┘
 */
public class UserRepository {

    private static final String TABLE = "ocsms_users";

    // ── Hardcoded privileged accounts ─────────────────────────────────────────
    private static final Map<String, User> HARDCODED = new HashMap<>();
    static {
        Treasurer t1 = new Treasurer("HC-T001", "TR001", "Hamza Sheikh",
                "hamza@nu.edu.pk", "treasury1");
        UniversityAdmin ua1 = new UniversityAdmin("HC-UA001", "ADMIN001", "Admin User",
                "admin@nu.edu.pk", "admin@nu");

        HARDCODED.put(t1.getRollNumber(),  t1);
        HARDCODED.put(ua1.getRollNumber(), ua1);
    }

    // ── Row → User ────────────────────────────────────────────────────────────
    private User rowToUser(JsonObject row) {
        String id       = row.get("id").getAsString();
        String roll     = row.get("roll_number").getAsString();
        String name     = row.get("name").getAsString();
        String email    = row.get("email").getAsString();
        String password = row.get("password").getAsString();
        String roleStr  = row.get("role").getAsString();
        boolean active  = !row.has("is_active") || row.get("is_active").getAsBoolean();

        User user;
        if ("SOCIETY_ADMIN".equals(roleStr)) {
            SocietyAdmin sa = new SocietyAdmin(id, roll, name, email, password);
            sa.setActive(active);
            user = sa;
        } else {
            Student st = new Student(id, roll, name, email, password);
            st.setActive(active);
            user = st;
        }
        return user;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Optional<User> findByRollNumber(String rollNumber) {
        if (HARDCODED.containsKey(rollNumber)) return Optional.of(HARDCODED.get(rollNumber));
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "roll_number=eq." + rollNumber);
            if (rows.size() > 0) return Optional.of(rowToUser(rows.get(0).getAsJsonObject()));
        } catch (IOException e) { System.err.println("[UserRepo] findByRollNumber: " + e.getMessage()); }
        return Optional.empty();
    }

    public Optional<User> findById(String userId) {
        for (User u : HARDCODED.values()) { if (u.getUserId().equals(userId)) return Optional.of(u); }
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "id=eq." + userId);
            if (rows.size() > 0) return Optional.of(rowToUser(rows.get(0).getAsJsonObject()));
        } catch (IOException e) { System.err.println("[UserRepo] findById: " + e.getMessage()); }
        return Optional.empty();
    }

    /** Save a Student or SocietyAdmin to Supabase. */
    public void save(User user) {
        if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.SOCIETY_ADMIN) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("id",          user.getUserId());
        payload.addProperty("roll_number", user.getRollNumber());
        payload.addProperty("name",        user.getName());
        payload.addProperty("email",       user.getEmail());
        payload.addProperty("password",    user.getPasswordHash());
        payload.addProperty("role",        user.getRole().name());
        payload.addProperty("is_active",   user.isActive());
        try {
            SupabaseClient.getInstance().post(TABLE, payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    /** Delete a user from Supabase (Uni Admin can remove a president). */
    public void delete(String userId) {
        try {
            SupabaseClient.getInstance().delete(TABLE, "id=eq." + userId);
        } catch (IOException e) { System.err.println("[UserRepo] delete: " + e.getMessage()); }
    }

    /** Set active flag on a Supabase user. */
    public void setActive(String userId, boolean active) {
        JsonObject patch = new JsonObject();
        patch.addProperty("is_active", active);
        try {
            SupabaseClient.getInstance().patch(TABLE, "id=eq." + userId, patch);
        } catch (IOException e) { System.err.println("[UserRepo] setActive: " + e.getMessage()); }
    }

    public List<User> findAll() {
        List<User> result = new ArrayList<>(HARDCODED.values());
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, null);
            for (var el : rows) result.add(rowToUser(el.getAsJsonObject()));
        } catch (IOException e) { System.err.println("[UserRepo] findAll: " + e.getMessage()); }
        return result;
    }

    public List<User> findByRole(UserRole role) {
        List<User> result = new ArrayList<>();
        for (User u : HARDCODED.values()) { if (u.getRole() == role) result.add(u); }
        if (role == UserRole.STUDENT || role == UserRole.SOCIETY_ADMIN) {
            try {
                JsonArray rows = SupabaseClient.getInstance().get(TABLE, "role=eq." + role.name());
                for (var el : rows) result.add(rowToUser(el.getAsJsonObject()));
            } catch (IOException e) { System.err.println("[UserRepo] findByRole: " + e.getMessage()); }
        }
        return result;
    }

    public boolean existsByRollNumber(String rollNumber) {
        if (HARDCODED.containsKey(rollNumber)) return true;
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "roll_number=eq." + rollNumber);
            return rows.size() > 0;
        } catch (IOException e) { return false; }
    }

    public boolean existsByEmail(String email) {
        for (User u : HARDCODED.values()) { if (u.getEmail().equalsIgnoreCase(email)) return true; }
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "email=eq." + email);
            return rows.size() > 0;
        } catch (IOException e) { return false; }
    }

    public static Map<String, User> getHardcodedAccounts() {
        return Collections.unmodifiableMap(HARDCODED);
    }
}
