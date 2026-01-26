package com.example.views;

import com.example.data.BookingResult;
import com.example.data.HarmonogramDTO;
import com.example.data.UserDTO;
import com.example.security.UserSession;
import com.example.services.ReceptionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "patient-booking", layout = MainLayout.class)
@PageTitle("Umów wizytę")
public class PatientBookingView extends VerticalLayout {

    private final ReceptionService receptionService = new ReceptionService();

    private ComboBox<String> specializationSelect;
    private ComboBox<UserDTO> doctorSelect;
    private DatePicker datePicker;
    private FlexLayout slotsLayout;

    public PatientBookingView() {
        UserSession user = UserSession.getLoggedInUser();
        if (user == null || !"Pacjent".equals(user.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setPadding(true);
        add(new H2("Zarezerwuj nową wizytę"));

        specializationSelect = new ComboBox<>("Wybierz specjalizację");
        specializationSelect.setWidth("300px");

        doctorSelect = new ComboBox<>("Wybierz lekarza");
        doctorSelect.setWidth("300px");
        doctorSelect.setEnabled(false);
        doctorSelect.setItemLabelGenerator(u -> u.getImie() + " " + u.getNazwisko());

        datePicker = new DatePicker("Wybierz datę");
        datePicker.setMin(LocalDate.now().plusDays(1)); // Rezerwacja min. na jutro
        datePicker.setEnabled(false);

        slotsLayout = new FlexLayout();
        slotsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        slotsLayout.getStyle().set("gap", "10px");

        add(specializationSelect, doctorSelect, datePicker, slotsLayout);

        loadData();
        setupListeners();
    }

    private void loadData() {
        try {
            specializationSelect.setItems(receptionService.getAllSpecializations());
        } catch (SQLException e) {
            Notification.show("Błąd ładowania: " + e.getMessage());
        }
    }

    private void setupListeners() {
        specializationSelect.addValueChangeListener(e -> {
            doctorSelect.clear();
            doctorSelect.setEnabled(false);
            datePicker.clear();
            datePicker.setEnabled(false);
            slotsLayout.removeAll();

            if (e.getValue() != null) {
                try {
                    List<UserDTO> allDoctors = receptionService.getAllDoctors();
                    List<UserDTO> filtered = allDoctors.stream()
                            .filter(d -> e.getValue().equals(d.getSpecjalizacja()))
                            .collect(Collectors.toList());
                    doctorSelect.setItems(filtered);
                    doctorSelect.setEnabled(true);
                } catch (SQLException ex) {
                    Notification.show("Błąd: " + ex.getMessage());
                }
            }
        });

        doctorSelect.addValueChangeListener(e -> {
            datePicker.clear();
            datePicker.setEnabled(e.getValue() != null);
            slotsLayout.removeAll();
        });

        datePicker.addValueChangeListener(e -> loadSlots());
    }

    private void loadSlots() {
        slotsLayout.removeAll();
        UserDTO doctor = doctorSelect.getValue();
        LocalDate date = datePicker.getValue();

        if (doctor == null || date == null) return;

        try {
            List<HarmonogramDTO> schedule = receptionService.getScheduleForDoctor(doctor.getId(), date);

            if (schedule.isEmpty()) {
                slotsLayout.add(new Span("Brak terminów w tym dniu."));
                return;
            }

            for (HarmonogramDTO slot : schedule) {
                if ("Wolny".equalsIgnoreCase(slot.getStatus())) {
                    Button slotBtn = new Button(slot.getGodzina().toString());
                    slotBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                    slotBtn.addClickListener(ev -> openReasonDialog(slot));
                    slotsLayout.add(slotBtn);
                }
            }
        } catch (SQLException e) {
            Notification.show("Błąd pobierania grafiku: " + e.getMessage());
        }
    }

    private void openReasonDialog(HarmonogramDTO slot) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Potwierdzenie rezerwacji: " + slot.getGodzina());

        ComboBox<String> reasonCombo = new ComboBox<>("Powód wizyty");
        reasonCombo.setWidthFull();
        TextArea customReason = new TextArea("Dodatkowy opis");
        customReason.setWidthFull();
        customReason.setVisible(false);

        try {
            Map<Integer, String> reasons = receptionService.getVisitReasons(doctorSelect.getValue().getId());
            reasonCombo.setItems(reasons.values());

            // Mapa pomocnicza ID -> Nazwa
            final Map<Integer, String> finalReasons = reasons;

            reasonCombo.addValueChangeListener(e -> {
                customReason.setVisible(e.getValue() != null && e.getValue().contains("Inny"));
            });

            Button confirmBtn = new Button("Rezerwuj", e -> {
                try {
                    Integer reasonId = null;
                    if (reasonCombo.getValue() != null) {
                        for (Map.Entry<Integer, String> entry : finalReasons.entrySet()) {
                            if (entry.getValue().equals(reasonCombo.getValue())) {
                                reasonId = entry.getKey();
                                break;
                            }
                        }
                    }

                    if (reasonId == null) {
                        Notification.show("Wybierz powód"); return;
                    }

                    receptionService.bookAppointment(
                            slot.getIdTerminu(),
                            UserSession.getLoggedInUser().getId(),
                            null, null, null, null, null, null, // Dane osobowe puste (już są w bazie)
                            reasonId,
                            customReason.getValue(),
                            "Wymaga potwierdzenia przez rejestracje"
                    );

                    Notification.show("Wizyta zarezerwowana! Oczekuj na potwierdzenie.")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                    loadSlots();

                } catch (Exception ex) {
                    Notification.show("Błąd: " + ex.getMessage());
                }
            });
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            dialog.add(new VerticalLayout(reasonCombo, customReason));
            dialog.getFooter().add(new Button("Anuluj", ev -> dialog.close()), confirmBtn);
            dialog.open();

        } catch (SQLException ex) {
            Notification.show("Błąd: " + ex.getMessage());
        }
    }
}