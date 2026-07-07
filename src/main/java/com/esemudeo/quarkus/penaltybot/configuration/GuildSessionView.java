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
	private static final String ROUND_BUTTON_SIZE = "40px";

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

	/**
	 * Header with one row of action buttons (Home + Server selection, labeled, on the
	 * left; Dark mode + Log out, round icon-only, on the right), the page title centered
	 * below it, and the greeting centered below the title.
	 */
	protected Div buildHeader(String title) {
		var homeButton = pillButton(VaadinIcon.HOME, "Home", () -> UI.getCurrent().navigate(GuildHomeView.class));
		var backButton = pillButton(VaadinIcon.ARROW_BACKWARD, "Server selection",
				() -> UI.getCurrent().navigate(GuildSelectionView.class));

		var themeToggle = new ThemeToggle(true);
		styleAsRoundButton(themeToggle);

		var logoutButton = roundIconButton(VaadinIcon.SIGN_OUT, "Log out", this::logout);

		var leftGroup = new Div(homeButton, backButton);
		leftGroup.getStyle().set("display", "flex").set("gap", "var(--lumo-space-s)");

		var rightGroup = new Div(themeToggle, logoutButton);
		rightGroup.getStyle().set("display", "flex").set("gap", "var(--lumo-space-s)");

		var buttonRow = new Div(leftGroup, rightGroup);
		buttonRow.getStyle()
				.set("display", "flex")
				.set("justify-content", "space-between")
				.set("align-items", "center")
				.set("width", "100%");

		var heading = new H2(title);
		heading.getStyle()
				.set("margin", "var(--lumo-space-m) 0 0 0")
				.set("font-size", "var(--lumo-font-size-xl)")
				.set("text-align", "center");

		var greeting = new Span("Hello, %s!".formatted(authSession.getUserName()));
		greeting.getStyle()
				.set("margin-top", "var(--lumo-space-xs)")
				.set("color", "var(--lumo-secondary-text-color)")
				.set("font-size", "var(--lumo-font-size-s)")
				.set("text-align", "center");

		var header = new Div(buttonRow, heading, greeting);
		header.getStyle()
				.set("display", "flex")
				.set("flex-direction", "column")
				.set("align-items", "center")
				.set("width", "100%")
				.set("margin-bottom", "var(--lumo-space-m)");
		return header;
	}

	private Button roundIconButton(VaadinIcon vaadinIcon, String tooltip, Runnable onClick) {
		var button = new Button(new Icon(vaadinIcon));
		button.getElement().setAttribute("title", tooltip);
		styleAsRoundButton(button);
		button.addClickListener(e -> onClick.run());
		return button;
	}

	/** A wide, pill-shaped button with both an icon and a visible text label. */
	private Button pillButton(VaadinIcon vaadinIcon, String label, Runnable onClick) {
		var button = new Button(label, new Icon(vaadinIcon));
		button.getStyle().set("border-radius", "999px");
		button.addClickListener(e -> onClick.run());
		return button;
	}

	private void styleAsRoundButton(Button button) {
		button.getStyle()
				.set("border-radius", "50%")
				.set("width", ROUND_BUTTON_SIZE)
				.set("height", ROUND_BUTTON_SIZE)
				.set("min-width", ROUND_BUTTON_SIZE)
				.set("padding", "0");
	}

	protected void logout() {
		UI.getCurrent().getPage().setLocation(LANDING_PATH);
		authSession.invalidateSession();
	}
}
