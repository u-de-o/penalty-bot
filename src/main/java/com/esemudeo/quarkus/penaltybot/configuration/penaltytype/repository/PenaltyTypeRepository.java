package com.esemudeo.quarkus.penaltybot.configuration.penaltytype.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.model.PenaltyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PenaltyTypeRepository {

    /** Only active, non-deleted types – used by the bot when presenting options to users. */
    public List<PenaltyType> findActiveByGuild(long guildId) {
        return PenaltyType.find("guildId = ?1 and active = true and deleted = false order by id", guildId).list();
    }

    /**
     * Active and inactive types, excluding deleted – used by the settings UI. Ordered by
     * id (creation/entry date order) so the list stays stable across saves. Without an
     * explicit order, Postgres row order is undefined and can visibly shuffle after the
     * UPDATEs a save performs (e.g. unsetDefaultForGuild touches every row).
     */
    public List<PenaltyType> findAllByGuild(long guildId) {
        return PenaltyType.find("guildId = ?1 and deleted = false order by id", guildId).list();
    }

    /** Internal lookup by technical name (UUID), used when saving a penalty. */
    public Optional<PenaltyType> findByGuildAndTechnicalName(long guildId, String technicalName) {
        return PenaltyType.find("guildId = ?1 and technicalName = ?2", guildId, technicalName).firstResultOptional();
    }

    /**
     * Creates a new active penalty type with an auto-generated UUID as technical name.
     * If {@code isDefault} is true, all other types for this guild are unset as default first.
     */
    public PenaltyType create(long guildId, String displayName, boolean isDefault, Integer price) {
        if (isDefault) {
            unsetDefaultForGuild(guildId);
        }
        PenaltyType penaltyType = PenaltyType.builder()
                .guildId(guildId)
                .technicalName(UUID.randomUUID().toString())
                .displayName(displayName)
                .defaultType(isDefault)
                .price(price)
                .active(true)
                .build();
        penaltyType.persist();
        return penaltyType;
    }

    /** Creates the guild default "Teamkill" type if no active types exist yet. */
    public void createDefaultIfNoneExist(long guildId) {
        if (findActiveByGuild(guildId).isEmpty()) {
            create(guildId, "Teamkill", true, null);
        }
    }

    /**
     * Updates display name, default flag, and price of an existing type.
     * guildId is included in the WHERE clause – a mismatched ID silently affects nothing.
     * If {@code isDefault} is true, all other types for this guild are unset as default first.
     */
    public void update(long id, long guildId, String displayName, boolean isDefault, Integer price) {
        if (isDefault) {
            unsetDefaultForGuild(guildId);
        }
        PenaltyType.update("displayName = ?1, defaultType = ?2, price = ?3 where id = ?4 and guildId = ?5",
                displayName, isDefault, price, id, guildId);
    }

    public void update(long id, long guildId, String displayName, boolean isDefault, Integer price, boolean active) {
        if (isDefault) {
            unsetDefaultForGuild(guildId);
        }
        PenaltyType.update("displayName = ?1, defaultType = ?2, price = ?3, active = ?4 where id = ?5 and guildId = ?6",
                displayName, isDefault, price, active, id, guildId);
    }

    /**
     * Soft-deletes a penalty type by marking it as inactive.
     * guildId is included in the WHERE clause – a mismatched ID silently affects nothing.
     */
    public void deactivate(long id, long guildId) {
        PenaltyType.update("active = false, defaultType = false where id = ?1 and guildId = ?2", id, guildId);
    }

    /**
     * Permanently hides a penalty type from both the bot and the settings UI.
     * Used as a fallback when hard-delete is blocked by a foreign key constraint.
     * guildId is included in the WHERE clause – a mismatched ID silently affects nothing.
     */
    public void markAsDeleted(long id, long guildId) {
        PenaltyType.update("deleted = true, active = false, defaultType = false where id = ?1 and guildId = ?2", id, guildId);
    }

    /**
     * Deletes a penalty type if it is not referenced by any penalty. Throws a PersistenceException if it is.
     * guildId is included in the WHERE clause – a mismatched ID silently affects nothing.
     */
    public void delete(long id, long guildId) {
        PenaltyType.delete("id = ?1 and guildId = ?2", id, guildId);
    }

    private void unsetDefaultForGuild(long guildId) {
        PenaltyType.update("defaultType = false where guildId = ?1", guildId);
    }
}
