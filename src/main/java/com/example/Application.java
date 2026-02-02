package com.example;

import com.example.services.DatabaseConnectionService;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement; // Ważny import!

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@PWA(name = "Przychodnia", shortName = "App", offlinePath="offline.html")
public class Application implements AppShellConfigurator, CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> [START] Sprawdzanie obecności Administratora...");

        try (Connection conn = DatabaseConnectionService.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM Uzytkownik WHERE Login = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, "admin");
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println(">>> [START] Administrator już istnieje. Pomijanie.");
                    return;
                }
            }

            System.out.println(">>> [START] Brak Administratora. Tworzenie konta...");
            conn.setAutoCommit(false);

            try {
                String hashedPassword = BCrypt.hashpw("admin", BCrypt.gensalt());
                long generatedId = -1;

                String sqlUser = "INSERT INTO Uzytkownik (Imie, Nazwisko, Login, Haslo, Numer_telefonu, Email, Rola, Czy_aktywny) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'Admin', TRUE)";

                try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                    stmtUser.setString(1, "admin");
                    stmtUser.setString(2, "admin");
                    stmtUser.setString(3, "admin");
                    stmtUser.setString(4, hashedPassword);
                    stmtUser.setString(5, "000000000");
                    stmtUser.setString(6, "admin@admin.pl");

                    int affectedRows = stmtUser.executeUpdate();
                    if (affectedRows == 0) throw new Exception("Nie udało się utworzyć użytkownika.");

                    // Pobranie wygenerowanego ID
                    try (ResultSet generatedKeys = stmtUser.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            generatedId = generatedKeys.getLong(1);
                        } else {
                            throw new Exception("Nie udało się pobrać ID nowego użytkownika.");
                        }
                    }
                }
                String sqlAdmin = "INSERT INTO Administrator (ID_Uzytkownika) VALUES (?)";

                try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
                    stmtAdmin.setLong(1, generatedId);
                    stmtAdmin.executeUpdate();
                }
                conn.commit();
                System.out.println(">>> [START] SUKCES! Utworzono admina (ID: " + generatedId + ")");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            System.err.println("!!! [START] Błąd krytyczny: " + e.getMessage());
            e.printStackTrace();
        }
    }
}