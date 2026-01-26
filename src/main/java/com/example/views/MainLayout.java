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


    private void createDrawer() {
        VerticalLayout menuLayout = new VerticalLayout();

        menuLayout.setPadding(true);
        menuLayout.setSpacing(true);

        menuLayout.add(createMenuButton("Pulpit", VaadinIcon.DASHBOARD.create(), DashboardView.class));

        if ("Pacjent".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Umów wizytę", VaadinIcon.CALENDAR_CLOCK.create(), PatientBookingView.class));
            menuLayout.add(createMenuButton("Historia wizyt", VaadinIcon.FILE_TEXT.create(), PatientHistoryView.class));
        }

        else if ("Lekarz".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Harmonogram", VaadinIcon.CALENDAR_USER.create(), DoctorScheduleView.class));
        }

        else if ("Admin".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Użytkownicy", VaadinIcon.USERS.create(), AdminUsersView.class));
            menuLayout.add(createMenuButton("Statystyki", VaadinIcon.CHART.create(), AdminStatsView.class));
        }

        else if ("Rejestracja".equals(user.getRola())) {
            menuLayout.add(createMenuButton("Lekarze i Grafiki", VaadinIcon.DOCTOR.create(), DoctorsListView.class));
        }

        addToDrawer(menuLayout);
    }

    private Button createMenuButton(String caption, Icon icon, Class<? extends Component> navigationTarget) {
        Button button = new Button(caption, icon);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.setWidthFull();
        button.getStyle().set("justify-content", "flex-start");
        icon.getElement().getStyle().set("margin-right", "15px");
        button.addClickListener(e -> UI.getCurrent().navigate(navigationTarget));

        return button;
    }
}