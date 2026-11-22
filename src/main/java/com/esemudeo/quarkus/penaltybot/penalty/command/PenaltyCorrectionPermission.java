package com.esemudeo.quarkus.penaltybot.penalty.command;

import com.esemudeo.quarkus.penaltybot.shared.command.GuildCommand;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Not a Discord command: it only registers the {@value #COMMAND_NAME} permission, so guilds can assign a
 * dedicated role in the setup panel that is allowed to book negative penalties (corrections) via {@code /penalty}.
 * Without configuration only the owner and administrators are allowed.
 */
@ApplicationScoped
public class PenaltyCorrectionPermission implements GuildCommand {

	public static final String COMMAND_NAME = "penalty-remove";

	@Override
	public String getName() {
		return COMMAND_NAME;
	}
}
