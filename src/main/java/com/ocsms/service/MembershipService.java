package com.ocsms.service;

import com.ocsms.model.*;
import com.ocsms.enums.MembershipStatus;
import com.ocsms.repository.SocietyRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MembershipService — business logic for applying, approving, and rejecting memberships.
 * Enforces: duplicate check, capacity check.
 */
public class MembershipService {

    private final SocietyRepository societyRepo;
    private final NotificationService notificationService;

    public MembershipService() {
        this.societyRepo         = new SocietyRepository();
        this.notificationService = new NotificationService();
    }

    /**
     * Apply for membership in a society.
     * Throws if already applied/approved or society is full.
     */
    public Membership applyForMembership(Student student, Society society, String motivation) {
        if (societyRepo.hasActiveMembership(student.getUserId(), society.getSocietyId())) {
            throw new IllegalStateException("You already have an active or pending application for " + society.getName() + ".");
        }
        if (society.isFull()) {
            throw new IllegalStateException("Society " + society.getName() + " has reached its member limit.");
        }

        Membership m = new Membership(student, society, motivation);
        societyRepo.saveMembership(m);
        society.addMembership(m);
        student.getMemberships().add(m);

        notificationService.notify(student.getName(), "Membership Application Submitted",
            "Your application for " + society.getName() + " is under review.");

        return m;
    }

    /**
     * Approve a pending membership application.
     */
    public void approveMembership(Membership membership) {
        if (!membership.isPending()) {
            throw new IllegalStateException("Only PENDING applications can be approved.");
        }
        membership.approve();
        societyRepo.saveMembership(membership);
        notificationService.notify(membership.getStudent().getName(),
            "Membership Approved!",
            "Welcome to " + membership.getSociety().getName() + "!");
    }

    /**
     * Reject a pending membership application.
     * Rejection reason is mandatory.
     */
    public void rejectMembership(Membership membership, String reason) {
        if (!membership.isPending()) {
            throw new IllegalStateException("Only PENDING applications can be rejected.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty.");
        }
        membership.reject(reason);
        societyRepo.saveMembership(membership);
        notificationService.notify(membership.getStudent().getName(),
            "Membership Rejected",
            "Your application for " + membership.getSociety().getName() +
                " was not approved. Reason: " + reason);
    }

    public List<Membership> getPendingMemberships(Society society) {
        return societyRepo.findMembershipsBySociety(society.getSocietyId()).stream()
            .filter(m -> m.getStatus() == MembershipStatus.PENDING)
            .collect(Collectors.toList());
    }

    public List<Membership> getApprovedMemberships(Society society) {
        return societyRepo.findMembershipsBySociety(society.getSocietyId()).stream()
            .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
            .collect(Collectors.toList());
    }

    public List<Membership> getRejectedMemberships(Society society) {
        return societyRepo.findMembershipsBySociety(society.getSocietyId()).stream()
            .filter(m -> m.getStatus() == MembershipStatus.REJECTED)
            .collect(Collectors.toList());
    }

    public List<Membership> getAllMemberships(Society society) {
        return societyRepo.findMembershipsBySociety(society.getSocietyId());
    }

    public boolean hasActiveApplication(String studentId, String societyId) {
        return societyRepo.hasActiveMembership(studentId, societyId);
    }
}
