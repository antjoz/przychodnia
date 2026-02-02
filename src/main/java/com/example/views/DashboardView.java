package com.example.views;

import com.example.data.HarmonogramDTO;
import com.example.security.UserSession;
import com.example.services.DoctorService;
import com.example.services.ReceptionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "panel", layout = MainLayout.class)
@PageTitle("Pulpit")
public class DashboardView extends VerticalLayout {

    private final ReceptionService receptionService = new ReceptionService();
    private final DoctorService doctorService = new DoctorService();

    private Grid<HarmonogramDTO> receptionGrid;
    private Grid<HarmonogramDTO> patientActionGrid;

    public DashboardView() {
        UserSession user = UserSession.getLoggedInUser();

        if (user == null) return;

        setAlignItems(Alignment.START);
        setPadding(true);
        setSpacing(true);

        add(new H2("Witaj w systemie, " + user.getImie() + "!"));

        if ("Rejestracja".equals(user.getRola()) || "Admin".equals(user.getRola())) {
            createReceptionDashboard();
        }
        else if ("Pacjent".equals(user.getRola())) {
            createPatientDashboard(user.getId());
        }
        else if ("Lekarz".equals(user.getRola())) {
            createDoctorDashboard(user.getId());
        }
    }

    private void createReceptionDashboard() {
        add(new H3("Wizyty oczekujące na akceptację Rejestracji"));
        add(new Paragraph("Zatwierdź termin, aby przekazać go do ostatecznego potwierdzenia przez pacjenta."));

        receptionGrid = new Grid<>();
        receptionGrid.addColumn(HarmonogramDTO::getData).setHeader("Data").setAutoWidth(true);
        receptionGrid.addColumn(HarmonogramDTO::getGodzina).setHeader("Godzina").setAutoWidth(true);
        receptionGrid.addColumn(dto -> dto.getImiePacjenta() + " " + dto.getNazwiskoPacjenta()).setHeader("Pacjent").setAutoWidth(true);
        receptionGrid.addColumn(HarmonogramDTO::getLekarz).setHeader("Lekarz").setAutoWidth(true);

        receptionGrid.addComponentColumn(dto -> {
            Button confirmBtn = new Button("Akceptuj", VaadinIcon.ARROW_RIGHT.create());
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            confirmBtn.addClickListener(e -> handleReceptionDecision(dto.getIdTerminu(), true));

            Button rejectBtn = new Button("Odrzuć", VaadinIcon.CLOSE.create());
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            rejectBtn.addClickListener(e -> handleReceptionDecision(dto.getIdTerminu(), false));

            return new HorizontalLayout(confirmBtn, rejectBtn);
        }).setHeader("Decyzja");

        add(receptionGrid);
        refreshReceptionGrid();
    }

