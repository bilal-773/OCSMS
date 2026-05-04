package com.ocsms.util;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SupabaseClient — thin wrapper around OkHttp for Supabase PostgREST API.
 *
 * KEY TYPE REQUIRED: "service_role" or "anon" JWT (starts with "eyJ...")
 * Found at: Supabase Dashboard → Project → Settings → API
 *
 * The "sbp_..." Personal Access Token (PAT) is for the Management API only.
 */
public class SupabaseClient {

    // ── Supabase project credentials ────────────────────────────────────────────
    // Project URL — confirmed correct from the project ID
    // Project ref decoded from JWT: hfdmsimhiaqonngiioqe
    private static final String SUPABASE_URL =
            "https://hfdmsimhiaqonngiioqe.supabase.co";

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhmZG1zaW1oaWFxb25uZ2lpb3FlIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NzM5MzgyMywiZXhwIjoyMDkyOTY5ODIzfQ.zJIYZ4b2bt3JzZ94kggzBZTfwkz4cH7J4GznE__Y36M";

    // ── Singleton ───────────────────────────────────────────────────────────────
    private static SupabaseClient instance;

    private final OkHttpClient http;
    private final Gson         gson;

    private SupabaseClient() {
        this.http = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setLenient().create();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) instance = new SupabaseClient();
        return instance;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * GET /rest/v1/{table}?{query}
     * Returns the response body as a JsonArray (may be empty).
     */
    public JsonArray get(String table, String query) throws IOException {
        String url = SUPABASE_URL + "/rest/v1/" + table
                   + (query != null && !query.isBlank() ? "?" + query : "");

        Request req = new Request.Builder()
                .url(url)
                .header("apikey",        SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type",  "application/json")
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "[]";
            if (!res.isSuccessful()) {
                throw new IOException(buildError("GET", res.code(), body));
            }
            JsonElement el = JsonParser.parseString(body);
            return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
        } catch (IOException e) {
            throw new IOException(wrapNetworkError("GET", table, e), e);
        }
    }

    /**
     * POST /rest/v1/{table} with JSON body.
     * Returns the inserted row as a JsonObject (or null on failure).
     */
    public JsonObject post(String table, JsonObject payload) throws IOException {
        String jsonBody = gson.toJson(payload);
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request req = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/" + table)
                .header("apikey",        SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type",  "application/json")
                .header("Prefer",        "return=representation")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            String respBody = res.body() != null ? res.body().string() : "null";
            if (!res.isSuccessful()) {
                throw new IOException(buildError("POST", res.code(), respBody));
            }
            JsonElement el = JsonParser.parseString(respBody);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) {
                return el.getAsJsonArray().get(0).getAsJsonObject();
            }
            return null;
        } catch (IOException e) {
            throw new IOException(wrapNetworkError("POST", table, e), e);
        }
    }

    /**
     * PATCH /rest/v1/{table}?{filter} with JSON body.
     */
    public void patch(String table, String filter, JsonObject payload) throws IOException {
        String url = SUPABASE_URL + "/rest/v1/" + table
                   + (filter != null && !filter.isBlank() ? "?" + filter : "");
        RequestBody body = RequestBody.create(
                gson.toJson(payload), MediaType.parse("application/json"));

        Request req = new Request.Builder()
                .url(url)
                .header("apikey",        SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type",  "application/json")
                .patch(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                String respBody = res.body() != null ? res.body().string() : "";
                throw new IOException(buildError("PATCH", res.code(), respBody));
            }
        } catch (IOException e) {
            throw new IOException(wrapNetworkError("PATCH", table, e), e);
        }
    }

    /**
     * DELETE /rest/v1/{table}?{filter}
     */
    public void delete(String table, String filter) throws IOException {
        String url = SUPABASE_URL + "/rest/v1/" + table
                   + (filter != null && !filter.isBlank() ? "?" + filter : "");
        Request req = new Request.Builder()
                .url(url)
                .header("apikey",        SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .delete()
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                String respBody = res.body() != null ? res.body().string() : "";
                throw new IOException(buildError("DELETE", res.code(), respBody));
            }
        } catch (IOException e) {
            throw new IOException(wrapNetworkError("DELETE", table, e), e);
        }
    }

    // ── Error builders ───────────────────────────────────────────────────────────

    private String buildError(String method, int code, String body) {
        String hint = "";
        if (code == 401) {
            hint = " [HINT: Wrong API key — use service_role key from Supabase Dashboard → Settings → API]";
        } else if (code == 404 || body.contains("relation") && body.contains("does not exist")) {
            hint = " [HINT: Table 'ocsms_users' not found — run the SQL setup script in Supabase SQL Editor]";
        } else if (code == 403) {
            hint = " [HINT: Row Level Security is blocking the request — run: ALTER TABLE ocsms_users DISABLE ROW LEVEL SECURITY;]";
        }
        return "Supabase " + method + " failed [HTTP " + code + "]" + hint + "\nResponse: " + body;
    }

    private String wrapNetworkError(String method, String table, IOException e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (msg.contains("Unable to resolve host") || msg.equals(SUPABASE_URL.replace("https://", ""))) {
            return "No internet connection or Supabase host unreachable. Check network and try again.";
        }
        return "Network error during " + method + " on table '" + table + "': " + msg;
    }

    public Gson getGson() { return gson; }
}
