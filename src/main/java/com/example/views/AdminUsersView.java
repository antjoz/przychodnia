package com.example.views;

import com.example.data.UserDTO;
import com.example.data.SpecjalizacjaDTO;
import com.example.services.AdminService;
import com.example.security.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;

@Route(value = "admin-users", layout = MainLayout.class)
@PageTitle("Zarządzanie Użytkownikami")
public class AdminUsersView extends VerticalLayout {

    private final AdminService adminService = new AdminService();
    private final Grid<UserDTO> grid = new Grid<>(UserDTO.class);

    private TextField searchField;
    private ComboBox<String> roleFilter;
    private GridListDataView<UserDTO> dataView;

    public AdminUsersView() {
        UserSession currentUser = UserSession.getLoggedInUser();
        if (currentUser == null || !"Admin".equals(currentUser.getRola())) {
            add(new H2("Brak dostępu"));
            return;
        }

        setSizeFull();
        configureGrid();

        HorizontalLayout toolbar = createToolbar();
        add(new H2("Lista Użytkowników"), toolbar, grid);

        try {
            refreshGrid();
        } catch (SQLException e) {
            Notification.show("Błąd pobierania danych: " + e.getMessage());
        }
    }

    private HorizontalLayout createToolbar() {
        searchField = new TextField();
        searchField.setPlaceholder("Szukaj");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateFilter());

        roleFilter = new ComboBox<>();
        roleFilter.setPlaceholder("Filtruj wg roli");
        roleFilter.setItems("Wszyscy", "Lekarz", "Rejestracja", "Admin", "Pacjent");
        roleFilter.setValue("Wszyscy");
        roleFilter.addValueChangeListener(e -> updateFilter());

