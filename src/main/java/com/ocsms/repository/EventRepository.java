package com.ocsms.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ocsms.enums.EventState;
import com.ocsms.model.*;
import com.ocsms.util.SupabaseClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EventRepository — Supabase-backed for Events.
 * EventRegistrations and Certificates remain in-memory (extend to Supabase later).
 *
 * Table: ocsms_events
 *   id                   TEXT PRIMARY KEY
 *   title                TEXT NOT NULL
 *   society_id           TEXT NOT NULL  -- FK → ocsms_societies.id
 *   event_date           TEXT           -- ISO datetime string
 *   venue                TEXT
 *   capacity             INT DEFAULT 100
 *   registered_count     INT DEFAULT 0
 *   registration_deadline TEXT
 *   state                TEXT DEFAULT 'DRAFT'
 *   description          TEXT
 *   event_type           TEXT
 *   created_at           TIMESTAMPTZ DEFAULT now()
 */
public class EventRepository {

    private static final String TABLE = "ocsms_events";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // In-memory stores for registrations/certs (no Supabase tables yet)
    private static final Map<String, EventRegistration> regStore  = new HashMap<>();
    private static final Map<String, Certificate>       certStore = new HashMap<>();

    // ── Row → Event ───────────────────────────────────────────────────────────
    private Event rowToEvent(JsonObject row, SocietyRepository socRepo) {
        Event e = new Event();
        e.setEventId(row.get("id").getAsString());
        e.setTitle(row.get("title").getAsString());
        if (row.has("venue") && !row.get("venue").isJsonNull())
            e.setVenue(row.get("venue").getAsString());
        if (row.has("capacity") && !row.get("capacity").isJsonNull())
            e.setCapacity(row.get("capacity").getAsInt());
        if (row.has("registered_count") && !row.get("registered_count").isJsonNull())
            e.setRegisteredCount(row.get("registered_count").getAsInt());
        if (row.has("event_date") && !row.get("event_date").isJsonNull()) {
            try { e.setDateTime(LocalDateTime.parse(row.get("event_date").getAsString(), FMT)); }
            catch (Exception ex) { e.setDateTime(LocalDateTime.now().plusDays(7)); }
        }
        if (row.has("registration_deadline") && !row.get("registration_deadline").isJsonNull()) {
            try { e.setRegistrationDeadline(LocalDateTime.parse(row.get("registration_deadline").getAsString(), FMT)); }
            catch (Exception ex) { e.setRegistrationDeadline(LocalDateTime.now().plusDays(5)); }
        }
        String stateStr = row.has("state") ? row.get("state").getAsString() : "DRAFT";
        try { e.setState(EventState.valueOf(stateStr)); } catch (Exception ex) { e.setState(EventState.DRAFT); }
        if (row.has("description") && !row.get("description").isJsonNull())
            e.setDescription(row.get("description").getAsString());
        if (row.has("event_type") && !row.get("event_type").isJsonNull())
            e.setEventType(row.get("event_type").getAsString());
        // Load society
        if (row.has("society_id") && !row.get("society_id").isJsonNull()) {
            String socId = row.get("society_id").getAsString();
            socRepo.findById(socId).ifPresent(e::setOrganizer);
        }
        return e;
    }

    // ── Event CRUD ────────────────────────────────────────────────────────────

