package com.example.views;

import com.example.services.AuthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("register")
public class RegisterView extends VerticalLayout {

    private final AuthService authService = new AuthService();

    public RegisterView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H2 header = new H2("Rejestracja Pacjenta");

        TextField imieField = new TextField("Imię");
        imieField.setRequired(true);

        TextField nazwiskoField = new TextField("Nazwisko");
        nazwiskoField.setRequired(true);

        TextField peselField = new TextField("PESEL");
        peselField.setRequired(true);
        peselField.setMinLength(11);
        peselField.setMaxLength(11);
        peselField.setAllowedCharPattern("[0-9]");
        peselField.setErrorMessage("PESEL musi składać się z 11 cyfr");

        TextField adresField = new TextField("Adres zamieszkania");
        adresField.setPlaceholder("Ulica, nr domu, kod pocztowy, miasto");
        adresField.setRequired(true);

        TextField loginField = new TextField("Login");
        loginField.setRequired(true);

        PasswordField passwordField = new PasswordField("Hasło");
        passwordField.setRequired(true);
        passwordField.setHelperText("Min. 8 znaków, 1 litera, 1 cyfra");

        TextField telefonField = new TextField("Telefon");
        telefonField.setMaxLength(16);
        telefonField.setAllowedCharPattern("[0-9+]");
        telefonField.setHelperText("Np. 123456789 lub +48...");

        EmailField emailField = new EmailField("E-mail");
        emailField.setErrorMessage("Podaj poprawny adres e-mail");
        emailField.setRequired(true);

        Button registerButton = new Button("Zarejestruj");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();

        registerButton.addClickListener(e -> {
            if (imieField.isEmpty() || nazwiskoField.isEmpty() ||
                    peselField.isEmpty() || adresField.isEmpty() ||
                    loginField.isEmpty() || passwordField.isEmpty() || emailField.isEmpty()) {

                Notification.show("Uzupełnij wszystkie wymagane pola!", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String passwordVal = passwordField.getValue();
            // Sprawdzenie długości (min 8), występowania litery i cyfry
            if (passwordVal.length() < 8 || !passwordVal.matches(".*[a-zA-Z].*") || !passwordVal.matches(".*\\d.*")) {
                Notification.show("Hasło musi mieć min. 8 znaków, literę i cyfrę!", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (peselField.isInvalid() || emailField.isInvalid()) {
                Notification.show("Popraw błędy w formularzu", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if(telefonField.isEmpty() || !telefonField.getValue().matches("^[+]?[0-9]{9,15}$")) {
                Notification.show("Podaj poprawny numer telefonu (9-15 cyfr)", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                authService.registerUser(
                        imieField.getValue(),
                        nazwiskoField.getValue(),
                        peselField.getValue(),
                        adresField.getValue(),
                        loginField.getValue(),
                        passwordField.getValue(),
                        telefonField.getValue(),
                        emailField.getValue()
                );

                Notification.show("Rejestracja udana! Możesz się zalogować.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("login");

            } catch (AuthService.ValidationException ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);

            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Wystąpił błąd podczas rejestracji.", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button backBtn = new Button("Anuluj", e -> UI.getCurrent().navigate(""));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(
                imieField, nazwiskoField,
                peselField, adresField,
                loginField, passwordField,
                telefonField, emailField
        );

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(adresField, 2);
        formLayout.setMaxWidth("600px");

        VerticalLayout formContainer = new VerticalLayout(header, formLayout, registerButton, backBtn);
        formContainer.setAlignItems(Alignment.CENTER);
        formContainer.setMaxWidth("600px");

        add(formContainer);
    }
}