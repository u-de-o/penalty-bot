package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.penalty.DateRange;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService.MemberRow;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService.OverviewTable;
import com.esemudeo.quarkus.penaltybot.penalty.repository.PenaltyRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Route;
import jakarta.inject.Inject;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Per-guild penalty overview: a sortable table of per-member penalty counts (one column
 * per penalty type) and the total amount owed, for a single month at a time. The month
 * pager sits above the grid; the newest available month is shown first.
 *
 * The underlying data is fetched via {@link PenaltyOverviewService#overview(long, DateRange)},
 * which is already range-based. A future custom day-precise range picker only needs to
 * build a different {@link DateRange} (e.g. {@link DateRange#ofDays}) and call the same
 * {@code loadMonth}-style refresh – no service or repository changes required.
 */
@Route("overview")
public class PenaltyOverviewView extends GuildSessionView {

	private static final String CONTENT_MAX_WIDTH = "1000px";
	private static final double CENTS_PER_EURO = 100.0;
	private static final String EURO_AMOUNT_FORMAT = "%.2f";

	@Inject
	PenaltyOverviewService penaltyOverviewService;

	@Inject
	PenaltyRepository penaltyRepository;

	private final Grid<MemberRow> grid = new Grid<>();
	private final Span monthLabel = new Span();
	private final Button olderButton = new Button("Older", new Icon(VaadinIcon.ANGLE_LEFT));
	private final Button newerButton = new Button("Newer", new Icon(VaadinIcon.ANGLE_RIGHT));

	private List<YearMonth> availableMonths = List.of();
	private int currentMonthIndex;
	private boolean columnsBuilt;
	private String paypalUsername;

	@Override
	protected void renderGuildView() {
		String title = "Penalty overview of %s".formatted(settingsService.getGuildName());
		setPageTitle(title);

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

		availableMonths = penaltyRepository.findAvailableMonthsForGuild(guildId());
		if (availableMonths.isEmpty()) {
			content.add(new Paragraph("No penalties have been recorded for this server yet."));
			add(content);
			return;
		}

		columnsBuilt = false;
		content.add(buildMonthPager());
		content.add(grid);
		loadMonth(indexOfLastCompletedMonth());

		add(content);
	}

	private long guildId() {
		return authSession.getGuildId();
	}

	/**
	 * Skips the current, still-ongoing month if it happens to be the newest entry in
	 * {@link #availableMonths} (index 0, since the list is sorted newest first), so the
	 * default view is always the last fully completed month.
	 */
	private int indexOfLastCompletedMonth() {
		if (availableMonths.size() > 1 && availableMonths.getFirst().equals(YearMonth.now())) {
			return 1;
		}
		return 0;
	}

	private Div buildMonthPager() {
		olderButton.addClickListener(e -> {
			if (currentMonthIndex < availableMonths.size() - 1) {
				loadMonth(currentMonthIndex + 1);
			}
		});
		newerButton.addClickListener(e -> {
			if (currentMonthIndex > 0) {
				loadMonth(currentMonthIndex - 1);
			}
		});

		monthLabel.getStyle().set("font-weight", "bold").set("min-width", "160px").set("text-align", "center");

		var pager = new Div(newerButton, monthLabel, olderButton);
		pager.getStyle()
				.set("display", "flex")
				.set("align-items", "center")
				.set("justify-content", "center")
				.set("gap", "var(--lumo-space-m)")
				.set("margin-bottom", "var(--lumo-space-m)");
		return pager;
	}

	private void loadMonth(int monthIndex) {
		currentMonthIndex = monthIndex;
		YearMonth month = availableMonths.get(monthIndex);
		OverviewTable table = penaltyOverviewService.overview(guildId(), DateRange.ofMonth(month));

		if (!columnsBuilt) {
			paypalUsername = table.paypalUsername().orElse(null);
			buildColumns(table.typeColumns());
			columnsBuilt = true;
		}

		grid.setItems(table.rows());
		monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());
		olderButton.setEnabled(monthIndex < availableMonths.size() - 1);
		newerButton.setEnabled(monthIndex > 0);
	}

	private void buildColumns(List<String> typeColumns) {
		grid.addColumn(MemberRow::displayName)
				.setHeader("Member")
				.setComparator(Comparator.comparing(MemberRow::displayName, String.CASE_INSENSITIVE_ORDER))
				.setSortable(true)
				.setAutoWidth(true);

		typeColumns.forEach(typeName -> grid.addColumn(row -> row.countFor(typeName))
				.setHeader(typeName)
				.setComparator(Comparator.comparingInt(row -> row.countFor(typeName)))
				.setSortable(true)
				.setAutoWidth(true));

		var amountColumn = grid.addColumn(this::formatAmount)
				.setHeader("Amount (€)")
				.setComparator(Comparator.comparingInt(MemberRow::totalCents))
				.setSortable(true)
				.setAutoWidth(true);

		if (paypalUsername != null) {
			grid.addComponentColumn(this::buildPaypalLink).setHeader("PayPal").setAutoWidth(true);
		}

		grid.sort(List.of(new GridSortOrder<>(amountColumn, SortDirection.DESCENDING)));
	}

	private String formatAmount(MemberRow row) {
		return EURO_AMOUNT_FORMAT.formatted(row.totalCents() / CENTS_PER_EURO);
	}

	private Anchor buildPaypalLink(MemberRow row) {
		if (row.totalCents() <= 0) {
			return new Anchor();
		}
		String euroAmount = EURO_AMOUNT_FORMAT.formatted(row.totalCents() / CENTS_PER_EURO);
		return new Anchor("https://paypal.me/%s/%sEUR".formatted(paypalUsername, euroAmount), "Pay");
	}
}
