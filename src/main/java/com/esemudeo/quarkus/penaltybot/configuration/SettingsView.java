package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.CommandPermissionsCard;
import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.CommandPermissionsHandler;
import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.model.CommandPermission;
import com.esemudeo.quarkus.penaltybot.configuration.global.GlobalSettingsCard;
import com.esemudeo.quarkus.penaltybot.configuration.global.GlobalSettingsHandler;
import com.esemudeo.quarkus.penaltybot.configuration.global.model.GlobalGuildConfig;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.PenaltyTypesCard;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.PenaltyTypesHandler;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.model.PenaltyType;
import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route("settings")
@PreserveOnRefresh
public class SettingsView extends VerticalLayout implements BeforeEnterObserver {

	private static final String CONTENT_MAX_WIDTH = "1200px";
	private static final String COLUMN_FLEX_BASIS = "1 1 400px";
	private static final String LUMO_DARK_THEME = "dark";
	private static final String LOGIN_PATH = "/login";
	private static final String SWITCH_SERVER_LABEL = "Choose another server";

	// Checks if the browser's system color scheme preference is set to dark
	private static final String JS_PREFERS_DARK_MODE = "return window.matchMedia('(prefers-color-scheme:dark)').matches";

	private static final String DARK_MODE_LABEL = "To the dark room";
	private static final String LIGHT_MODE_LABEL = "Back to the light";

	@Inject
	AuthSession authSession;

	@Inject
	SettingsService settingsService;

	private CommandPermissionsCard commandPermissionsCard;
	private PenaltyTypesCard penaltyTypesCard;
	private GlobalSettingsCard globalSettingsCard;
	private Button darkModeToggle;

	private boolean initialized;
	private String sessionNonce;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (sessionNonce != null && authSession.isActiveNonce(sessionNonce)) {
			settingsService.setUiNonce(sessionNonce);
		} else if (authSession.isNotAuthenticated()) {
			event.getUI().getPage().setLocation(LOGIN_PATH);
			return;
		} else {
			// Arrived from the guild selection: adopt the current active nonce for this UI.
			sessionNonce = authSession.getActiveNonce();
			settingsService.setUiNonce(sessionNonce);
			initialized = false;
		}

		try {
			settingsService.assertCanAccessCurrentGuild();
			if (!initialized) {
				buildSections();
				initialized = true;
			}
			applyInitialState();
		} catch (Exception e) {
			log.warn("Settings view error: {}", e.getMessage());
			event.forwardTo(ErrorView.class);
		}
	}

	private void buildSections() {
		removeAll();
		setPadding(false);
		setSpacing(false);
		setAlignItems(FlexComponent.Alignment.CENTER);
		setSizeFull();
		getStyle()
				.set("background-color", "var(--lumo-shade-5pct)")
				.set("overflow-x", "hidden");

		var content = new Div();
		content.getStyle()
				.set("max-width", CONTENT_MAX_WIDTH)
				.set("width", "100%")
				.set("padding", "var(--lumo-space-m)")
				.set("box-sizing", "border-box");

		content.add(buildHeader());

		// Load data
		var commandPermissions = settingsService.getCommands();
		var globalConfig = settingsService.getGlobalConfig();
		var penaltyTypes = settingsService.getAllPenaltyTypes();

		// Create handlers (logic)
		var cpHandler = new CommandPermissionsHandler(commandPermissions, settingsService);
		var ptHandler = new PenaltyTypesHandler(penaltyTypes, settingsService);
		var gsHandler = new GlobalSettingsHandler(globalConfig, settingsService);

		// Create cards (UI)
		commandPermissionsCard = new CommandPermissionsCard(cpHandler);
		penaltyTypesCard = new PenaltyTypesCard(ptHandler);
		globalSettingsCard = new GlobalSettingsCard(gsHandler);

		// Two-column layout
		var columnsRow = new Div();
		columnsRow.getStyle()
				.set("display", "flex")
				.set("gap", "var(--lumo-space-s)")
				.set("align-items", "flex-start")
				.set("flex-wrap", "wrap");

		var leftColumn = new Div();
		leftColumn.getStyle()
				.set("flex", COLUMN_FLEX_BASIS)
				.set("min-width", "0");
		leftColumn.add(commandPermissionsCard);

		var rightColumn = new Div();
		rightColumn.getStyle()
				.set("flex", COLUMN_FLEX_BASIS)
				.set("min-width", "0");
		rightColumn.add(penaltyTypesCard);
		rightColumn.add(globalSettingsCard);

		columnsRow.add(leftColumn, rightColumn);
		content.add(columnsRow);

		add(content);
	}

	private Div buildHeader() {
		var header = new Div();
		header.getStyle().set("margin-bottom", "var(--lumo-space-s)");

		var heading = new H2("Penalty Bot Server Settings");
		heading.getStyle()
				.set("margin", "0")
				.set("font-size", "var(--lumo-font-size-xl)");

		var guildName = new Span(settingsService.getGuildName());
		guildName.getStyle()
				.set("font-size", "var(--lumo-font-size-m)")
				.set("color", "var(--lumo-secondary-text-color)");

		darkModeToggle = new Button(DARK_MODE_LABEL, new Icon(VaadinIcon.MOON));
		darkModeToggle.addClickListener(e -> toggleDarkMode());

		var switchServerButton = new Button(SWITCH_SERVER_LABEL, new Icon(VaadinIcon.EXCHANGE));
		switchServerButton.addClickListener(e -> UI.getCurrent().navigate(GuildSelectionView.class));

		var actions = new Div(switchServerButton, darkModeToggle);
		actions.getStyle()
				.set("display", "flex")
				.set("gap", "var(--lumo-space-s)")
				.set("align-items", "center");

		var topRow = new Div();
		topRow.getStyle()
				.set("display", "flex")
				.set("justify-content", "space-between")
				.set("align-items", "center")
				.set("padding-right", "var(--lumo-space-m)");

		var titleBlock = new Div(heading, guildName);
		topRow.add(titleBlock, actions);

		var welcome = new Paragraph("Welcome, %s!".formatted(settingsService.getMemberDisplayName()));
		welcome.getStyle()
				.set("margin", "var(--lumo-space-xs) 0 0 0")
				.set("color", "var(--lumo-secondary-text-color)")
				.set("font-size", "var(--lumo-font-size-s)");

		header.add(topRow, welcome);
		return header;
	}

	private void toggleDarkMode() {
		var themeList = UI.getCurrent().getElement().getThemeList();
		boolean switchToDark = !themeList.contains(LUMO_DARK_THEME);
		if (switchToDark) {
			themeList.add(LUMO_DARK_THEME);
		} else {
			themeList.remove(LUMO_DARK_THEME);
		}
		updateDarkModeButton(switchToDark);
	}

	private void updateDarkModeButton(boolean isDark) {
		darkModeToggle.setText(isDark ? LIGHT_MODE_LABEL : DARK_MODE_LABEL);
		darkModeToggle.setIcon(new Icon(isDark ? VaadinIcon.SUN_O : VaadinIcon.MOON));
	}

	private void syncDarkModeWithSystemPreference() {
		UI.getCurrent().getPage().executeJs(JS_PREFERS_DARK_MODE).then(Boolean.class, isDark -> {
			if (isDark) {
				UI.getCurrent().getElement().getThemeList().add(LUMO_DARK_THEME);
				updateDarkModeButton(true);
			}
		});
	}

	private void applyInitialState() {
		commandPermissionsCard.applyInitialState();
		penaltyTypesCard.applyInitialState();
		globalSettingsCard.applyInitialState();
		syncDarkModeWithSystemPreference();
	}

}
