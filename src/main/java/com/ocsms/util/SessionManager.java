package com.ocsms.util;

import com.ocsms.model.User;

/**
 * SessionManager — Singleton Pattern.
 * Stores the currently logged-in user globally across all controllers.
 * 
 * Design Pattern: Singleton
 * Justification: One global session must exist across the entire application lifecycle.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser()         { return currentUser; }
    public void setCurrentUser(User u)   { this.currentUser = u; }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
