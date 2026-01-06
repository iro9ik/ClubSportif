package com.clubsportif.dao;

import com.clubsportif.config.Database;
import com.clubsportif.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    // Create a new member
    public void createMember(Member member) {
        String sql = "INSERT INTO members (user_id, nom, prenom, subscription, date_start, date_end, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, member.getUserId());
            stmt.setString(2, member.getNom());
            stmt.setString(3, member.getPrenom());
            stmt.setString(4, member.getSubscription());
            stmt.setDate(5, Date.valueOf(member.getDateStart()));
            stmt.setDate(6, Date.valueOf(member.getDateEnd()));
            stmt.setString(7, member.getStatus());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all members
    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Member member = new Member(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("subscription"),
                    rs.getDate("date_start").toLocalDate(),
                    rs.getDate("date_end").toLocalDate(),
                    rs.getString("status")
                );
                members.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return members;
    }

    // Get member by ID
    public Member getMemberById(int id) {
        String sql = "SELECT * FROM members WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Member(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("subscription"),
                    rs.getDate("date_start").toLocalDate(),
                    rs.getDate("date_end").toLocalDate(),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Update member
    public void updateMember(Member member) {
        String sql = "UPDATE members SET user_id = ?, nom = ?, prenom = ?, subscription = ?, date_start = ?, date_end = ?, status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, member.getUserId());
            stmt.setString(2, member.getNom());
            stmt.setString(3, member.getPrenom());
            stmt.setString(4, member.getSubscription());
            stmt.setDate(5, Date.valueOf(member.getDateStart()));
            stmt.setDate(6, Date.valueOf(member.getDateEnd()));
            stmt.setString(7, member.getStatus());
            stmt.setInt(8, member.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete member
    public void deleteMember(int id) {
        String sql = "DELETE FROM members WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update member status based on date_end
    public void updateMemberStatuses() {
        String sql = "UPDATE members SET status = CASE WHEN date_end < CURRENT_DATE THEN 'EXPIRED' ELSE 'ACTIVE' END";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get member by user ID
    public Member getMemberByUserId(int userId) {
        String sql = "SELECT * FROM members WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Member(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("subscription"),
                    rs.getDate("date_start").toLocalDate(),
                    rs.getDate("date_end").toLocalDate(),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
