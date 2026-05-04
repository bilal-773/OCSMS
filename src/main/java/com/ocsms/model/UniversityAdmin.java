package com.ocsms.model;

import com.ocsms.enums.UserRole;

/**
 * UniversityAdmin — extends User.
 * Creates societies, deactivates/archives societies, exports reports.
 */
public class UniversityAdmin extends User {

    public UniversityAdmin() {
        super();
        this.role = UserRole.UNIVERSITY_ADMIN;
    }

    public UniversityAdmin(String userId, String rollNumber, String name, String email, String passwordHash) {
        super(userId, rollNumber, name, email, passwordHash, UserRole.UNIVERSITY_ADMIN);
    }
}
