package com.example.views;

import com.example.data.BookingResult;
import com.example.data.HarmonogramDTO;
import com.example.data.UserDTO;
import com.example.services.ReceptionService;
import com.example.security.UserSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Route(value = "reception-schedule", layout = MainLayout.class)
@PageTitle("Harmonogram Lekarza")
public class ReceptionScheduleView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final ReceptionService receptionService = new ReceptionService();

    // Zmienne do nawigacji tygodniowej
    private LocalDate currentWeekStart;
    private H3 weekRangeLabel;
    private HorizontalLayout scheduleContainer;
    private H2 headerTitle;

    private int currentDoctorId;

    public ReceptionScheduleView() {
        UserSession user = UserSession.getLoggedInUser();
        if (user == null || (!"Rejestracja".equals(user.getRola()) && !"Admin".equals(user.getRola()))) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        setPadding(true);

        // Startujemy od obecnego poniedziałku
        currentWeekStart = LocalDate.now().with(ChronoField.DAY_OF_WEEK, 1);

        // --- GÓRNY PASEK (Powrót + Tytuł) ---
        Button backButton = new Button("Lista lekarzy", VaadinIcon.ARROW_LEFT.create(), e ->
                getUI().ifPresent(ui -> ui.navigate(DoctorsListView.class))
        );
        headerTitle = new H2("Harmonogram");

        HorizontalLayout headerLayout = new HorizontalLayout(backButton, headerTitle);
        headerLayout.setAlignItems(Alignment.CENTER);

        // --- PASEK NAWIGACJI (Poprzedni, Data, Następny, Skocz do daty) ---
        Button prevWeekBtn = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            loadSchedule();
        });

        Button nextWeekBtn = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            loadSchedule();
        });

        weekRangeLabel = new H3();
        weekRangeLabel.getStyle().set("margin", "0 15px");

        com.vaadin.flow.component.datepicker.DatePicker jumpToDate = new com.vaadin.flow.component.datepicker.DatePicker();
        jumpToDate.setPlaceholder("Skocz do daty...");
        jumpToDate.setLocale(new Locale("pl", "PL"));
        jumpToDate.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                // Ustawiamy currentWeekStart na poniedziałek wybranego tygodnia
                currentWeekStart = e.getValue().with(ChronoField.DAY_OF_WEEK, 1);
                loadSchedule();
            }
        });

        HorizontalLayout navigationBar = new HorizontalLayout(prevWeekBtn, weekRangeLabel, nextWeekBtn, jumpToDate);
        navigationBar.setAlignItems(Alignment.CENTER);
        navigationBar.setJustifyContentMode(JustifyContentMode.CENTER);
        navigationBar.setWidthFull();

        scheduleContainer = new HorizontalLayout();
        scheduleContainer.setSizeFull();
        scheduleContainer.setSpacing(true);

        add(headerLayout, navigationBar, scheduleContainer);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer doctorId) {
        if (headerTitle == null || doctorId == null) return;

        this.currentDoctorId = doctorId;
        try {
            UserDTO doctor = receptionService.getDoctorById(currentDoctorId);
            if (doctor != null) {
                headerTitle.setText(doctor.getImie() + " " + doctor.getNazwisko() +
                        (doctor.getSpecjalizacja() != null ? " (" + doctor.getSpecjalizacja() + ")" : ""));
            }
            loadSchedule();
        } catch (SQLException e) {
            Notification.show("Błąd: " + e.getMessage());
        }
    }

    private void loadSchedule() {
        if (scheduleContainer == null) return;

        scheduleContainer.removeAll();

        LocalDate weekEnd = currentWeekStart.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        weekRangeLabel.setText(currentWeekStart.format(formatter) + " - " + weekEnd.format(formatter));

        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            VerticalLayout dayColumn = createDayColumn(dayDate);
            scheduleContainer.add(dayColumn);
            scheduleContainer.setFlexGrow(1, dayColumn);
        }
    }

    private VerticalLayout createDayColumn(LocalDate date) {
        VerticalLayout column = new VerticalLayout();
        column.setPadding(false);
        column.setSpacing(true);
        column.setAlignItems(Alignment.STRETCH);

        String dayName = translateDayOfWeek(date.getDayOfWeek());
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM"));

        Div headerBox = new Div();
        headerBox.getStyle()
                .set("background-color", "#f5f5f5")
                .set("padding", "10px")
                .set("text-align", "center")
                .set("border-bottom", "2px solid #ddd")
                .set("font-weight", "bold");

        Span daySpan = new Span(dayName);
        Span dateSpan = new Span(dateStr);
        daySpan.getStyle().set("display", "block");

        headerBox.add(daySpan, dateSpan);
        column.add(headerBox);

        try {
            List<HarmonogramDTO> slots = receptionService.getScheduleForDoctor(currentDoctorId, date);

            if (slots.isEmpty()) {
                Div emptyInfo = new Div(new Span("-"));
                emptyInfo.getStyle().set("text-align", "center").set("color", "#999");
                column.add(emptyInfo);
            } else {
                for (HarmonogramDTO slot : slots) {
                    column.add(createSlotCard(slot));
                }
            }
        } catch (SQLException e) {
            column.add(new Span("Błąd!"));
        }

        return column;
    }

    private Component createSlotCard(HarmonogramDTO slot) {
        Div card = new Div();

        card.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("padding", "8px")
                .set("cursor", "pointer")
                .set("text-align", "center")
                .set("margin-bottom", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        boolean isTaken = !"Wolny".equalsIgnoreCase(slot.getStatus());

        if (isTaken) {
            card.getStyle()
                    .set("background-color", "#ffebee")
                    .set("border-color", "#ef9a9a");
        } else {
            card.getStyle()
                    .set("background-color", "#e8f5e9")
                    .set("border-color", "#a5d6a7");
        }

        H5 timeHeader = new H5(slot.getGodzina().toString());
        timeHeader.getStyle().set("margin", "0 0 5px 0");

        card.add(timeHeader);
        card.add(new Span(isTaken ? "Zajęty" : "Wolny"));

        card.addClickListener(e -> {
            if (isTaken) showAppointmentDetails(slot);
            else openBookingDialog(slot);
        });
        return card;
    }

    private void openBookingDialog(HarmonogramDTO slot) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Umów wizytę: " + slot.getGodzina());
        dialog.setWidth("650px");

        VerticalLayout contentLayout = new VerticalLayout();
        RadioButtonGroup<String> modeSelect = new RadioButtonGroup<>();
        modeSelect.setLabel("Wybierz pacjenta");
        modeSelect.setItems("Wybierz z listy", "Nowy pacjent");
        modeSelect.setValue("Wybierz z listy");

        VerticalLayout existingPatientLayout = new VerticalLayout();
        existingPatientLayout.setPadding(false);
        ComboBox<UserDTO> patientCombo = new ComboBox<>("Szukaj pacjenta");
        patientCombo.setWidthFull();
        patientCombo.setItemLabelGenerator(u -> u.getImie() + " " + u.getNazwisko() + " (" + u.getPesel() + ")");
        try {
            patientCombo.setItems(receptionService.getAllPatients());
        } catch (SQLException e) { /* Ignored */ }
        existingPatientLayout.add(patientCombo);

        FormLayout newPatientForm = new FormLayout();
        newPatientForm.setVisible(false);

        TextField imieField = new TextField("Imię");
        TextField nazwiskoField = new TextField("Nazwisko");

        TextField peselField = new TextField("PESEL");
        peselField.setMaxLength(11);
        peselField.setHelperText("Dokładnie 11 cyfr");
        peselField.setValueChangeMode(ValueChangeMode.EAGER);
        peselField.addValueChangeListener(e -> {
            if (!e.getValue().matches("\\d*")) peselField.setValue(e.getOldValue());
        });

        TextField telField = new TextField("Telefon");
        telField.setMaxLength(15);
        telField.setValueChangeMode(ValueChangeMode.EAGER);

        EmailField emailField = new EmailField("Email");
        TextField adresField = new TextField("Adres");

        newPatientForm.add(imieField, nazwiskoField, peselField, telField, emailField, adresField);

        modeSelect.addValueChangeListener(e -> {
            boolean isNew = "Nowy pacjent".equals(e.getValue());
            existingPatientLayout.setVisible(!isNew);
            newPatientForm.setVisible(isNew);
        });

        new Hr();
        ComboBox<String> reasonCombo = new ComboBox<>("Powód wizyty");
        reasonCombo.setWidthFull();
        TextArea customReasonField = new TextArea("Opisz powód wizyty");
        customReasonField.setWidthFull();
        customReasonField.setVisible(false);

        Map<Integer, String> reasonsMap;
        try {
            reasonsMap = receptionService.getVisitReasons(currentDoctorId);
            reasonCombo.setItems(reasonsMap.values());
        } catch (SQLException e) {
            reasonsMap = Map.of();
            Notification.show("Błąd SQL: " + e.getMessage());
        }
        final Map<Integer, String> finalReasonsMap = reasonsMap;

        reasonCombo.addValueChangeListener(e -> {
            String selected = e.getValue();
            customReasonField.setVisible(selected != null && selected.contains("Inny"));
        });

        Button saveButton = new Button("Zatwierdź", e -> {
            try {
                Integer reasonId = null;
                if (reasonCombo.getValue() != null) {
                    for (Map.Entry<Integer, String> entry : finalReasonsMap.entrySet()) {
                        if (entry.getValue().equals(reasonCombo.getValue())) {
                            reasonId = entry.getKey();
                            break;
                        }
                    }
                }
                if (reasonId == null) {
                    Notification.show("Wybierz powód wizyty"); return;
                }

                String customNote = customReasonField.getValue();
                BookingResult result;

                // W metodzie openBookingDialog -> wewnątrz listenera saveButton:

                if ("Wybierz z listy".equals(modeSelect.getValue())) {
                    // ...
                    result = receptionService.bookAppointment(
                            slot.getIdTerminu(), patientCombo.getValue().getId(),
                            null, null, null, null, null, null,
                            reasonId, customNote,
                            "Potwierdzona" // <--- DODAJEMY STATUS DLA REJESTRACJI
                    );
                } else {
                    // ...
                    result = receptionService.bookAppointment(
                            slot.getIdTerminu(), null,
                            imieField.getValue(), nazwiskoField.getValue(), peselField.getValue(),
                            telField.getValue(), emailField.getValue(), adresField.getValue(),
                            reasonId, customNote,
                            "Potwierdzona" // <--- DODAJEMY STATUS DLA REJESTRACJI
                    );
                }

                dialog.close();
                loadSchedule();
                if (result != null && result.getGeneratedLogin() != null) {
                    showCredentialsDialog(result.getGeneratedLogin(), result.getGeneratedPassword());
                } else {
                    Notification.show("Wizyta umówiona!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }

            } catch (Exception ex) {
                Notification.show("Błąd: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new VerticalLayout(modeSelect, existingPatientLayout, newPatientForm, reasonCombo, customReasonField));
        dialog.getFooter().add(new Button("Anuluj", ev -> dialog.close()), saveButton);
        dialog.open();
    }

    private void showCredentialsDialog(String login, String password) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Konto Pacjenta");
        TextField l = new TextField("Login"); l.setValue(login); l.setReadOnly(true); l.setWidthFull();
        TextField p = new TextField("Hasło"); p.setValue(password); p.setReadOnly(true); p.setWidthFull();
        dialog.add(new VerticalLayout(new Span("Przekaż dane pacjentowi:"), l, p));
        dialog.getFooter().add(new Button("OK", e -> dialog.close()));
        dialog.open();
    }

    private void showAppointmentDetails(HarmonogramDTO slot) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Szczegóły wizyty: " + slot.getData() + " " + slot.getGodzina());
        dialog.setWidth("500px");

        try {
            HarmonogramDTO details = receptionService.getAppointmentDetails(slot.getIdTerminu());

            if (details == null) {
                Notification.show("Brak wizyty.");
                return;
            }

            // 1. Pola informacyjne
            TextField patientName = new TextField("Pacjent");
            patientName.setValue(details.getImiePacjenta() + " " + details.getNazwiskoPacjenta());
            patientName.setReadOnly(true);
            patientName.setWidthFull();

            TextField peselField = new TextField("PESEL");
            peselField.setValue(details.getPesel() != null ? details.getPesel() : "Brak");
            peselField.setReadOnly(true);
            peselField.setWidthFull();

            TextField phoneField = new TextField("Telefon");
            phoneField.setValue(details.getTelefon() != null ? details.getTelefon() : "Brak");
            phoneField.setReadOnly(true);
            phoneField.setWidthFull();

            // --- POPRAWKA: Dodajemy pole wyświetlające aktualny status ---
            TextField currentStatusField = new TextField("Aktualny status");
            currentStatusField.setValue(details.getStatus() != null ? details.getStatus() : "Brak");
            currentStatusField.setReadOnly(true);
            currentStatusField.setWidthFull();
            currentStatusField.addThemeNames("small"); // Opcjonalnie mniejsza czcionka

            // 2. Status wizyty (Wyświetlanie i Edycja)
            Select<String> statusSelect = new Select<>();
            statusSelect.setLabel("Zmień status na");

            // --- POPRAWKA: Lista musi zawierać WSZYSTKIE możliwe statusy, żeby Select mógł wyświetlić aktualny ---
            statusSelect.setItems(
                    "Wymaga potwierdzenia przez pacjenta",
                    "Anulowana"
            );

            // Ustawienie wartości z DTO
            if (details.getStatus() != null) {
                statusSelect.setValue(details.getStatus());
            }
            statusSelect.setWidthFull();

            // --- PRZYCISKI AKCJI ---

            Button saveStatusBtn = new Button("Zapisz status", e -> {
                try {
                    receptionService.updateAppointmentStatus(slot.getIdTerminu(), statusSelect.getValue());
                    Notification.show("Status zaktualizowany").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                    loadSchedule();
                } catch (SQLException ex) {
                    Notification.show("Błąd aktualizacji: " + ex.getMessage());
                }
            });
            saveStatusBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            // Przycisk "Przełóż wizytę"
            Button rescheduleBtn = new Button("Przełóż wizytę", VaadinIcon.CALENDAR_CLOCK.create(), e -> {
                dialog.close();
                openRescheduleDialog(details.getIdRezerwacji(), slot);
            });
            rescheduleBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

            // Blokada przycisku "Przełóż", jeśli wizyta jest już odbyta/zrealizowana
            String currentStatus = details.getStatus();
            boolean isCompleted = "Odbyta".equalsIgnoreCase(currentStatus) ||
                    "Zrealizowana".equalsIgnoreCase(currentStatus);

            if (isCompleted) {
                rescheduleBtn.setEnabled(false);
                rescheduleBtn.setText("Wizyta zakończona");
            }

            HorizontalLayout buttonsLayout = new HorizontalLayout(saveStatusBtn, rescheduleBtn);
            buttonsLayout.setSpacing(true);

            // Dodajemy currentStatusField do layoutu
            dialog.add(new VerticalLayout(patientName, peselField, phoneField, currentStatusField, statusSelect));
            dialog.getFooter().add(new Button("Zamknij", e -> dialog.close()));
            dialog.getFooter().add(buttonsLayout);

            dialog.open();

        } catch (SQLException e) {
            Notification.show("Błąd pobierania danych: " + e.getMessage());
        }
    }

    private void openRescheduleDialog(int reservationId, HarmonogramDTO oldSlot) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zmiana terminu wizyty");
        dialog.setWidth("450px");

        VerticalLayout layout = new VerticalLayout();

        Span oldInfo = new Span("Obecny termin: " + oldSlot.getData() + " godz. " + oldSlot.getGodzina());
        oldInfo.getStyle().set("font-weight", "bold").set("color", "gray");

        com.vaadin.flow.component.datepicker.DatePicker newDatePicker = new com.vaadin.flow.component.datepicker.DatePicker("Wybierz nową datę");
        newDatePicker.setValue(LocalDate.now());
        newDatePicker.setWidthFull();

        ComboBox<HarmonogramDTO> newTimeCombo = new ComboBox<>("Dostępne godziny");
        newTimeCombo.setWidthFull();
        newTimeCombo.setEnabled(false);

        newTimeCombo.setItemLabelGenerator(dto -> dto.getGodzina().toString());

        newDatePicker.addValueChangeListener(e -> {
            LocalDate selectedDate = e.getValue();
            if (selectedDate == null) {
                newTimeCombo.clear();
                newTimeCombo.setEnabled(false);
                return;
            }

            try {
                List<HarmonogramDTO> daySlots = receptionService.getScheduleForDoctor(currentDoctorId, selectedDate);
                List<HarmonogramDTO> freeSlots = daySlots.stream()
                        .filter(s -> "Wolny".equalsIgnoreCase(s.getStatus()))
                        .toList();

                if (freeSlots.isEmpty()) {
                    Notification.show("Brak wolnych terminów w tym dniu.");
                    newTimeCombo.clear();
                    newTimeCombo.setEnabled(false);
                } else {
                    newTimeCombo.setItems(freeSlots);
                    newTimeCombo.setEnabled(true);
                }
            } catch (SQLException ex) {
                Notification.show("Błąd bazy danych: " + ex.getMessage());
            }
        });

        Button confirmBtn = new Button("Zatwierdź zmianę", e -> {
            HarmonogramDTO selectedSlot = newTimeCombo.getValue();
            if (selectedSlot == null) {
                Notification.show("Wybierz godzinę!");
                return;
            }

            try {
                receptionService.rescheduleAppointment(reservationId, selectedSlot.getIdTerminu());
                Notification.show("Termin zmieniony pomyślnie!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                loadSchedule();
            } catch (ReceptionService.ValidationException | SQLException ex) {
                Notification.show("Błąd: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(oldInfo, newDatePicker, newTimeCombo);
        dialog.add(layout);
        dialog.getFooter().add(new Button("Anuluj", e -> dialog.close()));
        dialog.getFooter().add(confirmBtn);

        dialog.open();
    }

    private String translateDayOfWeek(DayOfWeek day) {
        switch (day) {
            case MONDAY: return "Poniedziałek";
            case TUESDAY: return "Wtorek";
            case WEDNESDAY: return "Środa";
            case THURSDAY: return "Czwartek";
            case FRIDAY: return "Piątek";
            case SATURDAY: return "Sobota";
            case SUNDAY: return "Niedziela";
            default: return "";
        }
    }
}