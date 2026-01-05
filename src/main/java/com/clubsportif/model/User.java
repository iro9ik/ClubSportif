package com.clubsportif.model;

public class User {

    private int id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String role;

    public User(int id, String username, String email, String phone, String password, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public User(String username, String email, String phone, String password, String role) {
        this(0, username, email, phone, password, role);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
