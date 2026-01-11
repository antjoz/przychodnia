package com.example.views;

import com.example.data.HarmonogramDTO;
import com.example.security.UserSession;
import com.example.services.ReceptionService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;

@Route(value = "patient-history", layout = MainLayout.class)
@PageTitle("Historia wizyt")
public class PatientHistoryView extends VerticalLayout {

    private final ReceptionService receptionService = new ReceptionService();
    private Grid<HarmonogramDTO> grid;
    private ListDataProvider<HarmonogramDTO> dataProvider;

    private ComboBox<String> specializationFilter;
    private DatePicker dateFrom;
    private DatePicker dateTo;

    public PatientHistoryView() {
        UserSession user = UserSession.getLoggedInUser();
        if (user == null || !"Pacjent".equals(user.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        setPadding(true);
        add(new H2("Historia Twoich wizyt"));

        // --- FILTRY ---
        HorizontalLayout filters = new HorizontalLayout();
        filters.setAlignItems(Alignment.BASELINE);

        specializationFilter = new ComboBox<>("Specjalizacja");
        try {
            specializationFilter.setItems(receptionService.getAllSpecializations());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dateFrom = new DatePicker("Od");
        dateTo = new DatePicker("Do");

        filters.add(specializationFilter, dateFrom, dateTo);

        // Listener odświeżania filtrów
        specializationFilter.addValueChangeListener(e -> filterGrid());
        dateFrom.addValueChangeListener(e -> filterGrid());
        dateTo.addValueChangeListener(e -> filterGrid());

        add(filters);

        // --- TABELA ---
        grid = new Grid<>();
        grid.addColumn(HarmonogramDTO::getData).setHeader("Data").setSortable(true);
        grid.addColumn(HarmonogramDTO::getGodzina).setHeader("Godzina");
        grid.addColumn(HarmonogramDTO::getLekarz).setHeader("Lekarz");
        grid.addColumn(HarmonogramDTO::getStatus).setHeader("Status").setSortable(true);
        grid.addColumn(HarmonogramDTO::getPowodWizyty).setHeader("Powód");

        add(grid);
        loadData(user.getId());
    }

    private void loadData(int patientId) {
        try {
            List<HarmonogramDTO> data = receptionService.getPatientReservations(patientId);
            dataProvider = new ListDataProvider<>(data);
            grid.setDataProvider(dataProvider);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterGrid() {
        if (dataProvider == null) return;

        dataProvider.setFilter(dto -> {
            // Filtr specjalizacji (sprawdzamy czy string lekarza zawiera specj.)
            boolean specMatch = true;
            if (specializationFilter.getValue() != null) {
                // Lekarz w DTO ma format: "Jan Kowalski (Kardiolog)"
                // Więc sprawdzamy czy zawiera nazwę specjalizacji
                if (dto.getLekarz() == null || !dto.getLekarz().contains(specializationFilter.getValue())) {
                    specMatch = false;
                }
            }

            // Filtr daty Od
            boolean dateFromMatch = true;
            if (dateFrom.getValue() != null) {
                if (dto.getData().isBefore(dateFrom.getValue())) {
                    dateFromMatch = false;
                }
            }

            // Filtr daty Do
            boolean dateToMatch = true;
            if (dateTo.getValue() != null) {
                if (dto.getData().isAfter(dateTo.getValue())) {
                    dateToMatch = false;
                }
            }

            return specMatch && dateFromMatch && dateToMatch;
        });
    }
}