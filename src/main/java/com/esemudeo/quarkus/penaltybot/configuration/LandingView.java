package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;

/**
 * Root entry point. Authenticated users go straight to the guild selection; everyone
 * else sees a login button that starts the Discord OAuth flow.
 */
@Route("")
public class LandingView extends VerticalLayout implements BeforeEnterObserver {

	private static final String LOGIN_PATH = "/login";

	@Inject
	AuthSession authSession;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (authSession.isUserAuthenticated()) {
			event.forwardTo(GuildSelectionView.class);
			return;
		}
		buildLogin();
	}

	private void buildLogin() {
		removeAll();
		setSizeFull();
		setAlignItems(FlexComponent.Alignment.CENTER);
		setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		add(new H2("Penalty Bot"));

		var intro = new Paragraph("Penalty Bot tracks penalties for your Discord server. "
				+ "Sign in with Discord to manage penalty types, command permissions and payment "
				+ "settings for the servers where you have the required role.");
		intro.getStyle()
				.set("max-width", "480px")
				.set("text-align", "center")
				.set("color", "var(--lumo-secondary-text-color)");
		add(intro);

		var loginButton = new Button("Log in with Discord");
		loginButton.addClickListener(e -> UI.getCurrent().getPage().setLocation(LOGIN_PATH));
		add(loginButton);

		add(new ThemeToggle());
	}
}
