package com.clubsportif.config;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeTables() {
        createMembersTable();
        createRequestsTable();
        System.out.println("Database tables initialized successfully.");
    }

    private static void createMembersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS members (" +
                     "id SERIAL PRIMARY KEY, " +
                     "nom VARCHAR(100) NOT NULL, " +
                     "prenom VARCHAR(100) NOT NULL, " +
                     "subscription VARCHAR(50) NOT NULL, " +
                     "date_end DATE NOT NULL, " +
                     "status VARCHAR(20) NOT NULL" +
                     ")";
        
        executeSQL(sql);
    }

    private static void createRequestsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS requests (" +
                     "id SERIAL PRIMARY KEY, " +
                     "user_id INTEGER NOT NULL, " +
                     "nom VARCHAR(100) NOT NULL, " +
                     "prenom VARCHAR(100) NOT NULL, " +
                     "subscription VARCHAR(50) NOT NULL, " +
                     "request_date DATE NOT NULL, " +
                     "status VARCHAR(20) NOT NULL" +
                     ")";
        
        executeSQL(sql);
    }

    private static void executeSQL(String sql) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
