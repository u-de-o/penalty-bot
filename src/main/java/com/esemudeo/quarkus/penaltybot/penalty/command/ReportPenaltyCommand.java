package com.esemudeo.quarkus.penaltybot.penalty.command;

import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.model.PenaltyType;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.repository.PenaltyTypeRepository;
import com.esemudeo.quarkus.penaltybot.permission.RequiresCommandPermission;
import com.esemudeo.quarkus.penaltybot.shared.command.SlashCommand;
import com.esemudeo.quarkus.penaltybot.shared.command.UserContextMenuCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiresCommandPermission
@ApplicationScoped
public class ReportPenaltyCommand implements SlashCommand, UserContextMenuCommand {

	public static final String MODAL_ID_PREFIX = "report-penalty:";
	public static final String FIELD_MEMBER = "modal-member";
	public static final String FIELD_AMOUNT = "modal-amount";
	public static final String FIELD_REASON = "modal-reason";
	public static final String FIELD_COMMENT = "modal-comment";
	public static final int MAX_COMMENT_LENGTH = 500;

	@Inject
	PenaltyTypeRepository penaltyTypeRepository;

	@Override
	public String getName() {
		return "penalty";
	}

	@Override
	public String getHelpDescription() {
		return "Report a penalty for a member. Only members who can use this command can be reported.";
	}

	@Override
	public String getContextMenuName() {
		return "Report penalty";
	}

	@Override
	public void handleSlashCommand(SlashCommandInteractionEvent event) {
		executeInGuild(event, guild -> openModal(guild, event.getUser().getIdLong(), event));
	}

	@Override
	public void handleContextMenuCommand(UserContextInteractionEvent event) {
		executeInGuild(event, guild -> openModal(guild, event.getTarget().getIdLong(), event));
	}

	private void openModal(Guild guild, long preselectedUserId, GenericCommandInteractionEvent callback) {
		EntitySelectMenu memberSelect = EntitySelectMenu.create(FIELD_MEMBER, EntitySelectMenu.SelectTarget.USER)
				.setPlaceholder("Select a member")
				.setDefaultValues(EntitySelectMenu.DefaultValue.user(preselectedUserId))
				.setRequired(true)
				.build();

		TextInput amountInput = TextInput.create(FIELD_AMOUNT, TextInputStyle.SHORT)
				.setPlaceholder("1")
				.setValue("1")
				.setRequired(true)
				.build();

		List<PenaltyType> penaltyTypes = penaltyTypeRepository.findActiveByGuild(guild.getIdLong());
		List<SelectOption> allPenaltyTypes = penaltyTypes.stream()
				.map(pt -> SelectOption.of(pt.getDisplayName(), pt.getTechnicalName()))
				.toList();
		Optional<PenaltyType> defaultPenaltyType = penaltyTypes.stream()
				.filter(PenaltyType::isDefaultType)
				.findFirst();

		StringSelectMenu.Builder reasonSelectBuilder = StringSelectMenu.create(FIELD_REASON)
				.addOptions(allPenaltyTypes)
				.setRequired(true);

		if (defaultPenaltyType.isPresent()) {
			reasonSelectBuilder.setDefaultValues(defaultPenaltyType.get().getTechnicalName());
		} else {
			reasonSelectBuilder.setPlaceholder("No default value configured - please select manually");
		}

		StringSelectMenu reasonSelect = reasonSelectBuilder.build();

		TextInput commentInput = TextInput.create(FIELD_COMMENT, TextInputStyle.PARAGRAPH)
				.setPlaceholder("Optional – required when the amount is negative")
				.setMaxLength(MAX_COMMENT_LENGTH)
				.setRequired(false)
				.build();

		Modal modal = Modal.create(MODAL_ID_PREFIX + UUID.randomUUID(), "Report Penalty")
				.addComponents(
						Label.of("Affected Member", memberSelect),
						Label.of("Amount", amountInput),
						Label.of("Reason", reasonSelect),
						Label.of("Comment (required for negative amounts)", commentInput))
				.build();

		callback.replyModal(modal).queue();
	}
}