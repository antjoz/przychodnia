package com.example.services;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminStatsService {

    public Map<LocalDate, Integer> getDailyStats(LocalDate start, LocalDate end, boolean isCancellation) {

        // 1. Przygotuj mapę z zerami dla każdego dnia z zakresu (żeby wykres nie miał dziur)
        Map<LocalDate, Integer> stats = new LinkedHashMap<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            stats.put(current, 0);
            current = current.plusDays(1);
        }

        String operator = isCancellation ? "=" : "<>";

        String sql = "SELECT t.Data, COUNT(r.ID_Rezerwacji) as Liczba " +
                "FROM Termin t " +
                "JOIN Rezerwacja r ON t.ID_Terminu = r.ID_Terminu " +
                "WHERE t.Data BETWEEN ? AND ? " +
                "AND r.Status_rezerwacji " + operator + " 'Anulowana' " +
                "GROUP BY t.Data";

        try (Connection conn = DatabaseConnectionService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("Data").toLocalDate();
                    int count = rs.getInt("Liczba");
                    if (stats.containsKey(date)) {
                        stats.put(date, count);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}