package com.ocsms.service;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService — Observer Pattern implementation.
 * Collects in-app notifications; GUI controllers read from here to display badges.
 *
 * Design Pattern: Observer
 * Justification: Decouples event sources (MembershipService, EventService) from
 *                notification display. New channels (email, SMS) can be added without
 *                changing existing service code.
 */
public class NotificationService {

    // Shared notification store (static so all services share the same list in-memory)
    private static final List<Notification> notifications = new ArrayList<>();

    public void notify(String recipientName, String title, String message) {
        notifications.add(new Notification(recipientName, title, message));
        System.out.println("[NOTIFICATION → " + recipientName + "] " + title + ": " + message);
    }

    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public List<Notification> getNotificationsFor(String recipientName) {
        List<Notification> result = new ArrayList<>();
        for (Notification n : notifications) {
            if (n.getRecipientName().equals(recipientName)) result.add(n);
        }
        return result;
    }

    public void clearAll() {
        notifications.clear();
    }

    // ── Inner Notification record ────────────────────────────────────────────

    public static class Notification {
        private final String recipientName;
        private final String title;
        private final String message;

        public Notification(String recipientName, String title, String message) {
            this.recipientName = recipientName;
            this.title = title;
            this.message = message;
        }

        public String getRecipientName() { return recipientName; }
        public String getTitle()         { return title; }
        public String getMessage()       { return message; }

        @Override
        public String toString() { return "[" + title + "] " + message; }
    }
}
