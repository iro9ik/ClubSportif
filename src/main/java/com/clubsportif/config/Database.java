package com.clubsportif.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    private static final String PROPERTIES_FILE = "/db.properties";

    public static Connection getConnection() {
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream input = Database.class.getResourceAsStream(PROPERTIES_FILE)) {
                if (input == null) {
                    throw new RuntimeException("Sorry, unable to find " + PROPERTIES_FILE);
                }
                props.load(input);
            }
            return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("DB connection failed", e);
        }
    }
}
