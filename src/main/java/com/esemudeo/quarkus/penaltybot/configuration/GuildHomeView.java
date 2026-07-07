package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.Route;

/**
 * Per-guild landing shown after a server has been selected. Offers the two entry points
 * for that guild: its configuration and the penalty overview.
 */
@Route("home")
public class GuildHomeView extends GuildSessionView {

	private static final String CONTENT_MAX_WIDTH = "700px";
	private static final String TILE_MIN_WIDTH = "220px";

	@Override
	protected void renderGuildView() {
		String title = "Home of %s".formatted(settingsService.getGuildName());
		setPageTitle(title);

		removeAll();
		setSizeFull();
		setAlignItems(FlexComponent.Alignment.CENTER);

		var content = new Div();
		content.getStyle()
				.set("max-width", CONTENT_MAX_WIDTH)
				.set("width", "100%")
				.set("padding", "var(--lumo-space-m)")
				.set("box-sizing", "border-box");

		content.add(buildHeader(title));
		content.add(buildActions());
		add(content);
	}

	private Div buildActions() {
		var settingsTile = tile(VaadinIcon.COG, "Server settings",
				"Configure penalty types, command permissions and payment settings.",
				() -> UI.getCurrent().navigate(SettingsView.class));

		var overviewTile = tile(VaadinIcon.TABLE, "Penalty overview",
				"Browse monthly penalty totals per member.",
				() -> UI.getCurrent().navigate(PenaltyOverviewView.class));

		var actions = new Div(settingsTile, overviewTile);
		actions.getStyle()
				.set("display", "flex")
				.set("gap", "var(--lumo-space-m)")
				.set("flex-wrap", "wrap");
		return actions;
	}

	private Div tile(VaadinIcon vaadinIcon, String title, String description, Runnable onClick) {
		var icon = new Icon(vaadinIcon);
		icon.getStyle().set("width", "32px").set("height", "32px").set("color", "var(--lumo-primary-color)");

		var heading = new H3(title);
		heading.getStyle().set("margin", "var(--lumo-space-s) 0 0 0");

		var text = new Paragraph(description);
		text.getStyle()
				.set("margin", "var(--lumo-space-xs) 0 0 0")
				.set("color", "var(--lumo-secondary-text-color)")
				.set("font-size", "var(--lumo-font-size-s)");

		var tile = new Div(icon, heading, text);
		tile.getStyle()
				.set("flex", "1 1 %s".formatted(TILE_MIN_WIDTH))
				.set("min-width", TILE_MIN_WIDTH)
				.set("padding", "var(--lumo-space-l)")
				.set("border", "1px solid var(--lumo-contrast-10pct)")
				.set("border-radius", "var(--lumo-border-radius-l)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)")
				.set("cursor", "pointer")
				.set("transition", "box-shadow 0.15s ease-in-out");
		tile.getElement().setAttribute("tabindex", "0");
		tile.addClickListener(e -> onClick.run());
		return tile;
	}
}
