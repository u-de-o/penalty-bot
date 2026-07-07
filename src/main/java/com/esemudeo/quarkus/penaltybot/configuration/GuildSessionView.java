package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for authenticated per-guild pages. Applies the same session/nonce/access
 * guard as {@link SettingsView}: a page is only rendered for an authenticated user who
 * still passes the access gate for the guild currently held in the session. Subclasses
 * implement {@link #renderGuildView()} and can reuse {@link #buildHeader(String)}.
 */
@Slf4j
public abstract class GuildSessionView extends VerticalLayout implements BeforeEnterObserver {

	protected static final String LOGIN_PATH = "/login";
	protected static final String LANDING_PATH = "/";
	private static final String LOGOUT_LABEL = "Log out";
	private static final String BACK_TO_SELECTION_LABEL = "Server selection";
	private static final String HOME_LABEL = "Home";

	@Inject
	protected AuthSession authSession;

	@Inject
	protected SettingsService settingsService;

	private String sessionNonce;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (sessionNonce != null && authSession.isActiveNonce(sessionNonce)) {
			settingsService.setUiNonce(sessionNonce);
		} else if (authSession.isNotAuthenticated()) {
			event.getUI().getPage().setLocation(LOGIN_PATH);
			return;
		} else {
			sessionNonce = authSession.getActiveNonce();
			settingsService.setUiNonce(sessionNonce);
			onNewSession();
		}

		try {
			settingsService.assertCanAccessCurrentGuild();
			renderGuildView();
		} catch (Exception e) {
			log.warn("Guild view error: {}", e.getMessage());
			event.forwardTo(ErrorView.class);
		}
	}

	protected abstract void renderGuildView();

	/**
	 * Called when the page is entered under a freshly rotated nonce (a new login or a
	 * different guild was just selected) rather than a preserved reload of the same
	 * session. Subclasses that cache built UI/state across {@link #renderGuildView()}
	 * calls (see {@link SettingsView}) can override this to invalidate that cache.
	 */
	protected void onNewSession() {
		// no-op by default
	}

	/** Sets the browser tab title for this page. */
	protected void setPageTitle(String title) {
		UI.getCurrent().getPage().setTitle(title);
	}

	/** Header bar with the page title, greeting, navigation, theme toggle and logout. */
	protected Div buildHeader(String title) {
		var heading = new H2(title);
		heading.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-xl)");

		var greeting = new Span("Hello, %s!".formatted(authSession.getUserName()));
		greeting.getStyle()
				.set("color", "var(--lumo-secondary-text-color)")
				.set("font-size", "var(--lumo-font-size-s)");

		var titleBlock = new Div(heading, greeting);

		var homeButton = new Button(HOME_LABEL, new Icon(VaadinIcon.HOME));
		homeButton.addClickListener(e -> UI.getCurrent().navigate(GuildHomeView.class));

		var backButton = new Button(BACK_TO_SELECTION_LABEL, new Icon(VaadinIcon.ARROW_BACKWARD));
		backButton.addClickListener(e -> UI.getCurrent().navigate(GuildSelectionView.class));

		var logoutButton = new Button(LOGOUT_LABEL, new Icon(VaadinIcon.SIGN_OUT));
		logoutButton.addClickListener(e -> logout());

		var actions = new Div(homeButton, backButton, new ThemeToggle(), logoutButton);
		actions.getStyle()
				.set("display", "flex")
				.set("gap", "var(--lumo-space-s)")
				.set("align-items", "center");

		var header = new Div(titleBlock, actions);
		header.getStyle()
				.set("display", "flex")
				.set("justify-content", "space-between")
				.set("align-items", "center")
				.set("margin-bottom", "var(--lumo-space-m)");
		return header;
	}

	protected void logout() {
		UI.getCurrent().getPage().setLocation(LANDING_PATH);
		authSession.invalidateSession();
	}
}
