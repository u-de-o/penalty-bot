package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.penalty.DateRange;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService.MemberPenaltyEntry;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shows every individual penalty entry for one member within one month – who reported
 * what and when – sorted newest first. Reached by clicking a member in the
 * {@link PenaltyOverviewView} grid.
 */
@Route("member-penalties")
public class PenaltyMemberDetailView extends GuildSessionView {

	private static final String CONTENT_MAX_WIDTH = "900px";
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Inject
	PenaltyOverviewService penaltyOverviewService;

	private Long memberId;
	private String memberName;
	private YearMonth month;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		memberId = queryParam(event, "memberId").map(Long::parseLong).orElse(null);
		memberName = queryParam(event, "memberName").orElse(null);
		month = queryParam(event, "month").map(YearMonth::parse).orElse(null);

		if (memberId == null || memberName == null || month == null) {
			event.getUI().getPage().setLocation("/overview");
			return;
		}
		super.beforeEnter(event);
	}

	private Optional<String> queryParam(BeforeEnterEvent event, String name) {
		List<String> values = event.getLocation().getQueryParameters().getParameters().get(name);
		if (values == null || values.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(values.getFirst());
	}

	@Override
	protected void renderGuildView() {
		String monthLabel = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
		String title = "%s in %s".formatted(memberName, monthLabel);
		setPageTitle("%s of %s".formatted(title, settingsService.getGuildName()));

		removeAll();
		setSizeFull();
		setPadding(false);
		getStyle().set("overflow-x", "hidden");

		var content = new Div();
		content.getStyle()
				.set("max-width", CONTENT_MAX_WIDTH)
				.set("width", "100%")
				.set("padding", "var(--lumo-space-m)")
				.set("box-sizing", "border-box")
				.set("margin", "0 auto");

		content.add(buildHeader(title));
		content.add(buildBackButton());
		content.add(buildGrid());

		add(content);
	}

	private Button buildBackButton() {
		var button = new Button("Back to overview", new Icon(VaadinIcon.ARROW_BACKWARD));
		button.addClickListener(e -> UI.getCurrent().navigate(PenaltyOverviewView.class));
		return button;
	}

	private Grid<MemberPenaltyEntry> buildGrid() {
		Grid<MemberPenaltyEntry> grid = new Grid<>();
		grid.getStyle().set("margin-top", "var(--lumo-space-m)");

		var timestampColumn = grid.addColumn(entry -> TIMESTAMP_FORMAT.format(entry.timestamp().atZone(ZoneOffset.UTC)))
				.setHeader("Reported at")
				.setComparator(Comparator.comparing(MemberPenaltyEntry::timestamp))
				.setSortable(true)
				.setAutoWidth(true);

		grid.addColumn(MemberPenaltyEntry::penaltyTypeName)
				.setHeader("Type")
				.setComparator(Comparator.comparing(MemberPenaltyEntry::penaltyTypeName, String.CASE_INSENSITIVE_ORDER))
				.setSortable(true)
				.setAutoWidth(true);

		grid.addColumn(MemberPenaltyEntry::amount)
				.setHeader("Amount")
				.setComparator(Comparator.comparingInt(MemberPenaltyEntry::amount))
				.setSortable(true)
				.setAutoWidth(true);

		grid.addColumn(MemberPenaltyEntry::authorName)
				.setHeader("Reported by")
				.setComparator(Comparator.comparing(MemberPenaltyEntry::authorName, String.CASE_INSENSITIVE_ORDER))
				.setSortable(true)
				.setAutoWidth(true);

		List<MemberPenaltyEntry> entries = penaltyOverviewService.memberEntries(authSession.getGuildId(), memberId, DateRange.ofMonth(month));
		grid.setItems(entries);
		grid.sort(List.of(new GridSortOrder<>(timestampColumn, SortDirection.DESCENDING)));

		return grid;
	}
}
