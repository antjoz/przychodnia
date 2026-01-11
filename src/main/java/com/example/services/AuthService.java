package com.example.services;

import com.example.security.UserSession;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class AuthService {

    private static final String URL = "jdbc:postgresql://localhost:5432/przychodnia_db";
    private static final String USER = "postgres";
    private static final String PASS = "1234";

    // Wyjątek do przekazywania komunikatów błędów do widoku
    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }

    // --- REJESTRACJA ---
    public void registerUser(String imie, String nazwisko, String pesel, String adres,
                             String login, String password, String telefon, String email) throws Exception {

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            // 1. Sprawdzenie unikalności PRZED rozpoczęciem transakcji
            // Dzięki temu użytkownik dostanie ładny komunikat zamiast błędu SQL
            checkUniqueness(conn, login, email, telefon, pesel);

            conn.setAutoCommit(false); // Start transakcji

            try {
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                // Domyślnie pacjent jest aktywny (Czy_aktywny = TRUE)
                String sqlUzytkownik = "INSERT INTO Uzytkownik (Imie, Nazwisko, Login, Haslo, Numer_telefonu, Email, Rola, Czy_aktywny) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'Pacjent', TRUE)";

                String sqlPacjent = "INSERT INTO Pacjent (ID_Uzytkownika, PESEL, Adres) VALUES (?, ?, ?)";

                long generatedId = -1;

                // Wstawianie do Uzytkownik
                try (PreparedStatement stmtUser = conn.prepareStatement(sqlUzytkownik, Statement.RETURN_GENERATED_KEYS)) {
                    stmtUser.setString(1, imie);
                    stmtUser.setString(2, nazwisko);
                    stmtUser.setString(3, login);
                    stmtUser.setString(4, hashedPassword);
                    stmtUser.setString(5, telefon);
                    stmtUser.setString(6, email);

                    int affectedRows = stmtUser.executeUpdate();
                    if (affectedRows == 0) throw new SQLException("Nie udało się utworzyć użytkownika.");

                    try (ResultSet generatedKeys = stmtUser.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            generatedId = generatedKeys.getLong(1);
                        } else {
                            throw new SQLException("Nie udało się pobrać ID użytkownika.");
                        }
                    }
                }

                // Wstawianie do Pacjent
                try (PreparedStatement stmtPatient = conn.prepareStatement(sqlPacjent)) {
                    stmtPatient.setLong(1, generatedId);
                    stmtPatient.setString(2, pesel);
                    stmtPatient.setString(3, adres);
                    stmtPatient.executeUpdate();
                }

                conn.commit(); // Zatwierdzenie zmian

            } catch (SQLException e) {
                conn.rollback(); // Wycofanie zmian w razie błędu
                // Zabezpieczenie na wypadek wyścigu (race condition)
                if ("23505".equals(e.getSQLState())) {
                    throw new ValidationException("Jeden z podanych danych (Login, Email, Telefon lub PESEL) jest już w bazie.");
                }
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Błąd połączenia z bazą danych: " + e.getMessage());
        }
    }

    // --- LOGOWANIE ---
    public UserSession login(String loginInput, String passwordInput) throws ValidationException, Exception {
        String sql = "SELECT ID_Uzytkownika, Imie, Nazwisko, Haslo, Rola, Czy_aktywny FROM Uzytkownik WHERE Login = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, loginInput);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dbHash = rs.getString("Haslo");
                    boolean isActive = rs.getBoolean("Czy_aktywny");

                    if (BCrypt.checkpw(passwordInput, dbHash)) {

                        // SPRAWDZENIE CZY KONTO JEST AKTYWNE
                        if (!isActive) {
                            throw new ValidationException("Twoje konto jest zablokowane lub nieaktywne. Skontaktuj się z placówką.");
                        }

                        return new UserSession(
                                rs.getInt("ID_Uzytkownika"),
                                rs.getString("Imie"),
                                rs.getString("Nazwisko"),
                                rs.getString("Rola")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Błąd systemu logowania.");
        }
        return null; // Niepoprawny login lub hasło
    }

    // --- METODY POMOCNICZE ---

    private void checkUniqueness(Connection conn, String login, String email, String telefon, String pesel) throws SQLException, ValidationException {
        if (exists(conn, "Uzytkownik", "Login", login))
            throw new ValidationException("Podany Login jest już zajęty.");

        if (exists(conn, "Uzytkownik", "Email", email))
            throw new ValidationException("Podany Email jest już przypisany do innego konta.");

        if (exists(conn, "Uzytkownik", "Numer_telefonu", telefon))
            throw new ValidationException("Podany numer telefonu istnieje już w systemie.");

        if (exists(conn, "Pacjent", "PESEL", pesel))
            throw new ValidationException("Pacjent o podanym numerze PESEL jest już zarejestrowany.");
    }

    private boolean exists(Connection conn, String table, String column, String value) throws SQLException {
        // Zapytanie jest bezpieczne, bo nazwy tabel/kolumn są wpisane "na sztywno" w kodzie wyżej
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}