package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.configuration.command.PenaltySetupCommand;
import com.esemudeo.quarkus.penaltybot.permission.PermissionService;
import com.esemudeo.quarkus.penaltybot.shared.JDAInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines which guilds a Discord user may configure: the bot must be present in the
 * guild and the user must pass the {@code penalty-setup} command permission
 * (owner / admin / min-role / explicit-role), evaluated live against Discord.
 */
@ApplicationScoped
public class GuildAccessService {

    private static final Logger LOG = Logger.getLogger(GuildAccessService.class);

    @Inject
    JDAInstance jdaInstance;

    @Inject
    PermissionService permissionService;

    @Inject
    PenaltySetupCommand penaltySetupCommand;

    public record AccessibleGuild(long id, String name, String iconUrl) {}

    public List<AccessibleGuild> accessibleGuilds(long userId) {
        List<AccessibleGuild> accessible = new ArrayList<>();
        for (Guild guild : jdaInstance.getJda().getGuilds()) {
            Member member = resolveMember(guild, userId);
            if (member == null) {
                continue;
            }
            if (permissionService.isAllowedForCommand(guild, member, penaltySetupCommand.getName())) {
                accessible.add(new AccessibleGuild(guild.getIdLong(), guild.getName(), guild.getIconUrl()));
            }
        }
        return accessible;
    }

    public boolean canAccess(long userId, long guildId) {
        Guild guild = jdaInstance.getJda().getGuildById(guildId);
        if (guild == null) {
            return false;
        }
        Member member = resolveMember(guild, userId);
        if (member == null) {
            return false;
        }
        return permissionService.isAllowedForCommand(guild, member, penaltySetupCommand.getName());
    }

    private Member resolveMember(Guild guild, long userId) {
        try {
            return guild.retrieveMemberById(userId).complete();
        } catch (Exception e) {
            // User is not a member of this guild (or lookup failed) – simply skip it.
            LOG.debugf("Could not resolve member %d in guild %d: %s", userId, guild.getIdLong(), e.getMessage());
            return null;
        }
    }
}
