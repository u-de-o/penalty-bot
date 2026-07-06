package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("error")
public class ErrorView extends VerticalLayout {

    private static final String LOGIN_PATH = "/login";

    public ErrorView() {
        add(new H2("Access denied"));
        add(new Paragraph("Your session is invalid or you are not allowed to configure this server."));
        add(new Anchor(LOGIN_PATH, "Sign in again"));
        add(new ThemeToggle());
    }
}
