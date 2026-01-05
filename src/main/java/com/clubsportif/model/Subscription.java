package com.clubsportif.model;

import java.time.LocalDate;

public class Subscription {
    private int id;
    private int memberId;
    private String planType; // "1 month", "3 months", "1 year"
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // ACTIVE, EXPIRED

    public Subscription(int id, int memberId, String planType, LocalDate startDate, 
                       LocalDate endDate, String status) {
        this.id = id;
        this.memberId = memberId;
        this.planType = planType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Subscription(int memberId, String planType, LocalDate startDate, LocalDate endDate) {
        this(0, memberId, planType, startDate, endDate, calculateStatus(endDate));
    }

    private static String calculateStatus(LocalDate endDate) {
        return LocalDate.now().isAfter(endDate) ? "EXPIRED" : "ACTIVE";
    }

    // Getters
    public int getId() { return id; }
    public int getMemberId() { return memberId; }
    public String getPlanType() { return planType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEndDate(LocalDate endDate) { 
        this.endDate = endDate;
        this.status = calculateStatus(endDate);
    }
    public void setStatus(String status) { this.status = status; }
}
