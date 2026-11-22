package com.esemudeo.quarkus.penaltybot.penalty.listener;

import com.esemudeo.quarkus.penaltybot.configuration.global.model.GlobalGuildConfig;
import com.esemudeo.quarkus.penaltybot.configuration.global.repository.GlobalGuildConfigRepository;
import com.esemudeo.quarkus.penaltybot.penalty.command.PenaltyCorrectionPermission;
import com.esemudeo.quarkus.penaltybot.penalty.command.ReportPenaltyCommand;
import com.esemudeo.quarkus.penaltybot.penalty.model.Penalty;
import com.esemudeo.quarkus.penaltybot.penalty.repository.PenaltyRepository;
import com.esemudeo.quarkus.penaltybot.permission.PermissionService;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class ReportPenaltyModalListener extends ModalListener implements MemberModalTrait {

	@Inject
	PenaltyRepository penaltyRepository;

	@Inject
	PermissionService permissionService;

	@Inject
	GlobalGuildConfigRepository globalGuildConfigRepository;

	@Override
	protected String getModalIdPrefix() {
		return ReportPenaltyCommand.MODAL_ID_PREFIX;
	}

	@Override
	protected void handleModalInteraction(ModalInteractionEvent event, @Nonnull Guild guild) {
		Member affectedMember = validateMember(event, ReportPenaltyCommand.FIELD_MEMBER).orElse(null);
		if (affectedMember == null) {
			return;
		}

		if (hasNoPenaltyReportPermission(event, guild, affectedMember, permissionService)) {
			return;
		}

		String amountRaw = Objects.requireNonNull(event.getValue(ReportPenaltyCommand.FIELD_AMOUNT)).getAsString().trim();
		int amount;
		try {
			amount = Integer.parseInt(amountRaw);
			if (amount == 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			event.reply("Invalid amount: " + amountRaw).setEphemeral(true).queue();
			return;
		}

		boolean isCorrection = amount < 0;
		Member author = Objects.requireNonNull(event.getMember());
		if (isCorrection && !permissionService.isAllowedForCommand(guild, author, PenaltyCorrectionPermission.COMMAND_NAME)) {
			event.reply("You are not allowed to reduce penalties. Contact an admin for that, please.").setEphemeral(true).queue();
			return;
		}

		String comment = Optional.ofNullable(event.getValue(ReportPenaltyCommand.FIELD_COMMENT))
				.map(ModalMapping::getAsString)
				.map(String::trim)
				.filter(c -> !c.isEmpty())
				.orElse(null);
		if (isCorrection && comment == null) {
			event.reply("A comment is required for negative amounts.").setEphemeral(true).queue();
			return;
		}

		String penaltyTypeName = Objects.requireNonNull(event.getValue(ReportPenaltyCommand.FIELD_REASON)).getAsStringList().getFirst();

		Penalty penaltyDraft = Penalty.builder()
				.timestamp(Instant.now())
				.guildId(guild.getIdLong())
				.authorId(event.getUser().getIdLong())
				.affectedMemberId(affectedMember.getIdLong())
				.amount(amount)
				.comment(comment)
				.build();

		Optional<Penalty> penalty = isCorrection
				? penaltyRepository.saveCorrection(penaltyDraft, penaltyTypeName)
				: penaltyRepository.save(penaltyDraft, penaltyTypeName);
		if (penalty.isEmpty()) {
			event.reply("Unknown penalty type: " + penaltyTypeName).setEphemeral(true).queue();
			return;
		}

		String successMessage = "%s reported %d x %s for %s."
				.formatted(Objects.requireNonNull(event.getMember()).getEffectiveName(), amount, penalty.get().getPenaltyType().getDisplayName(), affectedMember.getEffectiveName());
		if (comment != null) {
			successMessage += "\nComment: " + comment;
		}

		Optional<GlobalGuildConfig> config = globalGuildConfigRepository.findByGuild(guild.getIdLong());
		Long notificationChannelId = config.map(GlobalGuildConfig::getNotificationChannelId).orElse(null);

		if (notificationChannelId != null) {
			TextChannel notificationChannel = guild.getTextChannelById(notificationChannelId);
			if (notificationChannel != null) {
				notificationChannel.sendMessage(successMessage).queue();
			}
		}
		event.reply(successMessage).setEphemeral(true).queue();
	}
}
