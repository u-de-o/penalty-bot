package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.configuration.GuildAccessService.AccessibleGuild;
import com.esemudeo.quarkus.penaltybot.configuration.auth.model.AuthFlowToken;
import com.esemudeo.quarkus.penaltybot.configuration.auth.repository.AuthFlowTokenRepository;
import com.esemudeo.quarkus.penaltybot.shared.JDAInstance;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Post-login landing: shows the guilds the authenticated user is allowed to configure.
 * The authenticated user is carried in via a one-time handoff token (from the OAuth
 * callback) and stored in the Vaadin session. Picking a guild opens the settings view.
 */
@Slf4j
@Route("guilds")
public class GuildSelectionView extends VerticalLayout implements BeforeEnterObserver {

	private static final String CONTENT_MAX_WIDTH = "700px";
	private static final String GUILD_ICON_SIZE = "48px";
	private static final String LOGIN_PATH = "/login";
	private static final String LANDING_PATH = "/";
	private static final String UNKNOWN_USER_NAME = "there";

	@Inject
	AuthSession authSession;

	@Inject
	AuthFlowTokenRepository authFlowTokenRepository;

	@Inject
	GuildAccessService guildAccessService;

	@Inject
	JDAInstance jdaInstance;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (!ensureAuthenticated(event)) {
			return;
		}
		render();
	}

	private boolean ensureAuthenticated(BeforeEnterEvent event) {
		Optional<String> handoff = handoffToken(event);
		if (handoff.isPresent() && consumeHandoff(handoff.get())) {
			return true;
		}
		if (authSession.isUserAuthenticated()) {
			return true;
		}
		event.getUI().getPage().setLocation(LOGIN_PATH);
		return false;
	}

	private Optional<String> handoffToken(BeforeEnterEvent event) {
		List<String> tokens = event.getLocation().getQueryParameters().getParameters().get("token");
		if (tokens == null || tokens.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(tokens.getFirst());
	}

	private boolean consumeHandoff(String token) {
		Optional<AuthFlowToken> tokenOpt = authFlowTokenRepository.findValidByToken(token);
		if (tokenOpt.isEmpty() || tokenOpt.get().getUserId() == null) {
			return false;
		}
		long userId = tokenOpt.get().getUserId();
		authFlowTokenRepository.markAsUsed(token);
		authSession.setUserId(userId);
		authSession.setUserName(resolveUserName(userId));
		authSession.rotateNonce();
		return true;
	}

	private String resolveUserName(long userId) {
		try {
			return jdaInstance.getJda().retrieveUserById(userId).complete().getEffectiveName();
		} catch (Exception e) {
			log.debug("Could not resolve Discord user name for {}: {}", userId, e.getMessage());
			return UNKNOWN_USER_NAME;
		}
	}

	private void render() {
		removeAll();
		setAlignItems(FlexComponent.Alignment.CENTER);
		setSizeFull();

		var content = new Div();
		content.getStyle()
				.set("max-width", CONTENT_MAX_WIDTH)
				.set("width", "100%")
				.set("padding", "var(--lumo-space-m)")
				.set("box-sizing", "border-box");

		content.add(buildTopBar());
		content.add(new H2("Choose a server"));

		List<AccessibleGuild> guilds = guildAccessService.accessibleGuilds(authSession.getUserId());
		if (guilds.isEmpty()) {
			content.add(emptyState());
		} else {
			guilds.forEach(guild -> content.add(guildTile(guild)));
		}

		add(content);
	}

	private Div buildTopBar() {
		var greeting = new Span("Hello, %s!".formatted(authSession.getUserName()));
		greeting.getStyle().set("color", "var(--lumo-secondary-text-color)");

		var logoutButton = new Button("Log out", new Icon(VaadinIcon.SIGN_OUT));
		logoutButton.addClickListener(e -> logout());

		var actions = new Div(new ThemeToggle(), logoutButton);
		actions.getStyle()
				.set("display", "flex")
				.set("gap", "var(--lumo-space-s)")
				.set("align-items", "center");

		var topBar = new Div(greeting, actions);
		topBar.getStyle()
				.set("display", "flex")
				.set("justify-content", "space-between")
				.set("align-items", "center")
				.set("margin-bottom", "var(--lumo-space-m)");
		return topBar;
	}

	private void logout() {
		UI.getCurrent().getPage().setLocation(LANDING_PATH);
		authSession.invalidateSession();
	}

	private Paragraph emptyState() {
		var paragraph = new Paragraph("There are no servers where you have permission to configure Penalty Bot.");
		paragraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
		return paragraph;
	}

	private Div guildTile(AccessibleGuild guild) {
		var tile = new Div();
		tile.getStyle()
				.set("display", "flex")
				.set("align-items", "center")
				.set("gap", "var(--lumo-space-m)")
				.set("padding", "var(--lumo-space-m)")
				.set("margin-bottom", "var(--lumo-space-s)")
				.set("border", "1px solid var(--lumo-contrast-10pct)")
				.set("border-radius", "var(--lumo-border-radius-l)")
				.set("cursor", "pointer");

		if (guild.iconUrl() != null) {
			var icon = new Image(guild.iconUrl(), guild.name());
			icon.getStyle()
					.set("width", GUILD_ICON_SIZE)
					.set("height", GUILD_ICON_SIZE)
					.set("border-radius", "50%");
			tile.add(icon);
		}

		var name = new Span(guild.name());
		name.getStyle().set("font-size", "var(--lumo-font-size-l)");
		tile.add(name);

		tile.addClickListener(e -> selectGuild(guild.id()));
		return tile;
	}

	private void selectGuild(long guildId) {
		if (!guildAccessService.canAccess(authSession.getUserId(), guildId)) {
			log.warn("Guild selection denied for user {} on guild {}", authSession.getUserId(), guildId);
			render();
			return;
		}
		authSession.setGuildId(guildId);
		authSession.rotateNonce();
		UI.getCurrent().navigate(SettingsView.class);
	}
}
