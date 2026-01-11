package com.example.data;

import java.time.LocalDate;
import java.time.LocalTime;

public class HarmonogramDTO {
    // Podstawowe
    private int idTerminu;
    private String status;
    private LocalDate data;
    private LocalTime godzina;
    private int idLekarza;
    private String lekarz;

    // Pacjent
    private int idRezerwacji;
    private int idPacjenta;
    private String imiePacjenta;
    private String nazwiskoPacjenta;

    // Dane kontaktowe
    private String pesel;
    private String telefon;
    private String email;

    // --- KLUCZOWA ZMIANA ---
    private String powodWizyty;    // To będzie: "Ból głowy" (z tabeli) LUB "Pacjent skarży się na..." (opis ręczny)
    private String notatkaLekarza; // To jest: "Zalecenia, przebieg badania" (kolumna Notatka z tabeli Wizyta)

    public HarmonogramDTO() {}

    // Gettery
    public int getIdTerminu() { return idTerminu; }
    public String getStatus() { return status; }
    public LocalDate getData() { return data; }
    public LocalTime getGodzina() { return godzina; }
    public String getLekarz() { return lekarz; }
    public int getIdRezerwacji() { return idRezerwacji; }
    public int getIdPacjenta() { return idPacjenta; }
    public String getImiePacjenta() { return imiePacjenta; }
    public String getNazwiskoPacjenta() { return nazwiskoPacjenta; }
    public String getPesel() { return pesel; }
    public String getTelefon() { return telefon; }
    public String getEmail() { return email; }

    public String getPowodWizyty() { return powodWizyty; }
    public String getNotatkaLekarza() { return notatkaLekarza; }

    // Settery
    public void setIdTerminu(int idTerminu) { this.idTerminu = idTerminu; }
    public void setStatus(String status) { this.status = status; }
    public void setData(LocalDate data) { this.data = data; }
    public void setGodzina(LocalTime godzina) { this.godzina = godzina; }
    public void setLekarz(String lekarz) { this.lekarz = lekarz; }
    public void setIdRezerwacji(int idRezerwacji) { this.idRezerwacji = idRezerwacji; }
    public void setIdPacjenta(int idPacjenta) { this.idPacjenta = idPacjenta; }
    public void setImiePacjenta(String imiePacjenta) { this.imiePacjenta = imiePacjenta; }
    public void setNazwiskoPacjenta(String nazwiskoPacjenta) { this.nazwiskoPacjenta = nazwiskoPacjenta; }
    public void setPesel(String pesel) { this.pesel = pesel; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
    public void setEmail(String email) { this.email = email; }

    public void setPowodWizyty(String powodWizyty) { this.powodWizyty = powodWizyty; }
    public void setNotatkaLekarza(String notatkaLekarza) { this.notatkaLekarza = notatkaLekarza; }
}