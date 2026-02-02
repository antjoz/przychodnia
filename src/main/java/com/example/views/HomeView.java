package com.example.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        getStyle().set("overflow", "auto");
        setPadding(true);
        setSpacing(true);

        VerticalLayout heroSection = new VerticalLayout();
        heroSection.setAlignItems(Alignment.CENTER);

        heroSection.getStyle().set("margin-top", "100px");

        heroSection.setSpacing(true);

        heroSection.add(new H1("Witaj w Przychodni BAZUJEMY na zdrowiu"));

        Button loginBtn = new Button("Zaloguj się", e -> UI.getCurrent().navigate("login"));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.setWidth("200px");

        Button registerBtn = new Button("Zarejestruj się", e -> UI.getCurrent().navigate("register"));
        registerBtn.setWidth("200px");

        heroSection.add(loginBtn, registerBtn);

        FlexLayout contactSection = new FlexLayout();
        contactSection.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        contactSection.setJustifyContentMode(JustifyContentMode.CENTER);
        contactSection.setAlignItems(Alignment.CENTER);
        contactSection.setWidthFull();
        contactSection.getStyle().set("gap", "40px");

        contactSection.getStyle().set("margin-top", "80px");
        contactSection.getStyle().set("margin-bottom", "50px");

        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setWidth("auto");
        infoLayout.setAlignItems(Alignment.START);

        infoLayout.add(new H2("Kontakt"));
        infoLayout.add(createIconRow(VaadinIcon.MAP_MARKER, "Plac Grunwaldzki 22, 50-384 Wrocław"));
        infoLayout.add(createIconRow(VaadinIcon.PHONE, "+48 71 320 00 00"));
        infoLayout.add(createIconRow(VaadinIcon.ENVELOPE, "rejestracja@przychodnia-wroclaw.pl"));

        infoLayout.add(new H3("Godziny otwarcia"));
        infoLayout.add(new Span("Pon - Pt: 08:00 - 18:00"));
        infoLayout.add(new Span("Sobota i Niedziela: Zamknięte"));

        String mapUrl = "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d2504.667232386266!2d17.05587431576184!3d51.11475797957256!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x470fe9c4d21650b9%3A0x675841121d28303d!2sPlac%20Grunwaldzki%2C%20Wroc%C5%82aw!5e0!3m2!1spl!2spl!4v1645000000000!5m2!1spl!2spl";

        IFrame googleMap = new IFrame(mapUrl);
        googleMap.setWidth("400px");
        googleMap.setHeight("300px");
        googleMap.getStyle().set("border", "0");
        googleMap.getStyle().set("border-radius", "10px");
        googleMap.getStyle().set("box-shadow", "0 4px 8px rgba(0,0,0,0.1)");

        contactSection.add(infoLayout, googleMap);

        add(heroSection, contactSection);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String text) {
        Icon i = icon.create();
        i.setColor("#006AF5");
        i.setSize("20px");
        Span s = new Span(text);
        HorizontalLayout row = new HorizontalLayout(i, s);
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        return row;
    }
}