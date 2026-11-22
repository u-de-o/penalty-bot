package com.esemudeo.quarkus.penaltybot.penalty.listener;

import com.esemudeo.quarkus.penaltybot.configuration.global.repository.GlobalGuildConfigRepository;
import com.esemudeo.quarkus.penaltybot.penalty.command.PenaltyCorrectionPermission;
import com.esemudeo.quarkus.penaltybot.penalty.command.ReportPenaltyCommand;
import com.esemudeo.quarkus.penaltybot.penalty.model.Penalty;
import com.esemudeo.quarkus.penaltybot.penalty.repository.PenaltyRepository;
import com.esemudeo.quarkus.penaltybot.permission.PermissionService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Simulates the /penalty modal submit without Discord: positive report vs. negative correction. */
class ReportPenaltyModalListenerTest {

	private ReportPenaltyModalListener listener;
	private PermissionService permissionService;
	private PenaltyRepository penaltyRepository;
	private ModalInteractionEvent event;
	private Guild guild;
	private Member author;

	@BeforeEach
	void setUp() {
		permissionService = mock(PermissionService.class);
		penaltyRepository = mock(PenaltyRepository.class);
		GlobalGuildConfigRepository configRepository = mock(GlobalGuildConfigRepository.class);
		when(configRepository.findByGuild(1L)).thenReturn(Optional.empty());

		listener = new ReportPenaltyModalListener();
		listener.permissionService = permissionService;
		listener.penaltyRepository = penaltyRepository;
		listener.globalGuildConfigRepository = configRepository;

		guild = mock(Guild.class);
		when(guild.getIdLong()).thenReturn(1L);
		Member affected = mock(Member.class);
		when(affected.getIdLong()).thenReturn(42L);
		when(affected.getEffectiveName()).thenReturn("Victim");
		author = mock(Member.class);
		when(author.getEffectiveName()).thenReturn("Author");
		User user = mock(User.class);
		when(user.getIdLong()).thenReturn(7L);

		event = mock(ModalInteractionEvent.class, Answers.RETURNS_DEEP_STUBS);
		when(event.getMember()).thenReturn(author);
		when(event.getUser()).thenReturn(user);
		Mentions mentions = mock(Mentions.class);
		when(mentions.getMembers()).thenReturn(List.of(affected));
		ModalMapping memberMapping = mock(ModalMapping.class);
		when(memberMapping.getAsMentions()).thenReturn(mentions);
		when(event.getValue(ReportPenaltyCommand.FIELD_MEMBER)).thenReturn(memberMapping);
		ModalMapping reasonMapping = mock(ModalMapping.class);
		when(reasonMapping.getAsStringList()).thenReturn(List.of("teamkill"));
		when(event.getValue(ReportPenaltyCommand.FIELD_REASON)).thenReturn(reasonMapping);

		// the reported member always has the penalty role
		when(permissionService.isAllowedForCommand(guild, affected, "penalty")).thenReturn(true);

		Penalty saved = mock(Penalty.class, Answers.RETURNS_DEEP_STUBS);
		when(saved.getPenaltyType().getDisplayName()).thenReturn("Teamkill");
		when(penaltyRepository.save(any(), eq("teamkill"))).thenReturn(Optional.of(saved));
		when(penaltyRepository.saveCorrection(any(), eq("teamkill"))).thenReturn(Optional.of(saved));
	}

	private void input(String amount, String comment) {
		ModalMapping amountMapping = mock(ModalMapping.class);
		when(amountMapping.getAsString()).thenReturn(amount);
		when(event.getValue(ReportPenaltyCommand.FIELD_AMOUNT)).thenReturn(amountMapping);
		if (comment == null) {
			when(event.getValue(ReportPenaltyCommand.FIELD_COMMENT)).thenReturn(null);
		} else {
			ModalMapping commentMapping = mock(ModalMapping.class);
			when(commentMapping.getAsString()).thenReturn(comment);
			when(event.getValue(ReportPenaltyCommand.FIELD_COMMENT)).thenReturn(commentMapping);
		}
	}

	private String reply() {
		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(event).reply(text.capture());
		return text.getValue();
	}

	@Test
	void positiveAmountIsBookedWithoutComment() {
		input("2", null);
		listener.handleModalInteraction(event, guild);
		verify(penaltyRepository).save(any(), eq("teamkill"));
		verify(penaltyRepository, never()).saveCorrection(any(), any());
		System.out.println("[positive]            " + reply());
	}

	@Test
	void negativeAmountWithoutRoleIsRejected() {
		when(permissionService.isAllowedForCommand(guild, author, PenaltyCorrectionPermission.COMMAND_NAME)).thenReturn(false);
		input("-10", "wrongly reported");
		listener.handleModalInteraction(event, guild);
		verify(penaltyRepository, never()).saveCorrection(any(), any());
		System.out.println("[negative, no role]   " + reply());
		assertTrue(reply().contains("not allowed"));
	}

	@Test
	void negativeAmountWithRoleButNoCommentIsRejected() {
		when(permissionService.isAllowedForCommand(guild, author, PenaltyCorrectionPermission.COMMAND_NAME)).thenReturn(true);
		input("-10", "   ");
		listener.handleModalInteraction(event, guild);
		verify(penaltyRepository, never()).saveCorrection(any(), any());
		System.out.println("[negative, no comment] " + reply());
		assertTrue(reply().contains("comment is required"));
	}

	@Test
	void negativeAmountWithRoleAndCommentIsBookedAsCorrection() {
		when(permissionService.isAllowedForCommand(guild, author, PenaltyCorrectionPermission.COMMAND_NAME)).thenReturn(true);
		input("-10", "Teamkill was a misclick");
		listener.handleModalInteraction(event, guild);

		ArgumentCaptor<Penalty> draft = ArgumentCaptor.forClass(Penalty.class);
		verify(penaltyRepository).saveCorrection(draft.capture(), eq("teamkill"));
		verify(penaltyRepository, never()).save(any(), any());
		System.out.println("[negative, ok]         " + reply().replace("\n", " | "));
		assertTrue(draft.getValue().getAmount() == -10);
		assertTrue("Teamkill was a misclick".equals(draft.getValue().getComment()));
	}
}
