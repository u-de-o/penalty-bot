package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;

/**
 * Root entry point. Authenticated users go straight to the guild selection; everyone
 * else is sent to the OAuth login, so visiting the site immediately starts sign-in.
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
		event.getUI().getPage().setLocation(LOGIN_PATH);
	}
}
