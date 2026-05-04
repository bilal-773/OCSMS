package com.ocsms.model;

import com.ocsms.enums.UserRole;

/**
 * FacultyAdvisor — extends User.
 * Creates societies, approves events, views dashboards.
 */
public class FacultyAdvisor extends User {

    private String department;

    public FacultyAdvisor() {
        super();
        this.role = UserRole.FACULTY_ADVISOR;
    }

    public FacultyAdvisor(String userId, String rollNumber, String name, String email, String passwordHash) {
        super(userId, rollNumber, name, email, passwordHash, UserRole.FACULTY_ADVISOR);
    }

    public String getDepartment()              { return department; }
    public void   setDepartment(String dept)  { this.department = dept; }
}
