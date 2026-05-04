package com.ocsms.service;

import com.ocsms.model.*;
import com.ocsms.repository.EventRepository;
import com.ocsms.util.PDFGenerator;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * CertificateService — generates and stores participation certificates.
 * Uses PDFBox via PDFGenerator utility.
 */
public class CertificateService {

    private final EventRepository    eventRepo;
    private final NotificationService notificationService;

    public CertificateService() {
        this.eventRepo           = new EventRepository();
        this.notificationService = new NotificationService();
    }

    /**
     * Generates a certificate for a student who attended an event.
     * Saves metadata to repo and triggers PDF generation.
     */
    public Certificate generate(Student student, Event event) {
        // Check for duplicate
        List<Certificate> existing = eventRepo.findCertificatesByEvent(event.getEventId());
        boolean alreadyGenerated = existing.stream()
            .anyMatch(c -> c.getRecipient().getUserId().equals(student.getUserId()));

        if (alreadyGenerated) {
            return existing.stream()
                .filter(c -> c.getRecipient().getUserId().equals(student.getUserId()))
                .findFirst().orElse(null);
        }

        String verCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Certificate cert = new Certificate(student, event, verCode);

        eventRepo.saveCertificate(cert);
        student.addCertificate(cert);

        // Generate the PDF file
        try {
            String path = PDFGenerator.generateCertificate(cert);
            System.out.println("Certificate saved: " + path);
        } catch (IOException e) {
            System.err.println("PDF generation failed for " + student.getName() + ": " + e.getMessage());
        }

        // Notify student (UC17 post-condition)
        notificationService.notify(student.getName(), "Certificate Ready",
            "Your certificate for '" + event.getTitle() + "' is available. Code: " + verCode);

        return cert;
    }

    /**
     * Generates certificates for ALL students who attended an event.
     */
    public int generateAll(Event event) {
        List<EventRegistration> registrations = eventRepo.findRegistrationsByEvent(event.getEventId());
        int count = 0;
        for (EventRegistration reg : registrations) {
            if (reg.isAttended()) {
                generate(reg.getStudent(), event);
                count++;
            }
        }
        return count;
    }

    public List<Certificate> getCertificatesForStudent(String studentId) {
        return eventRepo.findCertificatesByStudent(studentId);
    }

    /**
     * Verifies a certificate by its verification code.
     */
    public boolean verify(String code) {
        return eventRepo.findCertificateByCode(code).isPresent();
    }

    /** Alias for generate() — issues a new certificate. */
    public Certificate issueCertificate(Student student, Event event) {
        return generate(student, event);
    }

    /** Returns true if the student already has a certificate for the given event. */
    public boolean hasCertificate(String studentId, String eventId) {
        return eventRepo.findCertificatesByStudent(studentId).stream()
            .anyMatch(c -> c.getEvent().getEventId().equals(eventId));
    }

    /** Generates and saves a single certificate PDF, returns the file path. */
    public String generateAndSaveSingle(Certificate cert) throws Exception {
        return PDFGenerator.generateCertificate(cert);
    }
}
