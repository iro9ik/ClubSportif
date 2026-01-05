package com.clubsportif.model;

import java.time.LocalDate;

public class Member {
    private int id;
    private String nom;
    private String prenom;
    private String subscription; // "1 month", "3 months", "1 year"
    private LocalDate dateEnd;
    private String status; // ACTIVE, EXPIRED

    public Member(int id, String nom, String prenom, String subscription, 
                  LocalDate dateEnd, String status) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.subscription = subscription;
        this.dateEnd = dateEnd;
        this.status = status;
    }

    public Member(String nom, String prenom, String subscription, LocalDate dateEnd) {
        this(0, nom, prenom, subscription, dateEnd, calculateStatus(dateEnd));
    }

    private static String calculateStatus(LocalDate dateEnd) {
        return LocalDate.now().isAfter(dateEnd) ? "EXPIRED" : "ACTIVE";
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getSubscription() { return subscription; }
    public LocalDate getDateEnd() { return dateEnd; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSubscription(String subscription) { this.subscription = subscription; }
    public void setDateEnd(LocalDate dateEnd) { 
        this.dateEnd = dateEnd;
        this.status = calculateStatus(dateEnd);
    }
    public void setStatus(String status) { this.status = status; }
}
