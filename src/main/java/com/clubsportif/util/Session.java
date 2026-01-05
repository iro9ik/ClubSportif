package com.clubsportif.util;

import com.clubsportif.model.User;

public class Session {

    private static User currentUser;

    private Session() {
        // prevent instantiation
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }
}
