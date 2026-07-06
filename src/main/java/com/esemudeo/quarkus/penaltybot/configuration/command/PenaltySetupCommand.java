package com.esemudeo.quarkus.penaltybot.configuration.command;

import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import com.esemudeo.quarkus.penaltybot.shared.command.SlashCommand;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Points users at the configuration website. The site handles Discord login and shows
 * the servers a user may configure, so no per-user link/token needs to be generated.
 * The command stays a {@link SlashCommand} so its name keeps seeding the
 * {@code penalty-setup} permission that gates web access to a guild's settings.
 */
@ApplicationScoped
public class PenaltySetupCommand implements SlashCommand {

	@ConfigProperty(name = "app.base-url")
	String baseUrl;

	@Override
	public String getName() {
		return "penalty-setup";
	}

	@Override
	public String getHelpDescription() {
		return "Open the configuration page for this server.";
	}

	@Override
	public void handleSlashCommand(SlashCommandInteractionEvent event) {
		event.reply("Configure Penalty Bot here: %s".formatted(baseUrl))
				.setEphemeral(true)
				.queue();
	}
}
