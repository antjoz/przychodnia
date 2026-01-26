package com.example.views;

import com.example.services.AdminStatsService;
import com.example.security.UserSession;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;

@Route(value = "admin-stats", layout = MainLayout.class)
@PageTitle("Statystyki Przychodni")
public class AdminStatsView extends VerticalLayout {

    private final AdminStatsService statsService = new AdminStatsService();

    private VerticalLayout activeSpecChart;
    private VerticalLayout cancelledSpecChart;
    private VerticalLayout timelineContainer;

    public AdminStatsView() {
        UserSession currentUser = UserSession.getLoggedInUser();
        if (currentUser == null || !"Admin".equals(currentUser.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Dashboard Administratora"));

        // --- FILTRY DATY ---
        HorizontalLayout filters = new HorizontalLayout();
        filters.setDefaultVerticalComponentAlignment(Alignment.END);
        DatePicker startDate = new DatePicker("Od");
        startDate.setValue(LocalDate.now().minusDays(14));
        DatePicker endDate = new DatePicker("Do");
        endDate.setValue(LocalDate.now().plusDays(14));
        filters.add(startDate, endDate);
        add(filters);

        // --- SEKCJA 1: Wykres liniowy (Timeline) ---
        add(new H3("Wizyty w czasie (Niebieskie: Aktywne, Czerwone: Anulowane)"));
        timelineContainer = new VerticalLayout();
        add(timelineContainer);

        // --- SEKCJA 2: Specjalizacje (Obok siebie) ---
        HorizontalLayout specsLayout = new HorizontalLayout();
        specsLayout.setWidthFull();

        VerticalLayout leftCol = new VerticalLayout();
        leftCol.add(new H3("Najpopularniejsze (Zarezerwowane)"));
        activeSpecChart = new VerticalLayout();
        leftCol.add(activeSpecChart);

        VerticalLayout rightCol = new VerticalLayout();
        rightCol.add(new H3("Najczęściej Anulowane"));
        cancelledSpecChart = new VerticalLayout();
        rightCol.add(cancelledSpecChart);

        specsLayout.add(leftCol, rightCol);
        add(specsLayout);

        // Logika odświeżania
        startDate.addValueChangeListener(e -> refreshAll(startDate.getValue(), endDate.getValue()));
        endDate.addValueChangeListener(e -> refreshAll(startDate.getValue(), endDate.getValue()));

        refreshAll(startDate.getValue(), endDate.getValue());
    }

    private void refreshAll(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return;

        // 1. Oś czasu (Timeline)
        timelineContainer.removeAll();
        Map<LocalDate, Map<String, Integer>> timelineData = statsService.getTimelineStats(start, end);
        timelineContainer.add(new DoubleBarChart(timelineData));

        // 2. Specjalizacje - Aktywne
        activeSpecChart.removeAll();
        // Przekazujemy warunek SQL: != 'Anulowana'
        // UWAGA: Musisz poprawić metodę w Service, by przyjmowała ten parametr, lub użyć gotowych metod
        Map<String, Integer> activeData = statsService.getActiveVisitsBySpecialization();
        activeSpecChart.add(new SimpleBarChart(activeData, true, "#1676f3")); // Niebieski

        // 3. Specjalizacje - Anulowane
        cancelledSpecChart.removeAll();
        Map<String, Integer> cancelledData = statsService.getCancelledVisitsBySpecialization();
        cancelledSpecChart.add(new SimpleBarChart(cancelledData, true, "#E03030")); // Czerwony
    }

    // --- KOMPONENT 1: Prosty wykres (Poziomy) ---
    public static class SimpleBarChart extends Div {
        public SimpleBarChart(Map<String, Integer> data, boolean horizontal, String color) {
            setWidthFull();
            addClassName("chart-simple");

            int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

            if (horizontal) {
                getStyle().set("display", "flex");
                getStyle().set("flex-direction", "column");
                getStyle().set("gap", "8px");

                data.forEach((label, value) -> {
                    if (value == 0) return; // Ukrywamy zera dla czystości

                    HorizontalLayout row = new HorizontalLayout();
                    row.setWidthFull();
                    row.setAlignItems(HorizontalLayout.Alignment.CENTER);

                    Span labelSpan = new Span(label);
                    labelSpan.setWidth("120px");
                    labelSpan.getStyle().set("font-size", "0.9em");

                    Div barContainer = new Div();
                    barContainer.setWidthFull();
                    barContainer.getStyle().set("background-color", "#f5f5f5");
                    barContainer.getStyle().set("height", "20px");
                    barContainer.getStyle().set("border-radius", "4px");

                    Div bar = new Div();
                    double percent = ((double) value / maxValue) * 100;
                    bar.setWidth(percent + "%");
                    bar.setHeight("100%");
                    bar.getStyle().set("background-color", color);
                    bar.getStyle().set("border-radius", "4px");

                    Span countSpan = new Span(String.valueOf(value));
                    countSpan.getStyle().set("margin-left", "10px");
                    countSpan.getStyle().set("font-weight", "bold");

                    barContainer.add(bar);
                    row.add(labelSpan, barContainer, countSpan);
                    add(row);
                });
            }
        }
    }

    // --- KOMPONENT 2: Wykres podwójny (Pionowy - Timeline) ---
    public static class DoubleBarChart extends Div {
        public DoubleBarChart(Map<LocalDate, Map<String, Integer>> data) {
            setWidthFull();
            getStyle().set("display", "flex");
            getStyle().set("flex-direction", "row");
            getStyle().set("align-items", "flex-end");
            getStyle().set("height", "250px");
            getStyle().set("gap", "4px");
            getStyle().set("overflow-x", "auto");
            getStyle().set("padding-bottom", "40px");

            // Znajdź max wartość w obu kategoriach, żeby wyskalować
            int globalMax = 1;
            for (Map<String, Integer> day : data.values()) {
                globalMax = Math.max(globalMax, day.get("Active"));
                globalMax = Math.max(globalMax, day.get("Cancelled"));
            }

            for (Map.Entry<LocalDate, Map<String, Integer>> entry : data.entrySet()) {
                LocalDate date = entry.getKey();
                int active = entry.getValue().get("Active");
                int cancelled = entry.getValue().get("Cancelled");

                VerticalLayout col = new VerticalLayout();
                col.setPadding(false);
                col.setSpacing(false);
                col.setAlignItems(VerticalLayout.Alignment.CENTER);
                col.setWidth("35px");
                col.getStyle().set("flex-shrink", "0"); // Nie zgniataj słupków

                // Słupek Aktywny (Niebieski)
                Div barActive = createBar(active, globalMax, "#1676f3");
                // Słupek Anulowany (Czerwony)
                Div barCancelled = createBar(cancelled, globalMax, "#E03030");

                Span dateLabel = new Span(date.getMonthValue() + "-" + date.getDayOfMonth());
                dateLabel.getStyle().set("font-size", "0.65em");
                dateLabel.getStyle().set("transform", "rotate(-90deg)");
                dateLabel.getStyle().set("margin-top", "15px");
                dateLabel.getStyle().set("white-space", "nowrap");

                HorizontalLayout barsRow = new HorizontalLayout(barActive, barCancelled);
                barsRow.setSpacing(false);
                barsRow.setAlignItems(Alignment.END);
                barsRow.setHeight("100%"); // Wypełnij wysokość kontenera

                col.add(barsRow, dateLabel);
                add(col);
            }
        }

        private Div createBar(int value, int max, String color) {
            Div bar = new Div();
            double percent = ((double) value / max) * 100;
            // Minimalna wysokość 2px żeby było widać, że jest 0
            if (value == 0) percent = 1;

            bar.setWidth("8px");
            bar.setHeight(percent + "%");
            bar.getStyle().set("background-color", color);
            bar.getStyle().set("margin-right", "2px");
            bar.setTitle("Liczba: " + value); // Tooltip
            return bar;
        }
    }
}