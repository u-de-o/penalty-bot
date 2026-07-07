package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.CommandPermissionsCard;
import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.CommandPermissionsHandler;
import com.esemudeo.quarkus.penaltybot.configuration.global.GlobalSettingsCard;
import com.esemudeo.quarkus.penaltybot.configuration.global.GlobalSettingsHandler;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.PenaltyTypesCard;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.PenaltyTypesHandler;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("settings")
@PreserveOnRefresh
public class SettingsView extends GuildSessionView {

	private static final String CONTENT_MAX_WIDTH = "1200px";
	private static final String COLUMN_FLEX_BASIS = "1 1 400px";

	private CommandPermissionsCard commandPermissionsCard;
	private PenaltyTypesCard penaltyTypesCard;
	private GlobalSettingsCard globalSettingsCard;

	private boolean initialized;

	@Override
	protected void renderGuildView() {
		setPageTitle("Settings of %s".formatted(settingsService.getGuildName()));
		if (!initialized) {
			buildSections();
			initialized = true;
		}
		applyInitialState();
	}

	@Override
	protected void onNewSession() {
		initialized = false;
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

		content.add(buildHeader("Settings of %s".formatted(settingsService.getGuildName())));

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

	private void applyInitialState() {
		commandPermissionsCard.applyInitialState();
		penaltyTypesCard.applyInitialState();
		globalSettingsCard.applyInitialState();
	}

}
