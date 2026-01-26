package com.example.services;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminStatsService {

    // 1. Liczba wizyt wg specjalizacji (TYLKO ODBYTE/PLANOWANE - bez anulowanych)
    public Map<String, Integer> getActiveVisitsBySpecialization() {
        return getVisitsBySpecHelper("IS DISTINCT FROM 'Anulowana'"); // Postgres syntax, dla MySQL: "!= 'Anulowana'"
    }

    // 2. Liczba ANULOWANYCH wizyt wg specjalizacji
    public Map<String, Integer> getCancelledVisitsBySpecialization() {
        return getVisitsBySpecHelper("= 'Anulowana'");
    }

    // Pomocnicza metoda do SQL (unika powtarzania kodu)
    private Map<String, Integer> getVisitsBySpecHelper(String statusCondition) {
        Map<String, Integer> stats = new java.util.HashMap<>();
        // Dostosuj operator (Postgres: IS DISTINCT FROM, MySQL/MariaDB: != lub <>)
        // Zakładam standardowy SQL: != lub =
        String operator = statusCondition.contains("DISTINCT") ? statusCondition : statusCondition;

        // Dla pewności użyjmy prostego warunku w WHERE w kodzie poniżej

        String sql = "SELECT s.Specjalizacja, COUNT(r.ID_Rezerwacji) as Liczba " +
                "FROM Specjalizacja s " +
                "LEFT JOIN Lekarz l ON s.ID_Specjalizacji = l.ID_Specjalizacji " +
                "LEFT JOIN Termin t ON l.ID_Uzytkownika = t.ID_Lekarza " +
                "JOIN Rezerwacja r ON t.ID_Terminu = r.ID_Terminu " +
                "WHERE r.Status " + statusCondition + " " +
                "GROUP BY s.Specjalizacja";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("Specjalizacja"), rs.getInt("Liczba"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    // 3. Oś czasu: ODBYTE vs ANULOWANE
    // Zwracamy mapę map: Data -> { "Active": 5, "Cancelled": 2 }
    public Map<LocalDate, Map<String, Integer>> getTimelineStats(LocalDate start, LocalDate end) {
        Map<LocalDate, Map<String, Integer>> stats = new LinkedHashMap<>();

        // Inicjalizacja dat zerami (żeby wykres nie miał dziur)
        LocalDate current = start;
        while (!current.isAfter(end)) {
            Map<String, Integer> dayStats = new java.util.HashMap<>();
            dayStats.put("Active", 0);
            dayStats.put("Cancelled", 0);
            stats.put(current, dayStats);
            current = current.plusDays(1);
        }

        String sql = "SELECT t.Data, r.Status, COUNT(r.ID_Rezerwacji) as Liczba " +
                "FROM Termin t " +
                "JOIN Rezerwacja r ON t.ID_Terminu = r.ID_Terminu " +
                "WHERE t.Data BETWEEN ? AND ? " +
                "GROUP BY t.Data, r.Status";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("Data").toLocalDate();
                    String status = rs.getString("Status");
                    int count = rs.getInt("Liczba");

                    if (stats.containsKey(date)) {
                        if ("Anulowana".equals(status)) {
                            stats.get(date).put("Cancelled", count);
                        } else {
                            // Sumujemy wszystkie inne statusy jako "Active"
                            int currentActive = stats.get(date).get("Active");
                            stats.get(date).put("Active", currentActive + count);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}