package com.example.views;

import com.example.services.AdminStatsService;
import com.example.security.UserSession;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.Map;

@Route(value = "admin-stats", layout = MainLayout.class)
@PageTitle("Statystyki")
public class AdminStatsView extends VerticalLayout {

    private final AdminStatsService statsService = new AdminStatsService();

    // Kontenery, do których wrzucimy wykresy
    private HorizontalLayout reservationsChartLayout;
    private HorizontalLayout cancellationsChartLayout;

    public AdminStatsView() {
        UserSession currentUser = UserSession.getLoggedInUser();
        if (currentUser == null || !"Admin".equals(currentUser.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        setPadding(true);

        add(new H2("Statystyki Rezerwacji"));

        // --- FILTRY ---
        DatePicker startDate = new DatePicker("Od");
        startDate.setValue(LocalDate.now().minusDays(10)); // Domyślnie ostatnie 10 dni

        DatePicker endDate = new DatePicker("Do");
        endDate.setValue(LocalDate.now().plusDays(5));

        HorizontalLayout filters = new HorizontalLayout(startDate, endDate);
        filters.setAlignItems(Alignment.END);

        // Przycisk odśwież
        Div refreshBtn = new Div(); // placeholder, wystarczy event listener

        // --- WYKRES 1: REZERWACJE ---
        add(new H4("Liczba udanych rezerwacji (dzień po dniu)"));
        reservationsChartLayout = new HorizontalLayout();
        reservationsChartLayout.setWidthFull();
        reservationsChartLayout.setHeight("200px"); // Wysokość wykresu
        reservationsChartLayout.setAlignItems(Alignment.END); // Słupki rosną od dołu
        // Dodajemy pasek przewijania, jeśli dni jest dużo
        reservationsChartLayout.getStyle().set("overflow-x", "auto");

        add(reservationsChartLayout);

        // --- WYKRES 2: ANULOWANE ---
        add(new H4("Liczba anulowanych wizyt"));
        cancellationsChartLayout = new HorizontalLayout();
        cancellationsChartLayout.setWidthFull();
        cancellationsChartLayout.setHeight("200px");
        cancellationsChartLayout.setAlignItems(Alignment.END);
        cancellationsChartLayout.getStyle().set("overflow-x", "auto");

        add(cancellationsChartLayout);

        // --- LOGIKA ---
        startDate.addValueChangeListener(e -> refreshCharts(startDate.getValue(), endDate.getValue()));
        endDate.addValueChangeListener(e -> refreshCharts(startDate.getValue(), endDate.getValue()));

        // Pierwsze uruchomienie
        refreshCharts(startDate.getValue(), endDate.getValue());
    }

    private void refreshCharts(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return;

        // 1. Pobierz dane
        Map<LocalDate, Integer> reservations = statsService.getDailyStats(start, end, false);
        Map<LocalDate, Integer> cancellations = statsService.getDailyStats(start, end, true);

        // 2. Rysuj wykresy
        // Kolor niebieski dla rezerwacji (#1676f3), Czerwony dla anulowanych (#e03030)
        drawChart(reservationsChartLayout, reservations, "#1676f3");
        drawChart(cancellationsChartLayout, cancellations, "#e03030");
    }

    private void drawChart(HorizontalLayout container, Map<LocalDate, Integer> data, String color) {
        container.removeAll();

        // Znajdź wartość maksymalną, żeby wyskalować słupki (żeby najwyższy miał 100% wysokości)
        int maxValue = data.values().stream().mapToInt(v -> v).max().orElse(1);
        if (maxValue == 0) maxValue = 1; // Zabezpieczenie przed dzieleniem przez 0

        for (Map.Entry<LocalDate, Integer> entry : data.entrySet()) {
            LocalDate date = entry.getKey();
            int value = entry.getValue();

            // Słupek to VerticalLayout zawierający: liczbę, kolorowy prostokąt, datę
            VerticalLayout barWrapper = new VerticalLayout();
            barWrapper.setSpacing(false);
            barWrapper.setPadding(false);
            barWrapper.setAlignItems(Alignment.CENTER);
            barWrapper.setWidth("50px"); // Stała szerokość słupka
            barWrapper.getStyle().set("flex-shrink", "0"); // Nie ściskaj słupków

            // 1. Liczba nad słupkiem
            Span valueLabel = new Span(String.valueOf(value));
            valueLabel.getStyle().set("font-size", "12px");
            valueLabel.getStyle().set("margin-bottom", "5px");

            // 2. Kolorowy prostokąt (Bar)
            Div bar = new Div();
            bar.setWidth("30px");
            // Oblicz wysokość w procentach
            double heightPercent = ((double) value / maxValue) * 100;
            // Minimalna wysokość 2px, żeby było widać, że to 0
            if (heightPercent < 1 && value == 0) heightPercent = 2;

            bar.setHeight(heightPercent + "%");
            bar.getStyle().set("background-color", color);
            bar.getStyle().set("border-radius", "4px 4px 0 0");
            bar.getStyle().set("transition", "height 0.5s"); // Ładna animacja wzrostu

            // Jeśli słupek jest mały (0%), ustawiamy mu minimalną wysokość w pikselach dla estetyki
            if (value == 0) {
                bar.getStyle().set("height", "2px");
                bar.getStyle().set("background-color", "#ccc"); // Szary dla zera
            }

            // 3. Data pod słupkiem
            Span dateLabel = new Span(date.getDayOfMonth() + "." + date.getMonthValue());
            dateLabel.getStyle().set("font-size", "10px");
            dateLabel.getStyle().set("margin-top", "5px");

            barWrapper.add(valueLabel, bar, dateLabel);

            // Ważne: musimy ustawić flex-grow dla wrappera wewnątrz kontenera poziomego,
            // ale tutaj chcemy, by wysokość słupka (Div bar) była relatywna do wysokości wrappera.
            // Prostsza metoda: Ustawiamy wysokość wrappera na 100% kontenera.
            barWrapper.setHeight("100%");
            // Ale musimy wyrównać zawartość do dołu (Alignment.END dla wrappera nie zadziała idealnie na Div w środku bez flexa).
            // Użyjmy justify-content w wrapperze:
            barWrapper.setJustifyContentMode(JustifyContentMode.END);

            container.add(barWrapper);
        }
    }
}