    private void handleReceptionDecision(int terminId, boolean accepted) {
        try {
            if (accepted) {
                receptionService.updateAppointmentStatus(terminId, "Wymaga potwierdzenia przez pacjenta");
                Notification.show("Zaakceptowano wstępnie. Oczekuje na potwierdzenie pacjenta.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                receptionService.updateAppointmentStatus(terminId, "Anulowana");
                Notification.show("Rezerwacja została odrzucona.").addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            }
            refreshReceptionGrid();
        } catch (SQLException e) {
            Notification.show("Błąd: " + e.getMessage());
        }
    }

    private void refreshReceptionGrid() {
        try {
            receptionGrid.setItems(receptionService.getPendingReservations());
        } catch (SQLException e) {
            Notification.show("Błąd pobierania danych: " + e.getMessage());
        }
    }

    private void createPatientDashboard(int patientId) {
        add(new H3("Wizyty wymagające Twojego potwierdzenia"));
        add(new Paragraph("Rejestracja zaakceptowała termin. Kliknij 'Potwierdź', aby sfinalizować wizytę."));

        patientActionGrid = new Grid<>();
        patientActionGrid.addColumn(HarmonogramDTO::getData).setHeader("Data").setAutoWidth(true);
        patientActionGrid.addColumn(HarmonogramDTO::getGodzina).setHeader("Godzina").setAutoWidth(true);
        patientActionGrid.addColumn(HarmonogramDTO::getLekarz).setHeader("Lekarz").setAutoWidth(true);

        patientActionGrid.addComponentColumn(dto -> {
            Button confirmBtn = new Button("Potwierdź", VaadinIcon.CHECK_CIRCLE.create());
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            confirmBtn.addClickListener(e -> handlePatientDecision(dto.getIdTerminu(), true, patientId));

            Button cancelBtn = new Button("Rezygnuję", VaadinIcon.CLOSE_CIRCLE.create());
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            cancelBtn.addClickListener(e -> handlePatientDecision(dto.getIdTerminu(), false, patientId));

            return new HorizontalLayout(confirmBtn, cancelBtn);
        }).setHeader("Twoja decyzja");

        add(patientActionGrid);
        refreshPatientGrid(patientId);
    }

    private void handlePatientDecision(int terminId, boolean confirmed, int patientId) {
        try {
            if (confirmed) {
                receptionService.updateAppointmentStatus(terminId, "Potwierdzona");
                Notification.show("Wizyta potwierdzona! Do zobaczenia.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                receptionService.updateAppointmentStatus(terminId, "Anulowana");
                Notification.show("Zrezygnowano z wizyty.").addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            }
            refreshPatientGrid(patientId);
        } catch (SQLException e) {
            Notification.show("Błąd: " + e.getMessage());
        }
    }

    private void refreshPatientGrid(int patientId) {
        try {
            List<HarmonogramDTO> all = receptionService.getPatientReservations(patientId);

            List<HarmonogramDTO> actionRequired = all.stream()
                    .filter(dto -> "Wymaga potwierdzenia przez pacjenta".equals(dto.getStatus()))
                    .collect(Collectors.toList());

            patientActionGrid.setItems(actionRequired);

            patientActionGrid.setVisible(!actionRequired.isEmpty());

            if (actionRequired.isEmpty()) {
                add(new Paragraph("Brak wizyt wymagających Twojej uwagi."));
            }

        } catch (SQLException e) {
            Notification.show("Błąd pobierania danych: " + e.getMessage());
        }
    }

    private void createDoctorDashboard(int doctorId) {
        add(new H3("Dzisiejsze wizyty (" + java.time.LocalDate.now() + ")"));

        Grid<HarmonogramDTO> todayGrid = new Grid<>();
        todayGrid.addColumn(HarmonogramDTO::getGodzina).setHeader("Godzina").setAutoWidth(true);
        todayGrid.addColumn(dto -> dto.getImiePacjenta() + " " + dto.getNazwiskoPacjenta())
                .setHeader("Pacjent").setAutoWidth(true);

        todayGrid.addComponentColumn(dto -> {
            com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(dto.getStatus());
            badge.getElement().getThemeList().add("badge");
            if("Potwierdzona".equals(dto.getStatus())) badge.getElement().getThemeList().add("success");
            else if("Odbyta".equals(dto.getStatus())) badge.getElement().getThemeList().add("contrast");
            return badge;
        }).setHeader("Status");

        todayGrid.addComponentColumn(dto -> {
            Button btn = new Button("Realizuj", VaadinIcon.DOCTOR.create());
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            boolean active = "Potwierdzona".equals(dto.getStatus()) || "Odbyta".equals(dto.getStatus());
            btn.setEnabled(active);

            btn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate(VisitExecutionView.class, dto.getIdTerminu()));
            });
            return btn;
        }).setHeader("Akcja");

        add(todayGrid);

        try {
            List<HarmonogramDTO> all = doctorService.getSchedule(doctorId, java.time.LocalDate.now());
            List<HarmonogramDTO> patients = all.stream()
                    .filter(d -> !"Wolny".equals(d.getStatus()))
                    .collect(Collectors.toList());

            todayGrid.setItems(patients);
            if(patients.isEmpty()) add(new Paragraph("Brak pacjentów na dzisiaj."));
        } catch (SQLException e) {
            Notification.show("Błąd: " + e.getMessage());
        }
    }
}