package com.example.services;

import com.example.data.HarmonogramDTO;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DoctorService {

    // 1. Pobierz harmonogram (z uwzględnieniem statusów i danych pacjenta z tabeli Uzytkownik)
    public List<HarmonogramDTO> getSchedule(int doctorId, LocalDate date) throws SQLException {
        List<HarmonogramDTO> list = new ArrayList<>();

        // ZMIANA: Musimy połączyć Pacjenta z Uzytkownikiem, aby dostać Imie i Nazwisko
        String query = """
            SELECT t.ID_Terminu, t.Data, t.Godzina, 
                   r.ID_Rezerwacji, r.Status_rezerwacji, 
                   p.ID_Uzytkownika, u.Imie, u.Nazwisko
            FROM Termin t
            LEFT JOIN Rezerwacja r ON t.ID_Terminu = r.ID_Terminu
            LEFT JOIN Pacjent p ON r.ID_Pacjenta = p.ID_Uzytkownika
            LEFT JOIN Uzytkownik u ON p.ID_Uzytkownika = u.ID_Uzytkownika
            WHERE t.ID_Lekarza = ? AND t.Data = ? AND (r.Status_rezerwacji IS NULL OR r.Status_rezerwacji != 'Anulowana')
            ORDER BY t.Godzina
        """;

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, doctorId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setIdTerminu(rs.getInt("ID_Terminu"));
                    dto.setData(rs.getDate("Data").toLocalDate());
                    dto.setGodzina(rs.getTime("Godzina").toLocalTime());

                    String status = rs.getString("Status_rezerwacji");
                    if (status == null) status = "Wolny";
                    dto.setStatus(status);

                    // Sprawdzamy ID_Uzytkownika (klucz pacjenta)
                    if (rs.getObject("ID_Uzytkownika") != null) {
                        dto.setIdPacjenta(rs.getInt("ID_Uzytkownika"));
                        // Imie i Nazwisko bierzemy z tabeli Uzytkownik (alias 'u')
                        dto.setImiePacjenta(rs.getString("Imie"));
                        dto.setNazwiskoPacjenta(rs.getString("Nazwisko"));
                        dto.setIdRezerwacji(rs.getInt("ID_Rezerwacji"));
                    }
                    list.add(dto);
                }
            }
        }
        return list;
    }

    // 2. Pobierz szczegóły jednej wizyty (JOIN do Uzytkownik dla Pacjenta)
    public HarmonogramDTO getVisitDetails(int terminId) throws SQLException {
        String query = """
            SELECT t.ID_Terminu, t.Data, 
                   p.ID_Uzytkownika, p.PESEL, 
                   u.Imie, u.Nazwisko, u.Numer_telefonu, u.Email,
                   w.Notatka, w.Opis_Powodu, pw.Powod_wizyty
            FROM Rezerwacja r
            JOIN Termin t ON r.ID_Terminu = t.ID_Terminu
            JOIN Pacjent p ON r.ID_Pacjenta = p.ID_Uzytkownika
            JOIN Uzytkownik u ON p.ID_Uzytkownika = u.ID_Uzytkownika
            LEFT JOIN Wizyta w ON r.ID_Rezerwacji = w.ID_Rezerwacji
            LEFT JOIN PowodWizyty pw ON w.ID_PowodWizyty = pw.ID_PowodWizyty
            WHERE t.ID_Terminu = ?
        """;

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, terminId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setIdTerminu(rs.getInt("ID_Terminu"));
                    dto.setIdPacjenta(rs.getInt("ID_Uzytkownika"));
                    dto.setImiePacjenta(rs.getString("Imie"));
                    dto.setNazwiskoPacjenta(rs.getString("Nazwisko"));

                    // Dane kontaktowe
                    dto.setPesel(rs.getString("PESEL"));
                    dto.setTelefon(rs.getString("Numer_telefonu"));
                    dto.setEmail(rs.getString("Email"));

                    // --- LOGIKA POWODU (Dlaczego przyszedł?) ---
                    String standard = rs.getString("Powod_wizyty");
                    String custom = rs.getString("Opis_Powodu");
                    dto.setPowodWizyty(standard != null ? standard : custom);

                    // --- NOTATKA LEKARZA (Co lekarz wpisał?) ---
                    dto.setNotatkaLekarza(rs.getString("Notatka"));

                    return dto;
                }
            }
        }
        return null;
    }

    // 3. Zapisz wizytę (Bez zmian strukturalnych, logika pozostaje ta sama)
    public void completeVisit(int terminId, String notatka, String status) throws SQLException {
        String query = """
            UPDATE Wizyta w
            SET Notatka = ?
            FROM Rezerwacja r
            WHERE w.ID_Rezerwacji = r.ID_Rezerwacji AND r.ID_Terminu = ?
        """;

        String updateStatus = "UPDATE Rezerwacja SET Status_rezerwacji = ? WHERE ID_Terminu = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnectionService.getConnection();
            conn.setAutoCommit(false);

            // Zapis notatki
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, notatka);
                stmt.setInt(2, terminId);
                stmt.executeUpdate();
            }

            // Zmiana statusu
            try (PreparedStatement stmt = conn.prepareStatement(updateStatus)) {
                stmt.setString(1, status);
                stmt.setInt(2, terminId);
                stmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        }
    }

    // 4. Pobierz historię pacjenta (JOIN do Uzytkownik dla Lekarza)
    public List<HarmonogramDTO> getPatientHistory(int patientId) throws SQLException {
        List<HarmonogramDTO> list = new ArrayList<>();
        String query = """
            SELECT t.Data, 
                   l_u.Imie, l_u.Nazwisko, s.Specjalizacja,
                   w.Notatka, w.Opis_Powodu, pw.Powod_wizyty
            FROM Rezerwacja r
            JOIN Termin t ON r.ID_Terminu = t.ID_Terminu
            JOIN Lekarz l ON t.ID_Lekarza = l.ID_Uzytkownika
            JOIN Uzytkownik l_u ON l.ID_Uzytkownika = l_u.ID_Uzytkownika
            LEFT JOIN Specjalizacja s ON l.ID_Specjalizacji = s.ID_Specjalizacji
            JOIN Wizyta w ON r.ID_Rezerwacji = w.ID_Rezerwacji
            LEFT JOIN PowodWizyty pw ON w.ID_PowodWizyty = pw.ID_PowodWizyty
            WHERE r.ID_Pacjenta = ? AND r.Status_rezerwacji = 'Odbyta'
            ORDER BY t.Data DESC
        """;

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HarmonogramDTO dto = new HarmonogramDTO();
                    dto.setData(rs.getDate("Data").toLocalDate());

                    String lekarzStr = rs.getString("Imie") + " " + rs.getString("Nazwisko") +
                            (rs.getString("Specjalizacja") != null ? " (" + rs.getString("Specjalizacja") + ")" : "");
                    dto.setLekarz(lekarzStr);

                    // Rozdzielamy dane!

                    // 1. Powód (z tabeli lub opis ręczny)
                    String powod = (rs.getString("Powod_wizyty") != null) ? rs.getString("Powod_wizyty") : rs.getString("Opis_Powodu");
                    dto.setPowodWizyty(powod != null ? powod : "-");

                    // 2. Notatka lekarska (zalecenia)
                    String notatka = rs.getString("Notatka");
                    dto.setNotatkaLekarza(notatka != null ? notatka : "Brak notatki");

                    list.add(dto);
                }
            }
        }
        return list;
    }
}