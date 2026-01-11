package com.example.views;

import com.example.data.HarmonogramDTO;
import com.example.services.DoctorService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;

@Route(value = "realizacja", layout = MainLayout.class)
@PageTitle("Realizacja Wizyty")
public class VisitExecutionView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final DoctorService doctorService = new DoctorService();

    private int terminId;
    private int currentPatientId;

    // Pola Pacjenta
    private TextField nameField = new TextField("Imię i Nazwisko");
    private TextField peselField = new TextField("PESEL");
    private TextField phoneField = new TextField("Telefon");
    private TextField emailField = new TextField("E-mail");

    // Pola Wizyty
    private TextField reasonField = new TextField("Powód wizyty (zgłoszenie pacjenta)"); // NOWE POLE
    private TextArea notesField = new TextArea("Notatka Lekarska / Zalecenia"); // TO JEST NOTATKA

    private Button historyBtn = new Button("Historia Pacjenta", VaadinIcon.ARCHIVE.create());

    public VisitExecutionView() {
        setPadding(true);
        setMaxWidth("900px");

        // 1. Nagłówek
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.add(new H2("Karta Pacjenta"));
        header.add(historyBtn);
        add(header);

        // 2. Dane pacjenta
        FormLayout patientForm = new FormLayout();
        nameField.setReadOnly(true);
        peselField.setReadOnly(true);
        phoneField.setReadOnly(true);
        emailField.setReadOnly(true);

        patientForm.add(nameField, peselField, phoneField, emailField);
        patientForm.setColspan(nameField, 2);
        add(patientForm);

        // 3. Sekcja medyczna
        add(new H4("Realizacja wizyty"));

        // Wyświetlenie powodu wizyty (tylko do odczytu dla lekarza)
        reasonField.setReadOnly(true);
        reasonField.setWidthFull();
        add(reasonField);

        // Pole na notatki lekarza (do edycji)
        notesField.setWidthFull();
        notesField.setMinHeight("250px");
        notesField.setPlaceholder("Tu wpisz wywiad lekarski, wyniki badania przedmiotowego i zalecenia...");
        add(notesField);

        // 4. Przyciski
        Button btnOdbyta = new Button("Zakończ (Odbyta)", e -> save("Odbyta"));
        btnOdbyta.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnNieodbyta = new Button("Nieobecność (Nieodbyta)", e -> save("Nieodbyta"));
        btnNieodbyta.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button btnWroc = new Button("Anuluj / Wróć", e -> UI.getCurrent().navigate(DashboardView.class));
        btnWroc.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(btnOdbyta, btnNieodbyta, btnWroc);
        add(actions);

        // Akcja historii
        historyBtn.addClickListener(e -> showHistory());
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.terminId = parameter;
        loadVisitData();
    }

    private void loadVisitData() {
        try {
            HarmonogramDTO dto = doctorService.getVisitDetails(terminId);

            if (dto != null) {
                // Dane osobowe
                nameField.setValue(dto.getImiePacjenta() + " " + dto.getNazwiskoPacjenta());
                peselField.setValue(dto.getPesel() != null ? dto.getPesel() : "Brak");
                phoneField.setValue(dto.getTelefon() != null ? dto.getTelefon() : "-");
                emailField.setValue(dto.getEmail() != null ? dto.getEmail() : "-");

                // --- POWÓD WIZYTY (Dlaczego przyszedł) ---
                if (dto.getPowodWizyty() != null) {
                    reasonField.setValue(dto.getPowodWizyty());
                } else {
                    reasonField.setValue("Brak danych o powodzie");
                }

                // --- NOTATKA (Co lekarz już wpisał, np. przy edycji) ---
                if (dto.getNotatkaLekarza() != null) {
                    notesField.setValue(dto.getNotatkaLekarza());
                }

                this.currentPatientId = dto.getIdPacjenta();
            } else {
                Notification.show("Błąd: Nie znaleziono danych wizyty").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (SQLException e) {
            Notification.show("Błąd bazy: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void save(String status) {
        try {
            // Zapisujemy to, co jest w notesField jako notatkę lekarską
            doctorService.completeVisit(terminId, notesField.getValue(), status);
            Notification.show("Zapisano wizytę!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(DashboardView.class);
        } catch (SQLException e) {
            Notification.show("Błąd zapisu: " + e.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showHistory() {
        if (currentPatientId == 0) return;

        Dialog dialog = new Dialog();
        dialog.setWidth("1100px"); // Szerokie okno, żeby pomieścić kolumny
        dialog.setHeaderTitle("Historia leczenia: " + nameField.getValue());

        Grid<HarmonogramDTO> hGrid = new Grid<>();

        // 1. Data
        hGrid.addColumn(HarmonogramDTO::getData)
                .setHeader("Data")
                .setAutoWidth(true)
                .setSortable(true);

        // 2. Lekarz
        hGrid.addColumn(HarmonogramDTO::getLekarz)
                .setHeader("Lekarz")
                .setAutoWidth(true);

        // 3. Powód wizyty (Tylko powód, skracany jeśli bardzo długi)
        hGrid.addColumn(dto -> {
            String text = dto.getPowodWizyty();
            return (text != null && text.length() > 40) ? text.substring(0, 40) + "..." : text;
        }).setHeader("Powód wizyty").setFlexGrow(1);

        // 4. Przycisk Szczegóły (otwiera notatkę i pełny powód)
        hGrid.addComponentColumn(dto -> {
            Button btnDetails = new Button("Notatka / Szczegóły", VaadinIcon.FILE_TEXT_O.create());
            btnDetails.addThemeVariants(ButtonVariant.LUMO_SMALL);
            btnDetails.addClickListener(e -> showHistoryDetails(dto));
            return btnDetails;
        }).setHeader("Dokumentacja").setAutoWidth(true);

        try {
            hGrid.setItems(doctorService.getPatientHistory(currentPatientId));
        } catch (SQLException e) {
            e.printStackTrace();
            Notification.show("Błąd pobierania historii");
        }

        dialog.add(hGrid);
        dialog.add(new Button("Zamknij", e -> dialog.close()));
        dialog.open();
    }

    // Nowe okno szczegółów z dwoma polami
    private void showHistoryDetails(HarmonogramDTO dto) {
        Dialog detailsDialog = new Dialog();
        detailsDialog.setHeaderTitle("Szczegóły wizyty: " + dto.getData());
        detailsDialog.setWidth("700px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Pole 1: Lekarz
        TextField doctorField = new TextField("Lekarz przyjmujący");
        doctorField.setValue(dto.getLekarz());
        doctorField.setReadOnly(true);
        doctorField.setWidthFull();

        // Pole 2: Pełny powód wizyty
        TextArea reasonArea = new TextArea("Zgłoszony powód wizyty");
        reasonArea.setValue(dto.getPowodWizyty());
        reasonArea.setReadOnly(true);
        reasonArea.setWidthFull();
        reasonArea.setMinHeight("80px"); // Mniejsze, bo powód zazwyczaj jest krótki

        // Pole 3: Notatka lekarska / Zalecenia
        TextArea notesArea = new TextArea("Przebieg wizyty i zalecenia (Notatka)");
        notesArea.setValue(dto.getNotatkaLekarza());
        notesArea.setReadOnly(true);
        notesArea.setWidthFull();
        notesArea.setMinHeight("250px"); // Duże, na właściwy opis leczenia

        layout.add(doctorField, reasonArea, notesArea);

        detailsDialog.add(layout);
        detailsDialog.add(new Button("Zamknij", e -> detailsDialog.close()));
        detailsDialog.open();
    }
}