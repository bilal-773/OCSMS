package com.ocsms.model;

import com.ocsms.enums.MembershipStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Membership — entity.
 * Tracks a student's membership application lifecycle for a society.
 */
public class Membership {

    private String           membershipId;
    private Student          student;
    private Society          society;
    private String           motivation;
    private MembershipStatus status;
    private LocalDate        appliedDate;
    private String           rejectionReason;

    public Membership() {
        this.membershipId = UUID.randomUUID().toString();
        this.appliedDate  = LocalDate.now();
        this.status       = MembershipStatus.PENDING;
    }

    public Membership(Student student, Society society, String motivation) {
        this.membershipId = UUID.randomUUID().toString();
        this.student      = student;
        this.society      = society;
        this.motivation   = motivation;
        this.status       = MembershipStatus.PENDING;
        this.appliedDate  = LocalDate.now();
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public void approve() {
        this.status = MembershipStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = MembershipStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public boolean isPending() {
        return status == MembershipStatus.PENDING;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getMembershipId()              { return membershipId; }
    public void   setMembershipId(String id)     { this.membershipId = id; }

    public Student getStudent()                  { return student; }
    public void    setStudent(Student s)         { this.student = s; }

    public Society getSociety()                  { return society; }
    public void    setSociety(Society s)         { this.society = s; }

    public String getMotivation()                { return motivation; }
    public void   setMotivation(String m)        { this.motivation = m; }

    public MembershipStatus getStatus()              { return status; }
    public void             setStatus(MembershipStatus s) { this.status = s; }

    public LocalDate getAppliedDate()                { return appliedDate; }
    public void      setAppliedDate(LocalDate d)     { this.appliedDate = d; }

    public String getRejectionReason()               { return rejectionReason; }
    public void   setRejectionReason(String reason)  { this.rejectionReason = reason; }
}
