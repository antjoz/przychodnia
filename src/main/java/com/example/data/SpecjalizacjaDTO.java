package com.example.data;

public class SpecjalizacjaDTO {
    private int id;
    private String nazwa;

    public SpecjalizacjaDTO(int id, String nazwa) {
        this.id = id;
        this.nazwa = nazwa;
    }

    public int getId() { return id; }
    public String getNazwa() { return nazwa; }

    // To ważne, aby ComboBox mógł czasem wyświetlić tekst domyślnie
    @Override
    public String toString() {
        return nazwa;
    }
}