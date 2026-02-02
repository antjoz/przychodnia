package com.example.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserValidator {

    public static void checkUniqueness(Connection conn, String login, String email, String telefon, String pesel) throws SQLException {

        if (login != null && !login.isEmpty()) {
            if (exists(conn, "Uzytkownik", "Login", login)) {
                throw new SQLException("LOGIN_ZAJETY");
            }
        }

        if (email != null && !email.isEmpty()) {
            if (exists(conn, "Uzytkownik", "Email", email)) {
                throw new SQLException("Podany Email jest już przypisany do innego konta.");
            }
        }

        if (telefon != null && !telefon.isEmpty()) {
            if (!telefon.matches("^[+]?[0-9]{9,15}$")) {
                throw new SQLException("Numer telefonu musi mieć od 9 do 15 cyfr (opcjonalnie z '+').");
            }
            if (exists(conn, "Uzytkownik", "Numer_telefonu", telefon)) {
                throw new SQLException("Podany numer telefonu istnieje już w systemie.");
            }
        }

        if (pesel != null && !pesel.isEmpty()) {
            if (!pesel.matches("\\d{11}")) {
                throw new SQLException("PESEL musi składać się z 11 cyfr.");
            }
            if (exists(conn, "Pacjent", "PESEL", pesel)) {
                throw new SQLException("Pacjent o podanym numerze PESEL jest już zarejestrowany.");
            }
        }
    }

    public static boolean exists(Connection conn, String table, String column, String value) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}