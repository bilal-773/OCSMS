package com.ocsms.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Certificate — entity.
 * Issued to a Student who attended a Society Event.
 * PDFBox generates the actual PDF file.
 */
public class Certificate {

    private String    certId;
    private Student   recipient;
    private Event     event;
    private LocalDate issuedDate;
    private String    verificationCode;

    public Certificate() {
        this.certId           = UUID.randomUUID().toString();
        this.issuedDate       = LocalDate.now();
        this.verificationCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Certificate(Student recipient, Event event, String verificationCode) {
        this.certId           = UUID.randomUUID().toString();
        this.recipient        = recipient;
        this.event            = event;
        this.issuedDate       = LocalDate.now();
        this.verificationCode = verificationCode;
    }

    // ─── Domain Methods ────────────────────────────────────────────────────────

    public boolean verify(String code) {
        return this.verificationCode.equals(code);
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getCertId()                    { return certId; }
    public void   setCertId(String id)           { this.certId = id; }

    public Student getRecipient()                { return recipient; }
    public void    setRecipient(Student s)       { this.recipient = s; }

    public Event getEvent()                      { return event; }
    public void  setEvent(Event e)               { this.event = e; }

    public LocalDate getIssuedDate()              { return issuedDate; }
    public void      setIssuedDate(LocalDate d)   { this.issuedDate = d; }

    public String getVerificationCode()              { return verificationCode; }
    public void   setVerificationCode(String code)   { this.verificationCode = code; }
}
