package com.example.data;

public class UserDTO {
    private int id;
    private String imie;
    private String nazwisko;
    private String login;
    private String rola;
    private String email;
    private String telefon;
    private boolean czyAktywny;
    private Integer idLekarza;
    private String specjalizacja;
    private String pesel;

    // Główny konstruktor ze wszystkimi polami
    public UserDTO(int id, String imie, String nazwisko, String login, String rola,
                   String email, String telefon, boolean czyAktywny,
                   Integer idLekarza, String specjalizacja, String pesel) {
        this.id = id;
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.login = login;
        this.rola = rola;
        this.email = email;
        this.telefon = telefon;
        this.czyAktywny = czyAktywny;
        this.idLekarza = idLekarza;
        this.specjalizacja = specjalizacja;
        this.pesel = pesel;
    }

    // Konstruktor uproszczony (dla kompatybilności lub gdy PESEL nie jest potrzebny)
    public UserDTO(int id, String imie, String nazwisko, String login, String rola,
                   String email, String telefon, boolean czyAktywny, Integer idLekarza, String specjalizacja) {
        this(id, imie, nazwisko, login, rola, email, telefon, czyAktywny, idLekarza, specjalizacja, null);
    }

    // Gettery
    public int getId() { return id; }
    public String getImie() { return imie; }
    public String getNazwisko() { return nazwisko; }
    public String getLogin() { return login; }
    public String getRola() { return rola; }
    public String getEmail() { return email; }
    public String getTelefon() { return telefon; }
    public boolean isCzyAktywny() { return czyAktywny; }
    public Integer getIdLekarza() { return idLekarza; }
    public String getSpecjalizacja() { return specjalizacja; }
    public String getPesel() { return pesel; }

    // Settery (opcjonalne, zależnie od potrzeb)
    public void setCzyAktywny(boolean czyAktywny) { this.czyAktywny = czyAktywny; }
    public void setSpecjalizacja(String specjalizacja) { this.specjalizacja = specjalizacja; }
    public void setPesel(String pesel) { this.pesel = pesel; }
}