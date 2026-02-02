package com.example.security;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.UI;

public class UserSession {
    private int id;
    private String imie;
    private String nazwisko;
    private String rola;

    public UserSession(int id, String imie, String nazwisko, String rola) {
        this.id = id;
        this.imie = imie;
        this.nazwisko = nazwisko;
        this.rola = rola;
    }

    public UserSession() {
    }

    public static UserSession getLoggedInUser() {
        UserSession user = VaadinSession.getCurrent().getAttribute(UserSession.class);

        if (user == null) {
            UI.getCurrent().navigate("login");
            return null;
        }

        return user;
    }

    public int getId() { return id; }
    public String getImie() { return imie; }
    public String getNazwisko() { return nazwisko; }
    public String getRola() { return rola; }
}