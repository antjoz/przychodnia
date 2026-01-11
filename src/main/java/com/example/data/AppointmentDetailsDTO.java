package com.example.data;

public class AppointmentDetailsDTO {
    private String imie;
    private String nazwisko;
    private String pesel; // <-- NOWE POLE
    private String telefon;
    private String status;
    private String opisPowodu;

    public AppointmentDetailsDTO(String imie, String nazwisko, String pesel, String telefon, String status, String opisPowodu) {
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.pesel = pesel; // <-- ZAPISUJEMY
        this.telefon = telefon;
        this.status = status;
        this.opisPowodu = opisPowodu;
    }

    public String getImie() { return imie; }
    public String getNazwisko() { return nazwisko; }
    public String getPesel() { return pesel; } // <-- GETTER
    public String getTelefon() { return telefon; }
    public String getStatus() { return status; }
    public String getOpisPowodu() { return opisPowodu; }
}