        Button addBtn = new Button("Dodaj pracownika", VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.addClickListener(e -> openAddUserDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, roleFilter, addBtn);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setColumns();

        grid.addColumn(UserDTO::getImie).setHeader("Imię").setSortable(true).setAutoWidth(true);
        grid.addColumn(UserDTO::getNazwisko).setHeader("Nazwisko").setSortable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(user -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);


            Span emailSpan = new Span(user.getEmail());
            emailSpan.getStyle().set("text-overflow", "ellipsis");
            emailSpan.getStyle().set("white-space", "nowrap");
            emailSpan.getStyle().set("overflow", "hidden");
            emailSpan.getStyle().set("max-width", "150px");
            emailSpan.getStyle().set("display", "block");

            layout.add(emailSpan);

            if (user.getEmail() != null && user.getEmail().length() > 20) {
                Button showFullBtn = new Button(VaadinIcon.EYE.create());
                showFullBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                showFullBtn.setTooltipText("Pokaż pełny email");

                showFullBtn.addClickListener(e -> {
                    Notification notification = Notification.show(user.getEmail(), 5000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                });
                layout.add(showFullBtn);
            }

            return layout;
        })).setHeader("Email").setAutoWidth(true);

        grid.addColumn(UserDTO::getTelefon).setHeader("Telefon").setAutoWidth(true);
        grid.addColumn(UserDTO::getRola).setHeader("Stanowisko").setSortable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(user -> {
            HorizontalLayout layout = new HorizontalLayout();

            Button statusBtn = new Button(user.isCzyAktywny() ? "Aktywny" : "Zablokowany");
            if (user.isCzyAktywny()) {
                statusBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                statusBtn.setIcon(VaadinIcon.CHECK.create());
            } else {
                statusBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                statusBtn.setIcon(VaadinIcon.BAN.create());
            }

            statusBtn.addClickListener(event -> {
                try {
                    boolean nowyStatus = !user.isCzyAktywny();
                    adminService.toggleUserStatus(user.getId(), nowyStatus);
                    user.setCzyAktywny(nowyStatus);
                    grid.getDataProvider().refreshItem(user);
                    Notification.show(nowyStatus ? "Konto aktywowane" : "Konto zablokowane");
                } catch (SQLException e) {
                    Notification.show("Błąd bazy danych: " + e.getMessage());
                }
            });

            layout.add(statusBtn);

            if ("Lekarz".equals(user.getRola())) {
                Button editHoursBtn = new Button(VaadinIcon.CLOCK.create());
                editHoursBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editHoursBtn.setTooltipText("Zmień godziny przyjęć");
                editHoursBtn.addClickListener(e -> openEditHoursDialog(user));
                layout.add(editHoursBtn);
            }

            return layout;
        })).setHeader("Zarządzanie").setAutoWidth(true);
    }

    private void refreshGrid() throws SQLException {
        dataView = grid.setItems(adminService.getAllUsers());
        updateFilter();
    }

    private void updateFilter() {
        if (dataView == null) return;

        dataView.addFilter(user -> {
            String searchTerm = searchField.getValue().trim();
            boolean matchesSearch = true;

            if (!searchTerm.isEmpty()) {
                String lowerTerm = searchTerm.toLowerCase();
                boolean matchesName = user.getImie().toLowerCase().contains(lowerTerm);
                boolean matchesSurname = user.getNazwisko().toLowerCase().contains(lowerTerm);
                boolean matchesEmail = user.getEmail().toLowerCase().contains(lowerTerm);
                boolean matchesPhone = user.getTelefon() != null && user.getTelefon().contains(lowerTerm);

                matchesSearch = matchesName || matchesSurname || matchesEmail || matchesPhone;
            }

            String selectedRole = roleFilter.getValue();
            boolean matchesRole = true;
            if (selectedRole != null && !"Wszyscy".equals(selectedRole)) {
                matchesRole = selectedRole.equals(user.getRola());
            }

            return matchesSearch && matchesRole;
        });
    }

    private void openEditHoursDialog(UserDTO doctor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Grafik: " + doctor.getImie() + " " + doctor.getNazwisko());

        TimePicker newStart = new TimePicker("Nowy początek");
        newStart.setStep(Duration.ofMinutes(15));
        newStart.setMin(LocalTime.of(6, 0));
        newStart.setMax(LocalTime.of(20, 0));

        TimePicker newEnd = new TimePicker("Nowy koniec");
        newEnd.setStep(Duration.ofMinutes(15));
        newEnd.setEnabled(false);

        newStart.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                newEnd.setEnabled(true);
                newEnd.setMin(e.getValue().plusMinutes(15));
                newEnd.setMax(LocalTime.of(22, 0));
            } else {
                newEnd.setEnabled(false);
            }
        });

        Button saveBtn = new Button("Zapisz zmiany", e -> {
            if (newStart.isEmpty() || newEnd.isEmpty()) {
                Notification.show("Wybierz godziny pracy").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                adminService.updateDoctorHours(doctor.getId(), newStart.getValue(), newEnd.getValue());
                Notification.show("Grafik zaktualizowany!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                Notification.show("Błąd bazy: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(newStart, newEnd);
        dialog.add(layout);
        dialog.getFooter().add(new Button("Anuluj", e -> dialog.close()));
        dialog.getFooter().add(saveBtn);
        dialog.open();
    }

    private void openAddUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nowy Użytkownik");
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();
        form.setWidthFull();

        TextField imie = new TextField("Imię");
        TextField nazwisko = new TextField("Nazwisko");
        TextField login = new TextField("Login");

        PasswordField haslo = new PasswordField("Hasło");
        haslo.setHelperText("Min. 8 znaków, 1 litera, 1 cyfra");

        EmailField email = new EmailField("Email");

        TextField telefon = new TextField("Numer telefonu");
        telefon.setMaxLength(16);
        telefon.setAllowedCharPattern("[0-9+]");
        telefon.setHelperText("Np. 123456789 lub +48123456789");

        ComboBox<String> rola = new ComboBox<>("Rola");
        rola.setItems("Lekarz", "Rejestracja");

        ComboBox<SpecjalizacjaDTO> specjalizacja = new ComboBox<>("Specjalizacja");
        specjalizacja.setItems(adminService.getAllSpecializations());
        specjalizacja.setItemLabelGenerator(SpecjalizacjaDTO::getNazwa);

        TimePicker startPracy = new TimePicker("Początek pracy");
        startPracy.setStep(Duration.ofMinutes(15));
        TimePicker koniecPracy = new TimePicker("Koniec pracy");
        koniecPracy.setStep(Duration.ofMinutes(15));
        koniecPracy.setEnabled(false);

        startPracy.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                koniecPracy.setEnabled(true);
                koniecPracy.setMin(e.getValue().plusMinutes(15));
            }
        });

        startPracy.setVisible(false); koniecPracy.setVisible(false); specjalizacja.setVisible(false);

        rola.addValueChangeListener(e -> {
            boolean isLekarz = "Lekarz".equals(e.getValue());
            startPracy.setVisible(isLekarz);
            koniecPracy.setVisible(isLekarz);
            specjalizacja.setVisible(isLekarz);
        });

        form.add(imie, nazwisko, login, haslo, email, telefon, rola, specjalizacja, startPracy, koniecPracy);

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("350px", 2)
        );

        form.setColspan(email, 2);
        form.setColspan(login, 2);
        form.setColspan(specjalizacja, 2);

        Button saveButton = new Button("Utwórz konto", e -> {
            if (rola.isEmpty()) return;

            String passwordVal = haslo.getValue();
            if (passwordVal == null || passwordVal.length() < 8) {
                Notification.show("Hasło musi mieć co najmniej 8 znaków!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (!passwordVal.matches(".*[a-zA-Z].*") || !passwordVal.matches(".*\\d.*")) {
                Notification.show("Hasło musi zawierać co najmniej jedną literę i jedną cyfrę!")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String telVal = telefon.getValue();
            if (telVal == null || !telVal.matches("^[+]?[0-9]{9,15}$")) {
                Notification.show("Numer telefonu musi mieć 9-15 cyfr")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                if ("Lekarz".equals(rola.getValue())) {
                    if (specjalizacja.isEmpty() || startPracy.isEmpty() || koniecPracy.isEmpty()) {
                        Notification.show("Uzupełnij dane lekarza!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    adminService.registerDoctor(imie.getValue(), nazwisko.getValue(), login.getValue(), haslo.getValue(),
                            email.getValue(), telefon.getValue(), startPracy.getValue(), koniecPracy.getValue(),
                            specjalizacja.getValue().getId());
                    Notification.show("Dodano lekarza").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    adminService.registerReceptionist(imie.getValue(), nazwisko.getValue(), login.getValue(), haslo.getValue(),
                            email.getValue(), telefon.getValue());
                    Notification.show("Dodano pracownika rejestracji.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                refreshGrid();
                dialog.close();
            } catch (SQLException ex) {
                if ("LOGIN_ZAJETY".equals(ex.getMessage())) {
                    Notification.show("Ten login jest już zajęty!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    Notification.show("Błąd: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Anuluj", e -> dialog.close()));
        dialog.getFooter().add(saveButton);
        dialog.open();
    }
}