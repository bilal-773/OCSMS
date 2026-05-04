package com.ocsms.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BudgetAllocation — represents a budget granted by Treasurer to a specific Society.
 * Linked to the society. President can see it + upload expense bills against it.
 */
public class BudgetAllocation {

    private String     allocationId;
    private Society    society;
    private double     totalBudget;
    private double     usedAmount;
    private String     note;
    private LocalDate  allocatedDate;
    private LocalDate  expiryDate;
    private List<BudgetBill> bills;

    public BudgetAllocation() {
        this.allocationId  = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        this.allocatedDate = LocalDate.now();
        this.usedAmount    = 0.0;
        this.bills         = new ArrayList<>();
    }

    public BudgetAllocation(Society society, double totalBudget, String note, LocalDate expiry) {
        this();
        this.society     = society;
        this.totalBudget = totalBudget;
        this.note        = note;
        this.expiryDate  = expiry;
    }

    public double getRemainingBudget() { return totalBudget - usedAmount; }
    public boolean isOverspent()       { return usedAmount > totalBudget; }

    public void addBill(BudgetBill bill) {
        this.bills.add(bill);
        this.usedAmount += bill.getAmount();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String     getAllocationId()              { return allocationId; }
    public void       setAllocationId(String id)    { this.allocationId = id; }

    public Society    getSociety()                   { return society; }
    public void       setSociety(Society s)          { this.society = s; }

    public double     getTotalBudget()               { return totalBudget; }
    public void       setTotalBudget(double b)       { this.totalBudget = b; }

    public double     getUsedAmount()                { return usedAmount; }
    public void       setUsedAmount(double u)        { this.usedAmount = u; }

    public String     getNote()                      { return note; }
    public void       setNote(String n)              { this.note = n; }

    public LocalDate  getAllocatedDate()              { return allocatedDate; }
    public void       setAllocatedDate(LocalDate d)  { this.allocatedDate = d; }

    public LocalDate  getExpiryDate()                { return expiryDate; }
    public void       setExpiryDate(LocalDate d)     { this.expiryDate = d; }

    public List<BudgetBill> getBills()               { return bills; }
    public void             setBills(List<BudgetBill> b) { this.bills = b; }
}
