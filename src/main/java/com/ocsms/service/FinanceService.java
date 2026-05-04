package com.ocsms.service;

import com.ocsms.enums.EntryType;
import com.ocsms.model.FinanceEntry;
import com.ocsms.model.Society;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FinanceService — business logic for society income/expense tracking.
 * UC12: Budget & Finance Tracking
 */
public class FinanceService {

    // In-memory finance store
    private static final List<FinanceEntry> entries = new ArrayList<>();

    public FinanceEntry logIncome(Society society, double amount, String description) {
        FinanceEntry entry = new FinanceEntry(society, EntryType.INCOME, amount, description);
        entries.add(entry);
        return entry;
    }

    public FinanceEntry logExpense(Society society, double amount, String description) {
        FinanceEntry entry = new FinanceEntry(society, EntryType.EXPENSE, amount, description);
        entries.add(entry);
        return entry;
    }

    public List<FinanceEntry> getEntriesForSociety(String societyId) {
        return entries.stream()
            .filter(e -> e.getSociety().getSocietyId().equals(societyId))
            .collect(Collectors.toList());
    }

    public double getBalance(String societyId) {
        double income  = entries.stream()
            .filter(e -> e.getSociety().getSocietyId().equals(societyId) && e.getType() == EntryType.INCOME)
            .mapToDouble(FinanceEntry::getAmount).sum();
        double expense = entries.stream()
            .filter(e -> e.getSociety().getSocietyId().equals(societyId) && e.getType() == EntryType.EXPENSE)
            .mapToDouble(FinanceEntry::getAmount).sum();
        return income - expense;
    }

    public List<FinanceEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }
}
