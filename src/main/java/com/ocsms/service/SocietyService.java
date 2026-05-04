package com.ocsms.service;

import com.ocsms.model.Society;
import com.ocsms.model.FacultyAdvisor;
import com.ocsms.repository.SocietyRepository;
import com.ocsms.enums.SocietyStatus;

import java.util.List;
import java.util.UUID;

/**
 * SocietyService — business logic for creating, searching, and managing societies.
 */
public class SocietyService {

    private final SocietyRepository societyRepo;

    public SocietyService() {
        this.societyRepo = new SocietyRepository();
    }

    /**
     * Creates a new society.
     * Throws IllegalArgumentException if name is already taken.
     */
    public Society createSociety(String name, String category, String description,
                                  int memberLimit, FacultyAdvisor advisor) {
        if (societyRepo.existsByName(name)) {
            throw new IllegalArgumentException("A society named '" + name + "' already exists.");
        }
        String id = "SOC" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Society society = new Society(id, name, category, description, memberLimit);
        society.setAdvisor(advisor);
        societyRepo.save(society);
        return society;
    }

    public List<Society> getAllSocieties() {
        return societyRepo.findAll();
    }

    public List<Society> searchSocieties(String query) {
        return societyRepo.searchByName(query);
    }

    public List<Society> filterByCategory(String category) {
        return societyRepo.findByCategory(category);
    }

    public void archiveSociety(Society society, String reason) {
        society.archive(reason);
        societyRepo.updateStatus(society.getSocietyId(), com.ocsms.enums.SocietyStatus.ARCHIVED);
    }

    public boolean checkNameUnique(String name) {
        return !societyRepo.existsByName(name);
    }
}
