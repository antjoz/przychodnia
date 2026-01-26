package com.example.views;

import com.example.security.UserSession;
import com.example.services.AuthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("login")
public class LoginView extends VerticalLayout {

    private final AuthService authService = new AuthService();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Zaloguj się");
        i18nForm.setUsername("Login");
        i18nForm.setPassword("Hasło");
        i18nForm.setSubmit("Zaloguj");
        i18nForm.setForgotPassword("Zapomniałem hasła");
        i18n.setForm(i18nForm);

        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle("Błąd logowania");
        errorMessage.setMessage("Nieprawidłowy login lub hasło.");
        i18n.setErrorMessage(errorMessage);

        LoginForm loginForm = new LoginForm();
        loginForm.setI18n(i18n);

        loginForm.addLoginListener(e -> {
            try {
                UserSession user = authService.login(e.getUsername(), e.getPassword());

                if (user != null) {
                    VaadinSession.getCurrent().setAttribute(UserSession.class, user);

                    Notification.show("Witaj, " + user.getImie() + "!", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    UI.getCurrent().navigate("panel");
                } else {
                    loginForm.setError(true);
                }
            } catch (AuthService.ValidationException ex) {
                loginForm.setError(true);
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Wystąpił błąd serwera: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button registerBtn = new Button("Nie masz konta? Zarejestruj się", e -> UI.getCurrent().navigate("register"));
        registerBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(loginForm, registerBtn);
    }
}