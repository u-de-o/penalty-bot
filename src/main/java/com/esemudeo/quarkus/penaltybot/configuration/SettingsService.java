package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import com.esemudeo.quarkus.penaltybot.shared.JDAInstance;
import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.model.CommandPermission;
import com.esemudeo.quarkus.penaltybot.configuration.commandpermission.repository.CommandRepository;
import com.esemudeo.quarkus.penaltybot.configuration.global.model.GlobalGuildConfig;
import com.esemudeo.quarkus.penaltybot.configuration.global.repository.GlobalGuildConfigRepository;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.model.PenaltyType;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.repository.PenaltyTypeRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleColors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Service layer between the Vaadin settings UI and the repositories.
 * The guildId is always sourced from the authenticated session and never accepted from callers.
 * For single-entity operations the guildId is also included in the WHERE clause, so a
 * manipulated entity ID from a different guild silently affects nothing.
 */
@ApplicationScoped
public class SettingsService {

    @Inject
    AuthSession authSession;

    @Inject
    PenaltyTypeRepository penaltyTypeRepository;

    @Inject
    CommandRepository commandRepository;

    @Inject
    GlobalGuildConfigRepository globalGuildConfigRepository;

    @Inject
    JDAInstance jdaInstance;

    @Inject
    GuildAccessService guildAccessService;

    // --- Authentication ---

    /**
     * Re-verifies that the authenticated user still passes the access gate for the
     * guild currently held in the session. Called on every settings entry so that a
     * tampered session guildId or a revoked role is rejected live.
     */
    public void assertCanAccessCurrentGuild() {
        long guildId = guildId();
        if (!guildAccessService.canAccess(authSession.getUserId(), guildId)) {
            throw new SettingsAccessDeniedException("Not allowed to configure this guild");
        }
    }

    public String getGuildName() {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            throw new IllegalStateException("Guild not found");
        }
        return guild.getName();
    }

    public String getMemberDisplayName() {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            throw new IllegalStateException("Guild not found");
        }
        return guild.retrieveMemberById(authSession.getUserId()).complete().getEffectiveName();
    }

    // --- Guild roles ---

    public record GuildRole(long id, String name, String hexColor) {}

    public List<GuildRole> getGuildRoles() {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            return List.of();
        }
        // getRoles() returns roles sorted highest to lowest by position
        return guild.getRoles().stream()
                .filter(Predicate.not(Role::isManaged))
                .map(r -> new GuildRole(r.getIdLong(), r.getName(), roleHexColor(r)))
                .toList();
    }

    public Optional<GuildRole> getGuildRoleById(long roleId) {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            return Optional.empty();
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return Optional.empty();
        }
        return Optional.of(new GuildRole(role.getIdLong(), role.getName(), roleHexColor(role)));
    }

    public List<GuildRole> getGuildRolesByIds(java.util.Collection<Long> roleIds) {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            return List.of();
        }
        return roleIds.stream()
                .map(guild::getRoleById)
                .filter(Objects::nonNull)
                .map(r -> new GuildRole(r.getIdLong(), r.getName(), roleHexColor(r)))
                .toList();
    }

    private static final String HEX_COLOR_FORMAT = "#%06X";
    private static final int RGB_MASK = 0xFFFFFF;

    private String roleHexColor(Role role) {
        RoleColors colors = role.getColors();
        if (colors.isDefault()) {
            return null;
        }
        return String.format(HEX_COLOR_FORMAT, colors.getPrimaryRaw() & RGB_MASK);
    }

    // --- Penalty types ---

    public List<PenaltyType> getPenaltyTypes() {
        return penaltyTypeRepository.findAllByGuild(guildId());
    }

    public PenaltyType createPenaltyType(String displayName, boolean isDefault, Integer price) {
        return penaltyTypeRepository.create(guildId(), displayName, isDefault, price);
    }

    public void updatePenaltyType(long id, String displayName, boolean isDefault, Integer price) {
        penaltyTypeRepository.update(id, guildId(), displayName, isDefault, price);
    }

    public void updatePenaltyType(long id, String displayName, boolean isDefault, Integer price, boolean active) {
        penaltyTypeRepository.update(id, guildId(), displayName, isDefault, price, active);
    }

    public void makePenaltyTypeUnusable(long id) {
        long guildId = guildId();
        try {
            penaltyTypeRepository.delete(id, guildId);
        } catch (PersistenceException e) {
            penaltyTypeRepository.markAsDeleted(id, guildId);
        }
    }

    public List<PenaltyType> getAllPenaltyTypes() {
        return penaltyTypeRepository.findAllByGuild(guildId());
    }

    // --- Command permissions ---

    public List<CommandPermission> getCommands() {
        return commandRepository.findAllByGuild(guildId());
    }

    public void updateCommandMinRole(String commandName, Long minRoleId) {
        commandRepository.updateMinRole(guildId(), commandName, minRoleId);
    }

    public void replaceCommandExplicitRoles(String commandName, List<Long> roleIds) {
        commandRepository.setExplicitRoles(guildId(), commandName, roleIds);
    }

    // --- Global guild config ---

    public Optional<GlobalGuildConfig> getGlobalConfig() {
        return globalGuildConfigRepository.findByGuild(guildId());
    }

    public void updatePaypalMeUsername(String paypalUsername) {
        globalGuildConfigRepository.upsertPaypalMeUsername(guildId(), paypalUsername);
    }

    public record GuildTextChannel(long id, String name) {}

    public List<GuildTextChannel> getGuildTextChannels() {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            return List.of();
        }
        return guild.getTextChannels().stream()
                .map(c -> new GuildTextChannel(c.getIdLong(), c.getName()))
                .toList();
    }

    public Optional<GuildTextChannel> getGuildTextChannelById(long channelId) {
        Guild guild = jdaInstance.getJda().getGuildById(guildId());
        if (guild == null) {
            return Optional.empty();
        }
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            return Optional.empty();
        }
        return Optional.of(new GuildTextChannel(channel.getIdLong(), channel.getName()));
    }

    public void updateNotificationChannelId(Long channelId) {
        globalGuildConfigRepository.upsertNotificationChannelId(guildId(), channelId);
    }

    // ---

    public void setUiNonce(String nonce) {
        ComponentUtil.setData(UI.getCurrent(), String.class, nonce);
    }

    private long guildId() {
        if (authSession.isNotAuthenticated()) {
            throw new SettingsAccessDeniedException("Not authenticated");
        }
        String uiNonce = ComponentUtil.getData(UI.getCurrent(), String.class);
        if (!authSession.isActiveNonce(uiNonce)) {
            throw new SettingsAccessDeniedException("Session superseded by a newer tab");
        }
        return authSession.getGuildId();
    }
}
