package com.clubsportif.service;

import com.clubsportif.dao.UserDAO;
import com.clubsportif.model.User;

import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public Optional<User> authenticate(String username, String password) {

        try {
            Optional<User> userOpt = userDAO.findByUsername(username);

            if (userOpt.isEmpty()) {
                return Optional.empty();
            }

            User user = userOpt.get();

            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }

            return Optional.empty();

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }


    public void logout() {
        Session.logout();
    }
}
