package com.clubsportif.model;

import java.time.LocalDate;

public class Member {
    private int id;
    private int userId;                // NEW
    private String nom;
    private String prenom;
    private String subscription; // "1 month", "3 months", "1 year"
    private LocalDate dateStart;    // NEW - when membership started
    private LocalDate dateEnd;
    private String status; // ACTIVE, EXPIRED

    // Full constructor
    public Member(int id, int userId, String nom, String prenom, String subscription,
                  LocalDate dateStart, LocalDate dateEnd, String status) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.subscription = subscription;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.status = status;
    }

    // Convenience constructor (when creating a new member)
    public Member(int userId, String nom, String prenom, String subscription, LocalDate dateStart, LocalDate dateEnd) {
        this(0, userId, nom, prenom, subscription, dateStart, dateEnd, calculateStatus(dateEnd));
    }

    // Backwards-compatible convenience constructor (no userId)
    public Member(String nom, String prenom, String subscription, LocalDate dateStart, LocalDate dateEnd) {
        this(0, 0, nom, prenom, subscription, dateStart, dateEnd, calculateStatus(dateEnd));
    }

    private static String calculateStatus(LocalDate dateEnd) {
        return LocalDate.now().isAfter(dateEnd) ? "EXPIRED" : "ACTIVE";
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getSubscription() { return subscription; }
    public LocalDate getDateStart() { return dateStart; }
    public LocalDate getDateEnd() { return dateEnd; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setSubscription(String subscription) { this.subscription = subscription; }
    public void setDateStart(LocalDate dateStart) {
        this.dateStart = dateStart;
    }
    public void setDateEnd(LocalDate dateEnd) {
        this.dateEnd = dateEnd;
        this.status = calculateStatus(dateEnd);
    }
    public void setStatus(String status) { this.status = status; }
}
