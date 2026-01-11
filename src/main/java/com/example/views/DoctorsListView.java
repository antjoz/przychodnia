package com.example.views;

import com.example.data.UserDTO;
import com.example.services.ReceptionService;
import com.example.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "doctors-list", layout = MainLayout.class)
@PageTitle("Lista Lekarzy")
public class DoctorsListView extends VerticalLayout {

    private final ReceptionService receptionService = new ReceptionService();
    private Grid<UserDTO> grid = new Grid<>(UserDTO.class, false);
    private TextField searchField = new TextField();
    private ComboBox<String> specializationFilter = new ComboBox<>();

    private List<UserDTO> allDoctors; // Przechowujemy listę lokalnie do filtrowania

    public DoctorsListView() {
        UserSession user = UserSession.getLoggedInUser();
        if (user == null || !"Rejestracja".equals(user.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        setPadding(true);

        configureGrid();
        configureFilters();

        add(new H2("Lista Lekarzy"), new HorizontalLayout(searchField, specializationFilter), grid);

        updateList();
    }

    private void configureFilters() {
        searchField.setPlaceholder("Szukaj...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterList());

        specializationFilter.setPlaceholder("Wybierz specjalizację");
        try {
            specializationFilter.setItems(receptionService.getAllSpecializations());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        specializationFilter.addValueChangeListener(e -> filterList());
        specializationFilter.setClearButtonVisible(true);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(UserDTO::getImie).setHeader("Imię");
        grid.addColumn(UserDTO::getNazwisko).setHeader("Nazwisko").setSortable(true);
        grid.addColumn(UserDTO::getSpecjalizacja).setHeader("Specjalizacja");

        // Przycisk akcji
        grid.addComponentColumn(doctor -> {
            Button scheduleBtn = new Button("Grafik", VaadinIcon.CALENDAR.create());
            scheduleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            scheduleBtn.addClickListener(e ->
                    // NAWIGACJA Z PARAMETREM ID
                    getUI().ifPresent(ui -> ui.navigate(ReceptionScheduleView.class, doctor.getId()))
            );
            return scheduleBtn;
        }).setHeader("Akcja");
    }

    private void updateList() {
        try {
            allDoctors = receptionService.getAllDoctors();
            grid.setItems(allDoctors);
        } catch (SQLException e) {
            Notification.show("Błąd pobierania lekarzy: " + e.getMessage());
        }
    }

    private void filterList() {
        String searchTerm = searchField.getValue().trim().toLowerCase();
        String specFilter = specializationFilter.getValue();

        List<UserDTO> filtered = allDoctors.stream()
                .filter(doctor -> {
                    boolean matchesName = doctor.getNazwisko().toLowerCase().contains(searchTerm) ||
                            doctor.getImie().toLowerCase().contains(searchTerm);
                    boolean matchesSpec = specFilter == null || specFilter.isEmpty() ||
                            specFilter.equals(doctor.getSpecjalizacja());
                    return matchesName && matchesSpec;
                })
                .collect(Collectors.toList());

        grid.setItems(filtered);
    }
}