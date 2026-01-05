package com.clubsportif.model;

public class Admin extends User {

    public Admin(int id, String username, String email,
                 String phone, String password) {

        super(id, username, email, phone, password, "ADMIN");
    }
 
    public void promoteToMember(User user) {
        user.setRole("MEMBER");
    }

    public void demoteToVisitor(User user) {
        user.setRole("VISITOR");
    }
}
