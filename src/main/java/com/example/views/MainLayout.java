package com.example.views;

import com.example.security.UserSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.example.views.ReceptionScheduleView;

public class MainLayout extends AppLayout {

    private UserSession user;

    public MainLayout() {
        this.user = UserSession.getLoggedInUser();
        if (user == null) return;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        // ... (Tutaj bez zmian, zostaw tak jak miałeś) ...
        H1 logo = new H1("Przychodnia");
        logo.addClassNames("text-l", "m-m");

        Span userInfo = new Span(user.getImie() + " (" + user.getRola() + ")");
        userInfo.addClassNames("text-s", "mr-m");

        Button logoutBtn = new Button("Wyloguj", VaadinIcon.SIGN_OUT.create(), e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().setLocation("login");
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, userInfo, logoutBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");
        header.expand(logo);

        addToNavbar(header);
    }

    // --- TO JEST CZĘŚĆ, KTÓRĄ ZMIENIAMY ---

    private void createDrawer() {
        VerticalLayout menuLayout = new VerticalLayout();

        // Ważne: Padding sprawia, że przyciski nie dotykają krawędzi ekranu
        menuLayout.setPadding(true);
        menuLayout.setSpacing(true); // Odstępy między przyciskami

        // 1. OPCJE WSPÓLNE
        menuLayout.add(createMenuButton("Pulpit", VaadinIcon.DASHBOARD.create(), DashboardView.class));

        // 2. OPCJE PACJENTA
        if ("Pacjent".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Umów wizytę", VaadinIcon.CALENDAR_CLOCK.create(), PatientBookingView.class));
            menuLayout.add(createMenuButton("Historia wizyt", VaadinIcon.FILE_TEXT.create(), PatientHistoryView.class));
        }

        // 3. OPCJE LEKARZA
        else if ("Lekarz".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Harmonogram", VaadinIcon.CALENDAR_USER.create(), DoctorScheduleView.class));
        }

        // 4. OPCJE ADMINA
        else if ("Admin".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Użytkownicy", VaadinIcon.USERS.create(), AdminUsersView.class));
            menuLayout.add(createMenuButton("Statystyki", VaadinIcon.CHART.create(), AdminStatsView.class));
        }

        else if ("Rejestracja".equals(user.getRola())) {
            // Dodajemy przycisk kierujący do naszego nowego widoku z kafelkami
            menuLayout.add(createMenuButton("Lekarze i Grafiki", VaadinIcon.DOCTOR.create(), DoctorsListView.class));
        }

        addToDrawer(menuLayout);
    }

    // --- METODA TWORZĄCA WYGLĄD PRZYCISKU ---
    private Button createMenuButton(String caption, Icon icon, Class<? extends Component> navigationTarget) {
        Button button = new Button(caption, icon);

        // 1. LUMO_PRIMARY = Kolorowe tło (niebieskie) + Biały tekst + Biała ikona
        //    LUMO_LARGE = Większy, wygodniejszy przycisk
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        // 2. Rozciągnij przycisk na całą szerokość paska
        button.setWidthFull();

        // 3. Wyrównaj tekst i ikonę do lewej strony (zamiast do środka)
        button.getStyle().set("justify-content", "flex-start");

        // 4. Dodatkowy odstęp ikony od tekstu
        icon.getElement().getStyle().set("margin-right", "15px");

        // Opcjonalnie: Jeśli chcesz ciemne przyciski (czarne/szare) zamiast niebieskich,
        // odkomentuj poniższą linię:
        // button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        button.addClickListener(e -> UI.getCurrent().navigate(navigationTarget));

        return button;
    }
}