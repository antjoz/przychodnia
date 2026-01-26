package com.example.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionService {
    // Program najpierw sprawdza zmienne systemowe, a jak ich nie ma, bierze localhost
    private static final String URL = System.getenv("DB_URL") != null
            ? System.getenv("DB_URL")
            : "jdbc:postgresql://localhost:5432/przychodnia_db";

    private static final String USER = System.getenv("DB_USERNAME") != null
            ? System.getenv("DB_USERNAME")
            : "postgres";

    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null
            ? System.getenv("DB_PASSWORD")
            : "1234";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}