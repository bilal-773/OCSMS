package com.ocsms.repository;

import com.ocsms.model.BudgetAllocation;
import com.ocsms.model.BudgetBill;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BudgetRepository — in-memory store for BudgetAllocations and BudgetBills.
 * Singleton so all controllers share the same data in one session.
 */
public class BudgetRepository {

    private static final BudgetRepository INSTANCE = new BudgetRepository();
    public  static BudgetRepository getInstance()  { return INSTANCE; }

    private final Map<String, BudgetAllocation> allocations = new LinkedHashMap<>();
    private final Map<String, BudgetBill>       bills       = new LinkedHashMap<>();

    private BudgetRepository() {}

    // ── Allocations ────────────────────────────────────────────────────────────

    public void saveAllocation(BudgetAllocation a) {
        allocations.put(a.getAllocationId(), a);
    }

    public List<BudgetAllocation> findAll() {
        return new ArrayList<>(allocations.values());
    }

    public List<BudgetAllocation> findBySociety(String societyId) {
        return allocations.values().stream()
            .filter(a -> a.getSociety() != null &&
                         a.getSociety().getSocietyId().equals(societyId))
            .collect(Collectors.toList());
    }

    public Optional<BudgetAllocation> findById(String id) {
        return Optional.ofNullable(allocations.get(id));
    }

    public void deleteAllocation(String id) {
        allocations.remove(id);
    }

    // ── Bills ──────────────────────────────────────────────────────────────────

    public void saveBill(BudgetBill bill) {
        bills.put(bill.getBillId(), bill);
        // Also add to parent allocation
        allocations.values().stream()
            .filter(a -> a.getAllocationId().equals(bill.getAllocationId()))
            .findFirst()
            .ifPresent(a -> {
                boolean exists = a.getBills().stream()
                    .anyMatch(b -> b.getBillId().equals(bill.getBillId()));
                if (!exists) a.addBill(bill);
            });
    }

    public List<BudgetBill> findBillsByAllocation(String allocationId) {
        return bills.values().stream()
            .filter(b -> b.getAllocationId().equals(allocationId))
            .collect(Collectors.toList());
    }

    public List<BudgetBill> findBillsBySociety(String societyId) {
        Set<String> allocIds = allocations.values().stream()
            .filter(a -> a.getSociety() != null &&
                         a.getSociety().getSocietyId().equals(societyId))
            .map(BudgetAllocation::getAllocationId)
            .collect(Collectors.toSet());
        return bills.values().stream()
            .filter(b -> allocIds.contains(b.getAllocationId()))
            .collect(Collectors.toList());
    }
}
