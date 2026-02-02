package com.example.services;

import com.example.security.UserValidator;
import org.mindrot.jbcrypt.BCrypt;
import com.example.data.UserDTO;
import com.example.data.SpecjalizacjaDTO;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AdminService {

    public void registerDoctor(String imie, String nazwisko, String login, String haslo,
                               String email, String telefon,
                               LocalTime startPracy, LocalTime koniecPracy, int idSpecjalizacji) throws SQLException {

        Connection conn = null;
        try {
            conn = DatabaseConnectionService.getConnection();

            UserValidator.checkUniqueness(conn, login, email, telefon, null);

            conn.setAutoCommit(false);

            String hashedPassword = BCrypt.hashpw(haslo, BCrypt.gensalt());

            String insertUserSql = "INSERT INTO Uzytkownik (Imie, Nazwisko, Login, Haslo, Rola, Email, Numer_telefonu, Czy_aktywny) VALUES (?, ?, ?, ?, 'Lekarz', ?, ?, TRUE)";

            int newUserId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, imie);
                stmt.setString(2, nazwisko);
                stmt.setString(3, login);
                stmt.setString(4, hashedPassword);
                stmt.setString(5, email);
                stmt.setString(6, telefon);
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) newUserId = rs.getInt(1);
            }

            String insertDoctorSql = "INSERT INTO Lekarz (ID_Uzytkownika, Start_pracy, Koniec_pracy, ID_Specjalizacji) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertDoctorSql)) {
                stmt.setInt(1, newUserId);
                stmt.setTime(2, Time.valueOf(startPracy));
                stmt.setTime(3, Time.valueOf(koniecPracy));
                stmt.setInt(4, idSpecjalizacji);
                stmt.executeUpdate();
            }

            generateSlotsForDoctor(conn, newUserId, startPracy, koniecPracy, LocalDate.now(), LocalDate.now().plusMonths(3));

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    public void registerReceptionist(String imie, String nazwisko, String login, String haslo,
                                     String email, String telefon) throws SQLException {

        try (Connection conn = DatabaseConnectionService.getConnection()) {

            UserValidator.checkUniqueness(conn, login, email, telefon, null);
            conn.setAutoCommit(false);
            String hashedPassword = BCrypt.hashpw(haslo, BCrypt.gensalt());

            String sqlUser = "INSERT INTO Uzytkownik (Imie, Nazwisko, Login, Haslo, Rola, Email, Numer_telefonu, Czy_aktywny) VALUES (?, ?, ?, ?, 'Rejestracja', ?, ?, TRUE)";
            String sqlRecep = "INSERT INTO PracownikRejestracji (ID_Uzytkownika) VALUES (?)";

            int newUserId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, imie); stmt.setString(2, nazwisko); stmt.setString(3, login);
                stmt.setString(4, hashedPassword); stmt.setString(5, email); stmt.setString(6, telefon);
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) newUserId = rs.getInt(1);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

            if (newUserId != -1) {
                try (PreparedStatement stmt = conn.prepareStatement(sqlRecep)) {
                    stmt.setInt(1, newUserId);
                    stmt.executeUpdate();
                }
                conn.commit();
            } else {
                conn.rollback();
            }
        }
    }

    private void checkUniqueness(Connection conn, String login, String email, String telefon, String pesel) throws SQLException {

        if (exists(conn, "Uzytkownik", "Login", login))
            throw new SQLException("LOGIN_ZAJETY");

        if (exists(conn, "Uzytkownik", "Email", email))
            throw new SQLException("Podany Email jest już przypisany do innego konta.");

        if (telefon != null && !telefon.matches("^[+]?[0-9]{9,15}$")) {
            throw new SQLException("Numer telefonu musi mieć od 9 do 15 cyfr (opcjonalnie z '+').");
        }

        if (exists(conn, "Uzytkownik", "Numer_telefonu", telefon))
            throw new SQLException("Podany numer telefonu istnieje już w systemie.");

        if (pesel != null && !pesel.isEmpty()) {
            if (exists(conn, "Pacjent", "PESEL", pesel))
                throw new SQLException("Pacjent o podanym numerze PESEL jest już zarejestrowany.");
        }
    }

    private boolean exists(Connection conn, String table, String column, String value) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }


    public List<SpecjalizacjaDTO> getAllSpecializations() {
        List<SpecjalizacjaDTO> list = new ArrayList<>();
        String sql = "SELECT ID_Specjalizacji, Specjalizacja FROM Specjalizacja ORDER BY Specjalizacja";
        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new SpecjalizacjaDTO(rs.getInt("ID_Specjalizacji"), rs.getString("Specjalizacja")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void toggleUserStatus(int userId, boolean isActive) throws SQLException {
        String sql = "UPDATE Uzytkownik SET Czy_aktywny = ? WHERE ID_Uzytkownika = ?";
        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void updateDoctorHours(int doctorIdIsUserId, LocalTime newStart, LocalTime newEnd) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnectionService.getConnection();
            conn.setAutoCommit(false);

            String updateDoctorSql = "UPDATE Lekarz SET Start_pracy = ?, Koniec_pracy = ? WHERE ID_Uzytkownika = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateDoctorSql)) {
                stmt.setTime(1, Time.valueOf(newStart));
                stmt.setTime(2, Time.valueOf(newEnd));
                stmt.setInt(3, doctorIdIsUserId);
                stmt.executeUpdate();
            }

            String deleteSql = "DELETE FROM Termin " +
                    "WHERE ID_Lekarza = ? " +
                    "AND Data >= CURRENT_DATE " +
                    "AND (Godzina < ? OR Godzina >= ?) " +
                    "AND ID_Terminu NOT IN (SELECT ID_Terminu FROM Rezerwacja)";

            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, doctorIdIsUserId);
                stmt.setTime(2, Time.valueOf(newStart));
                stmt.setTime(3, Time.valueOf(newEnd));
                stmt.executeUpdate();
            }

            generateSlotsForDoctor(conn, doctorIdIsUserId, newStart, newEnd, LocalDate.now(), LocalDate.now().plusMonths(3));

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    private void generateSlotsForDoctor(Connection conn, int userId, LocalTime start, LocalTime end, LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        String checkSql = "SELECT 1 FROM Termin WHERE ID_Lekarza = ? AND Data = ? AND Godzina = ?";
        String insertSql = "INSERT INTO Termin (ID_Lekarza, Data, Godzina) VALUES (?, ?, ?)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            LocalDate currentDate = dateFrom;
            while (currentDate.isBefore(dateTo)) {
                DayOfWeek dzien = currentDate.getDayOfWeek();
                if (dzien == DayOfWeek.SATURDAY || dzien == DayOfWeek.SUNDAY) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                LocalTime currentTime = start;
                while (currentTime.isBefore(end)) {
                    checkStmt.setInt(1, userId);
                    checkStmt.setDate(2, Date.valueOf(currentDate));
                    checkStmt.setTime(3, Time.valueOf(currentTime));

                    ResultSet rs = checkStmt.executeQuery();
                    if (!rs.next()) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setDate(2, Date.valueOf(currentDate));
                        insertStmt.setTime(3, Time.valueOf(currentTime));
                        insertStmt.addBatch();
                    }
                    currentTime = currentTime.plusMinutes(15);
                }
                currentDate = currentDate.plusDays(1);
            }
            insertStmt.executeBatch();
        }
    }

    public List<UserDTO> getAllUsers() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT ID_Uzytkownika, Imie, Nazwisko, Login, Rola, Email, Numer_telefonu, Czy_aktywny " +
                "FROM Uzytkownik ORDER BY Nazwisko";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idUser = rs.getInt("ID_Uzytkownika");
                String rola = rs.getString("Rola");
                Integer idLekarzaDlaDto = "Lekarz".equals(rola) ? idUser : null;

                users.add(new UserDTO(
                        idUser,
                        rs.getString("Imie"),
                        rs.getString("Nazwisko"),
                        rs.getString("Login"),
                        rola,
                        rs.getString("Email"),
                        rs.getString("Numer_telefonu"),
                        rs.getBoolean("Czy_aktywny"),
                        idLekarzaDlaDto,
                        null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}