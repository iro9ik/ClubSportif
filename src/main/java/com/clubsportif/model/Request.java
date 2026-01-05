package com.clubsportif.model;

import java.time.LocalDate;

public class Request {
    private int id;
    private int userId;
    private String nom;
    private String prenom;
    private String subscription; // "1 month", "3 months", "1 year"
    private LocalDate requestDate;
    private String status; // PENDING, ACCEPTED, DECLINED, CANCELED

    public Request(int id, int userId, String nom, String prenom, String subscription, 
                   LocalDate requestDate, String status) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.subscription = subscription;
        this.requestDate = requestDate;
        this.status = status;
    }

    public Request(int userId, String nom, String prenom, String subscription) {
        this(0, userId, nom, prenom, subscription, LocalDate.now(), "PENDING");
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getSubscription() { return subscription; }
    public LocalDate getRequestDate() { return requestDate; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
}
