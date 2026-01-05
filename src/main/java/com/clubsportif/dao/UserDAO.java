package com.clubsportif.dao;

import com.clubsportif.config.Database;
import com.clubsportif.model.User;

import java.sql.*;
import java.util.Optional;

public class UserDAO {

    private Connection getConnection() throws SQLException {
        return Database.getConnection();
    }

    // REGISTER
    public void save(User user) throws SQLException {
        String sql = """
            INSERT INTO users(username, email, phone, password, role)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole());

            ps.executeUpdate();
        }
    }

    // LOGIN
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(
                    new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("password"),
                        rs.getString("role")
                    )
                );
            }
            return Optional.empty();
        }
    }
}
