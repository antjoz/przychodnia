package com.example.views;

import com.example.services.AdminStatsService;
import com.example.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Route(value = "admin-stats", layout = MainLayout.class)
@PageTitle("Statystyki - Dashboard")
public class AdminStatsView extends VerticalLayout {

    private final AdminStatsService statsService = new AdminStatsService();

    // Pola daty
    private DatePicker startDate;
    private DatePicker endDate;

    // Kontenery na wykresy (Layouty wewnątrz kart)
    private HorizontalLayout reservationsChartLayout;
    private HorizontalLayout cancellationsChartLayout;

    public AdminStatsView() {
        // 1. Zabezpieczenie dostępu
        UserSession currentUser = UserSession.getLoggedInUser();
        if (currentUser == null || !"Admin".equals(currentUser.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        // 2. Ustawienia głównego widoku (tło dashboardu)
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        // Lekko szare tło dla całego widoku, żeby białe karty się wyróżniały
        getStyle().set("background-color", "#f4f6f8");

        // --- NAGŁÓWEK ---
        H2 title = new H2("Statystyki Przychodni");
        title.getStyle().set("margin-top", "0");

        // --- PASEK NARZĘDZI (Filtry) ---
        HorizontalLayout toolbar = createToolbar();

        // --- KARTY Z WYKRESAMI ---
        // Używamy FlexLayout z wrapowaniem, żeby karty układały się ładnie
        FlexLayout dashboardLayout = new FlexLayout();
        dashboardLayout.setWidthFull();
        dashboardLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        dashboardLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        dashboardLayout.setAlignItems(Alignment.STRETCH);
        dashboardLayout.getStyle().set("gap", "20px"); // Odstęp między kartami

        // Inicjalizacja kontenerów na słupki
        reservationsChartLayout = createChartContainer();
        cancellationsChartLayout = createChartContainer();

        // Tworzenie Kart (Card)
        Div cardReservations = createCard("Zrealizowane Wizyty", reservationsChartLayout);
        Div cardCancellations = createCard("Anulowane Wizyty", cancellationsChartLayout);

        dashboardLayout.add(cardReservations, cardCancellations);

        add(title, toolbar, dashboardLayout);

        // --- START ---
        // Domyślnie ostatnie 14 dni
        setRange(LocalDate.now().minusDays(13), LocalDate.now());
    }

    private HorizontalLayout createToolbar() {
        startDate = new DatePicker("Od");
        endDate = new DatePicker("Do");

        // Logika odświeżania po zmianie daty ręcznie
        startDate.addValueChangeListener(e -> refreshCharts());
        endDate.addValueChangeListener(e -> refreshCharts());

        // Przyciski szybkich zakresów
        Button btn7Days = new Button("7 Dni", e -> setRange(LocalDate.now().minusDays(6), LocalDate.now()));
        Button btn30Days = new Button("30 Dni", e -> setRange(LocalDate.now().minusDays(29), LocalDate.now()));
        Button btnThisMonth = new Button("Ten miesiąc", e -> {
            LocalDate now = LocalDate.now();
            LocalDate firstDay = YearMonth.now().atDay(1);
            setRange(firstDay, now);
        });

        // Stylizacja przycisków (drobne, tertiary)
        btn7Days.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btn30Days.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnThisMonth.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout toolbar = new HorizontalLayout(startDate, endDate, btn7Days, btn30Days, btnThisMonth);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.addClassName("toolbar");
        toolbar.getStyle().set("background-color", "white");
        toolbar.getStyle().set("padding", "15px");
        toolbar.getStyle().set("border-radius", "10px");
        toolbar.getStyle().set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)");
        toolbar.setWidthFull();

        return toolbar;
    }

    // Pomocnicza metoda do tworzenia "Karty" (Biały prostokąt z cieniem)
    private Div createCard(String titleText, HorizontalLayout content) {
        Div card = new Div();
        // Style karty
        card.getStyle().set("background-color", "white");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)");
        card.getStyle().set("padding", "20px");
        card.getStyle().set("min-width", "400px"); // Minimalna szerokość
        card.getStyle().set("flex-grow", "1");      // Rozciąganie
        card.getStyle().set("flex-basis", "45%");   // Domyślnie zajmuje ok połowy ekranu

        H3 title = new H3(titleText);
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("color", "#555");
        title.getStyle().set("font-size", "1.1em");

        card.add(title, content);
        return card;
    }