    public Optional<Event> findById(String eventId) {
        SocietyRepository socRepo = new SocietyRepository();
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "id=eq." + eventId);
            if (rows.size() > 0) return Optional.of(rowToEvent(rows.get(0).getAsJsonObject(), socRepo));
        } catch (IOException e) { System.err.println("[EventRepo] findById: " + e.getMessage()); }
        return Optional.empty();
    }

    public List<Event> findAll() {
        SocietyRepository socRepo = new SocietyRepository();
        List<Event> result = new ArrayList<>();
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, null);
            for (var el : rows) result.add(rowToEvent(el.getAsJsonObject(), socRepo));
        } catch (IOException e) { System.err.println("[EventRepo] findAll: " + e.getMessage()); }
        return result;
    }

    public void save(Event event) {
        JsonObject payload = buildPayload(event);
        try {
            SupabaseClient.getInstance().post(TABLE, payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save event: " + e.getMessage(), e);
        }
    }

    public void update(Event event) {
        JsonObject payload = buildPayload(event);
        try {
            SupabaseClient.getInstance().patch(TABLE, "id=eq." + event.getEventId(), payload);
        } catch (IOException e) { System.err.println("[EventRepo] update: " + e.getMessage()); }
    }

    public void delete(String eventId) {
        try {
            SupabaseClient.getInstance().delete(TABLE, "id=eq." + eventId);
        } catch (IOException e) { System.err.println("[EventRepo] delete: " + e.getMessage()); }
    }

    public List<Event> findBySociety(String societyId) {
        SocietyRepository socRepo = new SocietyRepository();
        List<Event> result = new ArrayList<>();
        try {
            JsonArray rows = SupabaseClient.getInstance().get(TABLE, "society_id=eq." + societyId);
            for (var el : rows) result.add(rowToEvent(el.getAsJsonObject(), socRepo));
        } catch (IOException e) { System.err.println("[EventRepo] findBySociety: " + e.getMessage()); }
        return result;
    }

    public List<Event> findByState(EventState state) {
        return findAll().stream().filter(e -> e.getState() == state).collect(Collectors.toList());
    }

    private JsonObject buildPayload(Event event) {
        JsonObject p = new JsonObject();
        p.addProperty("id",          event.getEventId());
        p.addProperty("title",       event.getTitle());
        p.addProperty("venue",       event.getVenue() != null ? event.getVenue() : "");
        p.addProperty("capacity",    event.getCapacity());
        p.addProperty("registered_count", event.getRegisteredCount());
        p.addProperty("state",       event.getState().name());
        p.addProperty("description", event.getDescription() != null ? event.getDescription() : "");
        p.addProperty("event_type",  event.getEventType() != null ? event.getEventType() : "");
        if (event.getOrganizer() != null)
            p.addProperty("society_id", event.getOrganizer().getSocietyId());
        if (event.getDateTime() != null)
            p.addProperty("event_date", event.getDateTime().format(FMT));
        if (event.getRegistrationDeadline() != null)
            p.addProperty("registration_deadline", event.getRegistrationDeadline().format(FMT));
        return p;
    }

    // ── EventRegistration (in-memory) ─────────────────────────────────────────

    public void saveRegistration(EventRegistration reg)   { regStore.put(reg.getRegId(), reg); }
    public void updateRegistration(EventRegistration reg) { regStore.put(reg.getRegId(), reg); }

    public List<EventRegistration> findRegistrationsByEvent(String eventId) {
        return regStore.values().stream()
            .filter(r -> r.getEvent().getEventId().equals(eventId))
            .collect(Collectors.toList());
    }
    public List<EventRegistration> findRegistrationsByStudent(String studentId) {
        return regStore.values().stream()
            .filter(r -> r.getStudent().getUserId().equals(studentId))
            .collect(Collectors.toList());
    }
    public boolean isAlreadyRegistered(String studentId, String eventId) {
        return regStore.values().stream()
            .anyMatch(r -> r.getStudent().getUserId().equals(studentId)
                       && r.getEvent().getEventId().equals(eventId));
    }
    public Optional<EventRegistration> findRegistration(String studentId, String eventId) {
        return regStore.values().stream()
            .filter(r -> r.getStudent().getUserId().equals(studentId)
                      && r.getEvent().getEventId().equals(eventId))
            .findFirst();
    }

    // ── Certificate (in-memory) ───────────────────────────────────────────────

    public void saveCertificate(Certificate cert) { certStore.put(cert.getCertId(), cert); }
    public List<Certificate> findCertificatesByStudent(String studentId) {
        return certStore.values().stream()
            .filter(c -> c.getRecipient().getUserId().equals(studentId))
            .collect(Collectors.toList());
    }
    public List<Certificate> findCertificatesByEvent(String eventId) {
        return certStore.values().stream()
            .filter(c -> c.getEvent().getEventId().equals(eventId))
            .collect(Collectors.toList());
    }
    public Optional<Certificate> findCertificateByCode(String code) {
        return certStore.values().stream()
            .filter(c -> c.getVerificationCode().equals(code))
            .findFirst();
    }

    // ── Aliases ───────────────────────────────────────────────────────────────
    public List<EventRegistration> getRegistrationsForEvent(String eventId)   { return findRegistrationsByEvent(eventId); }
    public List<EventRegistration> getRegistrationsForStudent(String studentId) { return findRegistrationsByStudent(studentId); }
}
