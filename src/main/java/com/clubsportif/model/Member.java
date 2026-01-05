package com.clubsportif.model;

import java.time.LocalDate;

public class Member {
    private int id;
    private String nom;
    private int age;
    private LocalDate dateEntree;
    private double salaire;

    public Member(int id, String nom, int age, LocalDate dateEntree, double salaire) {
        this.id = id;
        this.nom = nom;
        this.age = age;
        this.dateEntree = dateEntree;
        this.salaire = salaire;
    }

    public double getSalaire() {
        return salaire;
    }

    public String getNom() {
        return nom;
    }
}