    private HorizontalLayout createChartContainer() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setHeight("250px"); // Trochę wyższy wykres
        layout.setAlignItems(Alignment.END);
        layout.getStyle().set("overflow-x", "auto"); // Przewijanie
        layout.getStyle().set("padding-bottom", "10px");
        // Scrollbar styling (opcjonalnie, dla webkit)
        layout.getElement().executeJs("this.style.scrollbarWidth = 'thin';");
        return layout;
    }

    private void setRange(LocalDate start, LocalDate end) {
        // Ustawienie wartości wywoła listenery, które wywołają refreshCharts()
        // Blokujemy listenery na chwilę, żeby nie odświeżać dwa razy
        startDate.setValue(start);
        endDate.setValue(end);
    }

    private void refreshCharts() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        if (start == null || end == null || start.isAfter(end)) return;

        // 1. Pobierz dane
        Map<LocalDate, Integer> reservations = statsService.getDailyStats(start, end, false);
        Map<LocalDate, Integer> cancellations = statsService.getDailyStats(start, end, true);

        // 2. Rysuj wykresy (Rezerwacje - Niebieski, Anulowane - Czerwony/Pomarańczowy)
        drawChart(reservationsChartLayout, reservations, "#1676f3");
        drawChart(cancellationsChartLayout, cancellations, "#e63946");
    }

    private void drawChart(HorizontalLayout container, Map<LocalDate, Integer> data, String color) {
        container.removeAll();

        int maxValue = data.values().stream().mapToInt(v -> v).max().orElse(1);
        if (maxValue == 0) maxValue = 1;

        for (Map.Entry<LocalDate, Integer> entry : data.entrySet()) {
            LocalDate date = entry.getKey();
            int value = entry.getValue();

            // WRAPPER SŁUPKA
            VerticalLayout barWrapper = new VerticalLayout();
            barWrapper.setSpacing(false);
            barWrapper.setPadding(false);
            barWrapper.setJustifyContentMode(JustifyContentMode.END); // Dół
            barWrapper.setAlignItems(Alignment.CENTER); // Środek w poziomie
            barWrapper.setWidth("40px");
            barWrapper.getStyle().set("flex-shrink", "0");
            barWrapper.setHeight("100%");

            // ETYKIETA WARTOŚCI (nad słupkiem)
            Span valueLabel = new Span(String.valueOf(value));
            valueLabel.getStyle().set("font-size", "11px");
            valueLabel.getStyle().set("font-weight", "bold");
            valueLabel.getStyle().set("color", "#666");
            valueLabel.getStyle().set("margin-bottom", "4px");
            // Ukrywamy zero, żeby wykres był czystszy (opcjonalnie)
            if (value == 0) valueLabel.setVisible(false);

            // SŁUPEK (Bar)
            Div bar = new Div();
            bar.setWidth("20px"); // Węższe słupki wyglądają nowocześniej

            double heightPercent = ((double) value / maxValue) * 100;
            if (heightPercent < 2 && value == 0) heightPercent = 2; // min wysokość dla zera

            bar.setHeight(heightPercent + "%");
            bar.getStyle().set("background-color", color);
            bar.getStyle().set("border-radius", "4px 4px 0 0");
            bar.getStyle().set("transition", "all 0.3s ease"); // Płynna animacja

            // Tooltip (dymek po najechaniu)
            bar.setTitle(date.toString() + ": " + value);

            // Efekt Hover (rozjaśnienie po najechaniu)
            // W Javie Vaadin dodanie CSS :hover jest trudne bez pliku CSS,
            // ale tooltip systemowy (setTitle) załatwia sprawę informacji.

            if (value == 0) {
                bar.getStyle().set("background-color", "#e0e0e0"); // Szary dla zera
            }

            // ETYKIETA DATY (pod słupkiem)
            Span dateLabel = new Span(date.getDayOfMonth() + "." + date.getMonthValue());
            dateLabel.getStyle().set("font-size", "9px");
            dateLabel.getStyle().set("color", "#888");
            dateLabel.getStyle().set("margin-top", "6px");

            barWrapper.add(valueLabel, bar, dateLabel);
            container.add(barWrapper);
        }
    }
}