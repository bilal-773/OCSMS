package com.ocsms.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Announcement — entity.
 * Posted by a SocietyAdmin for all society members.
 */
public class Announcement {

    private String        announcementId;
    private String        title;
    private String        body;
    private Society       society;
    private SocietyAdmin  postedBy;
    private LocalDateTime postedAt;

    public Announcement() {
        this.announcementId = UUID.randomUUID().toString();
        this.postedAt       = LocalDateTime.now();
    }

    public Announcement(String title, String body, Society society, SocietyAdmin postedBy) {
        this.announcementId = UUID.randomUUID().toString();
        this.title          = title;
        this.body           = body;
        this.society        = society;
        this.postedBy       = postedBy;
        this.postedAt       = LocalDateTime.now();
    }

    public String getAnnouncementId()            { return announcementId; }
    public String getTitle()                     { return title; }
    public void   setTitle(String t)             { this.title = t; }
    public String getBody()                      { return body; }
    public void   setBody(String b)              { this.body = b; }
    public Society getSociety()                  { return society; }
    public void    setSociety(Society s)         { this.society = s; }
    public SocietyAdmin getPostedBy()            { return postedBy; }
    public void         setPostedBy(SocietyAdmin a) { this.postedBy = a; }
    public LocalDateTime getPostedAt()           { return postedAt; }
}
