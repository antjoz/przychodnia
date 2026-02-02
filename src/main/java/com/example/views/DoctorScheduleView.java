package com.example.views;

import com.example.data.HarmonogramDTO;
import com.example.security.UserSession;
import com.example.services.DoctorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.time.LocalDate;

@Route(value = "harmonogram-lekarz", layout = MainLayout.class)
@PageTitle("Grafik Lekarski")
public class DoctorScheduleView extends VerticalLayout {

    private final DoctorService doctorService = new DoctorService();
    private Grid<HarmonogramDTO> grid;
    private DatePicker datePicker;

    public DoctorScheduleView() {
        UserSession user = UserSession.getLoggedInUser();
        if (user == null || !"Lekarz".equals(user.getRola())) return;

        setPadding(true);
        add(new H2("Mój Harmonogram"));

        HorizontalLayout toolbar = new HorizontalLayout();
        datePicker = new DatePicker("Wybierz datę");
        datePicker.setValue(LocalDate.now());
        datePicker.addValueChangeListener(e -> loadData(user.getId()));

        Button refresh = new Button(VaadinIcon.REFRESH.create(), e -> loadData(user.getId()));
        toolbar.add(datePicker, refresh);
        toolbar.setAlignItems(Alignment.BASELINE);
        add(toolbar);

        grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addColumn(HarmonogramDTO::getGodzina).setHeader("Godzina").setAutoWidth(true);
        grid.addColumn(dto -> dto.getImiePacjenta() != null ?
                        dto.getImiePacjenta() + " " + dto.getNazwiskoPacjenta() : "---")
                .setHeader("Pacjent");

        grid.addComponentColumn(dto -> {
            Span badge = new Span(dto.getStatus());
            badge.getElement().getThemeList().add("badge");

            String s = dto.getStatus();
            if ("Potwierdzona".equals(s)) badge.getElement().getThemeList().add("success");
            else if ("Odbyta".equals(s)) badge.getElement().getThemeList().add("contrast");
            else if ("Wolny".equals(s)) badge.getElement().getThemeList().add("primary");
            else badge.getElement().getThemeList().add("error");
            return badge;
        }).setHeader("Status");

        add(grid);
        loadData(user.getId());
    }

    private void loadData(int doctorId) {
        if (datePicker.getValue() == null) return;
        try {
            grid.setItems(doctorService.getSchedule(doctorId, datePicker.getValue()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}