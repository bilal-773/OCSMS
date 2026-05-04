package com.ocsms.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ocsms.model.Society;
import com.ocsms.enums.SocietyStatus;
import com.ocsms.util.SupabaseClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SocietyRepository — Supabase-backed.
 *
 * Table: ocsms_societies
 *   id            TEXT PRIMARY KEY
 *   name          TEXT UNIQUE NOT NULL
 *   category      TEXT NOT NULL
 *   description   TEXT
 *   member_limit  INT  DEFAULT 50
 *   status        TEXT DEFAULT 'ACTIVE'
 *   president_id  TEXT  -- FK → ocsms_users.id (nullable until assigned)
 *   created_at    TIMESTAMPTZ DEFAULT now()
 */
public class SocietyRepository {

    private static final String TABLE = "ocsms_societies";

    // ── Row → Society ─────────────────────────────────────────────────────────
    private Society rowToSociety(JsonObject row) {
        Society s = new Society();
        s.setSocietyId(row.get("id").getAsString());
        s.setName(row.get("name").getAsString());
        s.setCategory(row.get("category").getAsString());
        if (row.has("description") && !row.get("description").isJsonNull())
            s.setDescription(row.get("description").getAsString());
        if (row.has("member_limit") && !row.get("member_limit").isJsonNull())
            s.setMemberLimit(row.get("member_limit").getAsInt());
        else s.setMemberLimit(50);
        String statusStr = row.has("status") ? row.get("status").getAsString() : "ACTIVE";
        try { s.setStatus(SocietyStatus.valueOf(statusStr)); }
        catch (Exception e) { s.setStatus(SocietyStatus.ACTIVE); }
        return s;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Optional<Society> findById(String societyId) {
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "id=eq." + societyId);
            if (rows.size() > 0) return Optional.of(rowToSociety(rows.get(0).getAsJsonObject()));
        } catch (IOException e) {
            System.err.println("[SocietyRepo] findById: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Society> findByPresidentId(String presidentUserId) {
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "president_id=eq." + presidentUserId);
            if (rows.size() > 0) return Optional.of(rowToSociety(rows.get(0).getAsJsonObject()));
        } catch (IOException e) {
            System.err.println("[SocietyRepo] findByPresidentId: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Society> findAll() {
        List<Society> result = new ArrayList<>();
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, null);
            for (var el : rows) result.add(rowToSociety(el.getAsJsonObject()));
        } catch (IOException e) {
            System.err.println("[SocietyRepo] findAll: " + e.getMessage());
        }
        return result;
    }

    public void save(Society society) {
        JsonObject payload = new JsonObject();
        payload.addProperty("id",           society.getSocietyId());
        payload.addProperty("name",         society.getName());
        payload.addProperty("category",     society.getCategory());
        payload.addProperty("description",  society.getDescription() != null ? society.getDescription() : "");
        payload.addProperty("member_limit", society.getMemberLimit());
        payload.addProperty("status",       society.getStatus().name());
        try {
            SupabaseClient.getInstance().post(TABLE, payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save society: " + e.getMessage(), e);
        }
    }

    public void updateStatus(String societyId, SocietyStatus status) {
        JsonObject patch = new JsonObject();
        patch.addProperty("status", status.name());
        try {
            SupabaseClient.getInstance().patch(TABLE, "id=eq." + societyId, patch);
        } catch (IOException e) {
            System.err.println("[SocietyRepo] updateStatus: " + e.getMessage());
        }
    }

    public void assignPresident(String societyId, String presidentUserId) {
        JsonObject patch = new JsonObject();
        patch.addProperty("president_id", presidentUserId);
        try {
            SupabaseClient.getInstance().patch(TABLE, "id=eq." + societyId, patch);
        } catch (IOException e) {
            System.err.println("[SocietyRepo] assignPresident: " + e.getMessage());
        }
    }

    public void delete(String societyId) {
        try {
            SupabaseClient.getInstance().delete(TABLE, "id=eq." + societyId);
        } catch (IOException e) {
            System.err.println("[SocietyRepo] delete: " + e.getMessage());
        }
    }

    public boolean existsByName(String name) {
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "name=ilike." + name);
            return rows.size() > 0;
        } catch (IOException e) { return false; }
    }

    public List<Society> searchByName(String query) {
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "name=ilike.*" + query + "*");
            List<Society> result = new ArrayList<>();
            for (var el : rows) result.add(rowToSociety(el.getAsJsonObject()));
            return result;
        } catch (IOException e) { return Collections.emptyList(); }
    }

    public List<Society> findByCategory(String category) {
        if (category == null || category.isBlank() || category.equals("All")) return findAll();
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "category=ilike." + category);
            List<Society> result = new ArrayList<>();
            for (var el : rows) result.add(rowToSociety(el.getAsJsonObject()));
            return result;
        } catch (IOException e) { return Collections.emptyList(); }
    }

    // ── Membership (in-memory — extend to Supabase later) ─────────────────────
    private static final Map<String, com.ocsms.model.Membership> membershipStore = new HashMap<>();

    public void saveMembership(com.ocsms.model.Membership m) {
        membershipStore.put(m.getMembershipId(), m);
    }

    public List<com.ocsms.model.Membership> findAllMemberships() {
        return new ArrayList<>(membershipStore.values());
    }

    public List<com.ocsms.model.Membership> findMembershipsBySociety(String societyId) {
        return membershipStore.values().stream()
            .filter(m -> m.getSociety().getSocietyId().equals(societyId))
            .collect(java.util.stream.Collectors.toList());
    }

    public Optional<com.ocsms.model.Membership> findMembershipById(String membershipId) {
        return Optional.ofNullable(membershipStore.get(membershipId));
    }


    public boolean hasActiveMembership(String studentId, String societyId) {
        return membershipStore.values().stream()
            .anyMatch(m -> m.getStudent().getUserId().equals(studentId)
                        && m.getSociety().getSocietyId().equals(societyId)
                        && (m.getStatus().name().equals("APPROVED") || m.getStatus().name().equals("PENDING")));
    }

    /** Persists the society logo path to Supabase. */
    public void updateLogoPath(String societyId, String logoPath) {
        JsonObject patch = new JsonObject();
        patch.addProperty("logo_path", logoPath);
        try {
            SupabaseClient.getInstance().patch(TABLE, "id=eq." + societyId, patch);
        } catch (IOException e) {
            System.err.println("[SocietyRepo] updateLogoPath: " + e.getMessage());
        }
    }
}

