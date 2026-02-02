package com.example.services;

import com.example.data.BookingResult;
import com.example.data.HarmonogramDTO;
import com.example.data.UserDTO;
import com.example.security.UserValidator;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReceptionService {

    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }

    public List<UserDTO> getAllDoctors() throws SQLException {
        List<UserDTO> doctors = new ArrayList<>();
        String query = "SELECT u.*, s.specjalizacja " +
                "FROM Uzytkownik u " +
                "JOIN Lekarz l ON u.id_uzytkownika = l.id_uzytkownika " +
                "LEFT JOIN Specjalizacja s ON l.id_specjalizacji = s.id_specjalizacji " +
                "WHERE u.Rola = 'Lekarz' AND u.Czy_aktywny = true";

        try (Connection conn = DatabaseConnectionService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("Id_uzytkownika");
                doctors.add(new UserDTO(
                        id,
                        rs.getString("Imie"),
                        rs.getString("Nazwisko"),
                        rs.getString("Login"),
                        rs.getString("Rola"),
                        rs.getString("Email"),
                        rs.getString("Numer_telefonu"),
                        rs.getBoolean("Czy_aktywny"),
                        id,
                        rs.getString("specjalizacja"),
                        null
                ));
            }
        }
        return doctors;
    }

    public List<String> getAllSpecializations() throws SQLException {
        List<String> specs = new ArrayList<>();
        String query = "SELECT specjalizacja FROM Specjalizacja";
        try (Connection conn = DatabaseConnectionService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                specs.add(rs.getString("specjalizacja"));
            }
        }
        return specs;
    }

    public UserDTO getDoctorById(int id) throws SQLException {
        String query = "SELECT u.*, s.specjalizacja " +
                "FROM Uzytkownik u " +
                "JOIN Lekarz l ON u.id_uzytkownika = l.id_uzytkownika " +
                "LEFT JOIN Specjalizacja s ON l.id_specjalizacji = s.id_specjalizacji " +
                "WHERE u.Id_uzytkownika = ?";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserDTO(
                            rs.getInt("Id_uzytkownika"),
                            rs.getString("Imie"),
                            rs.getString("Nazwisko"),
                            rs.getString("Login"),
                            rs.getString("Rola"),
                            rs.getString("Email"),
                            rs.getString("Numer_telefonu"),
                            rs.getBoolean("Czy_aktywny"),
                            rs.getInt("Id_uzytkownika"),
                            rs.getString("specjalizacja"),
                            null
                    );
                }
            }
        }
        return null;
    }

    public List<HarmonogramDTO> getScheduleForDoctor(int doctorId, LocalDate date) throws SQLException {
        Map<Integer, HarmonogramDTO> scheduleMap = new LinkedHashMap<>();

        String query =
                "SELECT t.id_terminu, t.data, t.godzina, " +
                        "       r.status_rezerwacji, " +
                        "       u.imie, u.nazwisko, w.opis_powodu, " +
                        "       w.id_rezerwacji AS wizyta_id, " +
                        "       l.Start_pracy, l.Koniec_pracy " +
                        "FROM Termin t " +
                        "JOIN Lekarz l ON t.id_lekarza = l.id_uzytkownika " +
                        "LEFT JOIN Rezerwacja r ON t.id_terminu = r.id_terminu AND r.status_rezerwacji != 'Anulowana' " +
                        "LEFT JOIN Pacjent p ON r.id_pacjenta = p.id_uzytkownika " +
                        "LEFT JOIN Uzytkownik u ON p.id_uzytkownika = u.id_uzytkownika " +
                        "LEFT JOIN Wizyta w ON r.id_rezerwacji = w.id_rezerwacji " +
                        "WHERE t.id_lekarza = ? AND t.data = ? " +
                        "ORDER BY t.godzina";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int terminId = rs.getInt("id_terminu");
                    LocalTime termTime = rs.getTime("godzina").toLocalTime();
                    LocalTime startWork = rs.getTime("Start_pracy") != null ? rs.getTime("Start_pracy").toLocalTime() : LocalTime.MIN;
                    LocalTime endWork = rs.getTime("Koniec_pracy") != null ? rs.getTime("Koniec_pracy").toLocalTime() : LocalTime.MAX;

                    String statusRezerwacji = rs.getString("status_rezerwacji");
                    boolean wizytaIstnieje = rs.getObject("wizyta_id") != null;
                    boolean isOutsideWorkingHours = termTime.isBefore(startWork) || !termTime.isBefore(endWork);

                    if (isOutsideWorkingHours && !wizytaIstnieje) {
                        continue;
                    }

                    String displayStatus = "Wolny";
                    String imie = null;
                    String nazwisko = null;
                    String powod = null;
                    int idRezerwacji = 0;

                    if (wizytaIstnieje) {
                        displayStatus = statusRezerwacji;
                        imie = rs.getString("imie");
                        nazwisko = rs.getString("nazwisko");
                        powod = rs.getString("opis_powodu");
                        idRezerwacji = rs.getInt("wizyta_id");
                    }

                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setIdTerminu(terminId);
                    dto.setStatus(displayStatus);
                    dto.setData(rs.getDate("data").toLocalDate());
                    dto.setGodzina(termTime);
                    dto.setImiePacjenta(imie);
                    dto.setNazwiskoPacjenta(nazwisko);
                    dto.setPowodWizyty(powod);
                    dto.setIdRezerwacji(idRezerwacji);

                    if (!scheduleMap.containsKey(terminId)) {
                        scheduleMap.put(terminId, dto);
                    } else {
                        if (wizytaIstnieje) {
                            scheduleMap.put(terminId, dto);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(scheduleMap.values());
    }

    public List<HarmonogramDTO> getPendingReservations() throws SQLException {
        List<HarmonogramDTO> list = new ArrayList<>();
        String query =
                "SELECT r.ID_Rezerwacji, t.ID_Terminu, t.Data, t.Godzina, u_pac.Imie, u_pac.Nazwisko, " +
                        "       u_lek.Imie AS LekarzImie, u_lek.Nazwisko AS LekarzNazwisko, s.Specjalizacja " +
                        "FROM Rezerwacja r " +
                        "JOIN Termin t ON r.ID_Terminu = t.ID_Terminu " +
                        "JOIN Pacjent p ON r.ID_Pacjenta = p.ID_Uzytkownika " +
                        "JOIN Uzytkownik u_pac ON p.ID_Uzytkownika = u_pac.ID_Uzytkownika " +
                        "JOIN Lekarz l ON t.ID_Lekarza = l.ID_Uzytkownika " +
                        "JOIN Uzytkownik u_lek ON l.ID_Uzytkownika = u_lek.ID_Uzytkownika " +
                        "LEFT JOIN Specjalizacja s ON l.ID_Specjalizacji = s.ID_Specjalizacji " +
                        "WHERE r.Status_rezerwacji = 'Wymaga potwierdzenia przez rejestracje' " +
                        "ORDER BY t.Data, t.Godzina";

        try (Connection conn = DatabaseConnectionService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String lekarzInfo = rs.getString("LekarzImie") + " " + rs.getString("LekarzNazwisko") +
                        (rs.getString("Specjalizacja") != null ? " (" + rs.getString("Specjalizacja") + ")" : "");

                HarmonogramDTO dto = new HarmonogramDTO();
                dto.setIdTerminu(rs.getInt("ID_Terminu"));
                dto.setStatus("Wymaga potwierdzenia przez rejestracje");
                dto.setData(rs.getDate("Data").toLocalDate());
                dto.setGodzina(rs.getTime("Godzina").toLocalTime());
                dto.setImiePacjenta(rs.getString("Imie"));
                dto.setNazwiskoPacjenta(rs.getString("Nazwisko"));
                dto.setIdRezerwacji(rs.getInt("ID_Rezerwacji"));
                dto.setLekarz(lekarzInfo);

                list.add(dto);
            }
        }
        return list;
    }

    public List<UserDTO> getAllPatients() throws SQLException {
        List<UserDTO> patients = new ArrayList<>();
        String query = "SELECT u.*, p.PESEL, p.Adres FROM Uzytkownik u " +
                "JOIN Pacjent p ON u.Id_uzytkownika = p.Id_uzytkownika " +
                "WHERE u.Rola = 'Pacjent'";
        try (Connection conn = DatabaseConnectionService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                patients.add(new UserDTO(
                        rs.getInt("Id_uzytkownika"),
                        rs.getString("Imie"),
                        rs.getString("Nazwisko"),
                        rs.getString("Login"),
                        "Pacjent",
                        rs.getString("Email"),
                        rs.getString("Numer_telefonu"),
                        rs.getBoolean("Czy_aktywny"),
                        null,
                        null,
                        rs.getString("PESEL")
                ));
            }
        }
        return patients;
    }

    public Map<Integer, String> getVisitReasons(int doctorId) throws SQLException {
        Map<Integer, String> reasons = new LinkedHashMap<>();
        String query = "SELECT pw.ID_PowodWizyty, pw.Powod_wizyty " +
                "FROM PowodWizyty pw " +
                "JOIN Lekarz l ON pw.ID_Specjalizacji = l.ID_Specjalizacji " +
                "WHERE l.id_uzytkownika = ?";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reasons.put(rs.getInt("ID_PowodWizyty"), rs.getString("Powod_wizyty"));
                }
            }
        }
        reasons.put(-1, "Inny (wpisz własny)");
        return reasons;
    }

    public BookingResult bookAppointment(int terminId, Integer existingPatientId,
                                         String newName, String newSurname, String newPesel,
                                         String newPhone, String newEmail, String newAddress,
                                         int reasonId, String visitReasonNote,
                                         String initialStatus)
            throws SQLException, ValidationException {

        Connection conn = null;
        String generatedLogin = null;
        String plainPassword = null;

        try {
            conn = DatabaseConnectionService.getConnection();

            if (existingPatientId == null) {
                UserValidator.checkUniqueness(conn, null, newEmail, newPhone, newPesel);
                if (newName == null || newName.trim().isEmpty()) throw new ValidationException("Imię jest wymagane.");
                if (newSurname == null || newSurname.trim().isEmpty()) throw new ValidationException("Nazwisko jest wymagane.");
            }

            conn.setAutoCommit(false);
            int finalPatientId;

            if (existingPatientId == null) {
                generatedLogin = (newName.substring(0, 1) + newSurname + newPesel.substring(0, 3)).toLowerCase();
                if (UserValidator.exists(conn, "Uzytkownik", "Login", generatedLogin)) {
                    generatedLogin += "1";
                }
                plainPassword = "Start" + newPesel.substring(0, 4);
                String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

                String insertUser = "INSERT INTO Uzytkownik (Imie, Nazwisko, Login, Haslo, Numer_telefonu, Email, Rola, Czy_aktywny) VALUES (?, ?, ?, ?, ?, ?, 'Pacjent', true) RETURNING Id_uzytkownika";
                try (PreparedStatement stmtUser = conn.prepareStatement(insertUser)) {
                    stmtUser.setString(1, newName); stmtUser.setString(2, newSurname);
                    stmtUser.setString(3, generatedLogin); stmtUser.setString(4, hashedPassword);
                    stmtUser.setString(5, newPhone); stmtUser.setString(6, newEmail);
                    try (ResultSet rs = stmtUser.executeQuery()) {
                        if (rs.next()) finalPatientId = rs.getInt(1);
                        else throw new SQLException("Błąd tworzenia użytkownika");
                    }
                }
                String insertPatient = "INSERT INTO Pacjent (ID_Uzytkownika, PESEL, Adres) VALUES (?, ?, ?)";
                try (PreparedStatement stmtPat = conn.prepareStatement(insertPatient)) {
                    stmtPat.setInt(1, finalPatientId); stmtPat.setString(2, newPesel); stmtPat.setString(3, newAddress);
                    stmtPat.executeUpdate();
                }
            } else {
                finalPatientId = existingPatientId;
            }

            String insertReserv = "INSERT INTO Rezerwacja (ID_Terminu, ID_Pacjenta, Status_rezerwacji) VALUES (?, ?, ?) RETURNING ID_Rezerwacji";
            int idRezerwacji = -1;
            try (PreparedStatement stmtRes = conn.prepareStatement(insertReserv)) {
                stmtRes.setInt(1, terminId);
                stmtRes.setInt(2, finalPatientId);
                stmtRes.setString(3, initialStatus);
                try (ResultSet rs = stmtRes.executeQuery()) {
                    if (rs.next()) idRezerwacji = rs.getInt(1);
                    else throw new SQLException("Błąd tworzenia rezerwacji.");
                }
            }

            String insertVisit = "INSERT INTO Wizyta (ID_Rezerwacji, ID_PowodWizyty, Opis_Powodu, Notatka) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmtVis = conn.prepareStatement(insertVisit)) {
                stmtVis.setInt(1, idRezerwacji);
                if (reasonId == -1) stmtVis.setNull(2, Types.INTEGER);
                else stmtVis.setInt(2, reasonId);
                stmtVis.setString(3, visitReasonNote);
                stmtVis.setNull(4, Types.VARCHAR);
                stmtVis.executeUpdate();
            }

            conn.commit();
            return new BookingResult(true, generatedLogin, plainPassword);

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        }
    }

    public HarmonogramDTO getAppointmentDetails(int terminId) throws SQLException {
        String query = "SELECT r.ID_Rezerwacji, u.Imie, u.Nazwisko, u.Numer_telefonu, u.Email, p.PESEL, r.Status_rezerwacji, w.Opis_Powodu " +
                "FROM Rezerwacja r " +
                "JOIN Pacjent p ON r.ID_Pacjenta = p.ID_uzytkownika " +
                "JOIN Uzytkownik u ON p.ID_Uzytkownika = u.Id_uzytkownika " +
                "LEFT JOIN Wizyta w ON r.ID_Rezerwacji = w.ID_Rezerwacji " +
                "WHERE r.ID_Terminu = ? AND r.Status_rezerwacji != 'Anulowana'";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, terminId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setIdRezerwacji(rs.getInt("ID_Rezerwacji"));
                    dto.setImiePacjenta(rs.getString("Imie"));
                    dto.setNazwiskoPacjenta(rs.getString("Nazwisko"));
                    dto.setPesel(rs.getString("PESEL"));
                    dto.setTelefon(rs.getString("Numer_telefonu"));
                    dto.setEmail(rs.getString("Email"));
                    dto.setStatus(rs.getString("Status_rezerwacji"));
                    dto.setPowodWizyty(rs.getString("Opis_Powodu"));

                    return dto;
                }
            }
        }
        return null;
    }

    public void rescheduleAppointment(int reservationId, int newTerminId) throws SQLException, ValidationException {
        Connection conn = null;
        try {
            conn = DatabaseConnectionService.getConnection();
            conn.setAutoCommit(false);

            String checkQuery = "SELECT COUNT(*) FROM Rezerwacja WHERE ID_Terminu = ?";
            try (PreparedStatement chkStmt = conn.prepareStatement(checkQuery)) {
                chkStmt.setInt(1, newTerminId);
                try (ResultSet rs = chkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ValidationException("Wybrany termin jest już zajęty! Odśwież widok.");
                    }
                }
            }

            String updateQuery = "UPDATE Rezerwacja SET ID_Terminu = ? WHERE ID_Rezerwacji = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setInt(1, newTerminId);
                stmt.setInt(2, reservationId);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    throw new SQLException("Nie znaleziono rezerwacji do aktualizacji.");
                }
            }

            updateAppointmentStatus(newTerminId, "Wymaga potwierdzenia przez pacjenta");

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        }
    }
    public List<HarmonogramDTO> getPatientReservations(int patientId) throws SQLException {
        List<HarmonogramDTO> list = new ArrayList<>();

        String query = "SELECT r.ID_Rezerwacji, r.Status_rezerwacji, t.ID_Terminu, t.Data, t.Godzina, " +
                "       l_u.Imie AS LekarzImie, l_u.Nazwisko AS LekarzNazwisko, s.Specjalizacja, " +
                "       w.Opis_Powodu, pw.Powod_wizyty " +
                "FROM Rezerwacja r " +
                "JOIN Termin t ON r.ID_Terminu = t.ID_Terminu " +
                "JOIN Lekarz l ON t.ID_Lekarza = l.ID_Uzytkownika " +
                "JOIN Uzytkownik l_u ON l.ID_Uzytkownika = l_u.ID_Uzytkownika " +
                "LEFT JOIN Specjalizacja s ON l.ID_Specjalizacji = s.ID_Specjalizacji " +
                "LEFT JOIN Wizyta w ON r.ID_Rezerwacji = w.ID_Rezerwacji " +
                "LEFT JOIN PowodWizyty pw ON w.ID_PowodWizyty = pw.ID_PowodWizyty " +
                "WHERE r.ID_Pacjenta = ? " +
                "ORDER BY t.Data DESC, t.Godzina DESC";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setIdTerminu(rs.getInt("ID_Terminu"));
                    dto.setStatus(rs.getString("Status_rezerwacji"));
                    dto.setData(rs.getDate("Data").toLocalDate());
                    dto.setGodzina(rs.getTime("Godzina").toLocalTime());


                    String lekarzInfo = rs.getString("LekarzImie") + " " + rs.getString("LekarzNazwisko") +
                            (rs.getString("Specjalizacja") != null ? " (" + rs.getString("Specjalizacja") + ")" : "");
                    dto.setLekarz(lekarzInfo);


                    String standardReason = rs.getString("Powod_wizyty");
                    String customReason = rs.getString("Opis_Powodu");


                    if (standardReason != null) {
                        dto.setPowodWizyty(standardReason);
                    } else {
                        dto.setPowodWizyty(customReason);
                    }

                    list.add(dto);
                }
            }
        }
        return list;
    }

    public void updateAppointmentStatus(int terminId, String newStatus) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnectionService.getConnection();
            conn.setAutoCommit(false);

            String updateRes = "UPDATE Rezerwacja SET Status_rezerwacji = ? WHERE ID_Terminu = ? AND Status_rezerwacji != 'Anulowana'";
            try (PreparedStatement stmt = conn.prepareStatement(updateRes)) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, terminId);
                stmt.executeUpdate();
            }

            if ("Anulowana".equals(newStatus)) {
                String deleteVisit = "DELETE FROM Wizyta WHERE ID_Rezerwacji IN " +
                        "(SELECT ID_Rezerwacji FROM Rezerwacja WHERE ID_Terminu = ? AND Status_rezerwacji = 'Anulowana')";
                try (PreparedStatement stmtDel = conn.prepareStatement(deleteVisit)) {
                    stmtDel.setInt(1, terminId);
                    stmtDel.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        }
    }
}