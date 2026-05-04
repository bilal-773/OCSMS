package com.ocsms.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BudgetBill — an expense document (receipt/bill) uploaded by the Society President
 * against a specific BudgetAllocation to show how the budget was spent.
 */
public class BudgetBill {

    private String    billId;
    private String    allocationId;
    private String    description;
    private double    amount;
    private String    filePath;       // path to uploaded PDF/image on local disk
    private LocalDate uploadDate;
    private String    uploadedBy;     // name of president who uploaded

    public BudgetBill() {
        this.billId      = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.uploadDate  = LocalDate.now();
    }

    public BudgetBill(String allocationId, String description, double amount,
                      String filePath, String uploadedBy) {
        this();
        this.allocationId = allocationId;
        this.description  = description;
        this.amount       = amount;
        this.filePath     = filePath;
        this.uploadedBy   = uploadedBy;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String    getBillId()                   { return billId; }
    public void      setBillId(String id)          { this.billId = id; }

    public String    getAllocationId()              { return allocationId; }
    public void      setAllocationId(String id)    { this.allocationId = id; }

    public String    getDescription()              { return description; }
    public void      setDescription(String d)      { this.description = d; }

    public double    getAmount()                   { return amount; }
    public void      setAmount(double a)           { this.amount = a; }

    public String    getFilePath()                 { return filePath; }
    public void      setFilePath(String fp)        { this.filePath = fp; }

    public LocalDate getUploadDate()               { return uploadDate; }
    public void      setUploadDate(LocalDate d)    { this.uploadDate = d; }

    public String    getUploadedBy()               { return uploadedBy; }
    public void      setUploadedBy(String u)       { this.uploadedBy = u; }
}
