package com.ocsms.model;

import com.ocsms.enums.EntryType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * FinanceEntry — entity.
 * Tracks income and expenses for a society.
 */
public class FinanceEntry {

    private String    entryId;
    private Society   society;
    private EntryType type;
    private double    amount;
    private String    description;
    private LocalDate date;
    private boolean   hasReceipt;

    public FinanceEntry() {
        this.entryId = UUID.randomUUID().toString();
        this.date    = LocalDate.now();
    }

    public FinanceEntry(Society society, EntryType type, double amount, String description) {
        this.entryId     = UUID.randomUUID().toString();
        this.society     = society;
        this.type        = type;
        this.amount      = amount;
        this.description = description;
        this.date        = LocalDate.now();
        this.hasReceipt  = false;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public String getEntryId()               { return entryId; }
    public void   setEntryId(String id)      { this.entryId = id; }

    public Society getSociety()              { return society; }
    public void    setSociety(Society s)     { this.society = s; }

    public EntryType getType()               { return type; }
    public void      setType(EntryType t)    { this.type = t; }

    public double getAmount()                { return amount; }
    public void   setAmount(double amt)      { this.amount = amt; }

    public String getDescription()           { return description; }
    public void   setDescription(String d)   { this.description = d; }

    public LocalDate getDate()               { return date; }
    public void      setDate(LocalDate d)    { this.date = d; }

    public boolean isHasReceipt()            { return hasReceipt; }
    public void    setHasReceipt(boolean r)  { this.hasReceipt = r; }
}
