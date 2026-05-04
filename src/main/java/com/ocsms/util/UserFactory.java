package com.ocsms.util;

import com.ocsms.enums.UserRole;
import com.ocsms.model.*;

import java.util.UUID;

/**
 * UserFactory — Factory Method Pattern.
 * Creates the correct User subclass based on the selected role at registration.
 *
 * Design Pattern: Factory Method
 * Justification: Avoids switch-case bloat in controllers. Adding a new role only
 *                requires changing this one class.
 */
public class UserFactory {

    public static User create(UserRole role, String rollNumber, String name, String email, String password) {
        String id = "U" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return switch (role) {
            case STUDENT         -> new Student(id, rollNumber, name, email, password);
            case SOCIETY_ADMIN   -> new SocietyAdmin(id, rollNumber, name, email, password);
            case FACULTY_ADVISOR -> new FacultyAdvisor(id, rollNumber, name, email, password);
            case TREASURER       -> new Treasurer(id, rollNumber, name, email, password);
            default              -> new UniversityAdmin(id, rollNumber, name, email, password);
        };
    }
}
