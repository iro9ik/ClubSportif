package com.clubsportif.dao;

import com.clubsportif.config.Database;
import com.clubsportif.model.Request;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // Create a new request
    public void createRequest(Request request) {
        String sql = "INSERT INTO requests (user_id, nom, prenom, subscription, request_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, request.getUserId());
            stmt.setString(2, request.getNom());
            stmt.setString(3, request.getPrenom());
            stmt.setString(4, request.getSubscription());
            stmt.setDate(5, Date.valueOf(request.getRequestDate()));
            stmt.setString(6, request.getStatus());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all requests
    public List<Request> getAllRequests() {
        List<Request> requests = new ArrayList<>();
        String sql = "SELECT * FROM requests ORDER BY request_date DESC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Request request = new Request(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("subscription"),
                    rs.getDate("request_date").toLocalDate(),
                    rs.getString("status")
                );
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return requests;
    }

    // Get requests by user ID
    public List<Request> getRequestsByUserId(int userId) {
        List<Request> requests = new ArrayList<>();
        String sql = "SELECT * FROM requests WHERE user_id = ? ORDER BY request_date DESC";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Request request = new Request(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("subscription"),
                    rs.getDate("request_date").toLocalDate(),
                    rs.getString("status")
                );
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return requests;
    }

    // Get daily requests count
    public int getDailyRequestsCount() {
        String sql = "SELECT COUNT(*) FROM requests WHERE request_date = CURRENT_DATE";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }

    // Update request status
    public void updateRequestStatus(int requestId, String status) {
        String sql = "UPDATE requests SET status = ? WHERE id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setInt(2, requestId);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete request
    public void deleteRequest(int id) {
        String sql = "DELETE FROM requests WHERE id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check if user has active request
    public boolean hasActiveRequest(int userId) {
        String sql = "SELECT COUNT(*) FROM requests WHERE user_id = ? AND status = 'PENDING'";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
}
