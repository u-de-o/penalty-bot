package com.esemudeo.quarkus.penaltybot.penalty;

import com.esemudeo.quarkus.penaltybot.configuration.global.model.GlobalGuildConfig;
import com.esemudeo.quarkus.penaltybot.configuration.global.repository.GlobalGuildConfigRepository;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.model.PenaltyType;
import com.esemudeo.quarkus.penaltybot.configuration.penaltytype.repository.PenaltyTypeRepository;
import com.esemudeo.quarkus.penaltybot.penalty.repository.PenaltyRepository;
import com.esemudeo.quarkus.penaltybot.penalty.repository.PenaltyRepository.PenaltyTypeSummary;
import com.esemudeo.quarkus.penaltybot.shared.JDAInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Guild;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the per-member penalty overview table for a guild over a {@link DateRange}.
 * Reuses {@link PenaltyRepository#aggregateByRangeForAllUsers} so the same code path
 * serves the monthly view today and a day-precise custom range later.
 */
@ApplicationScoped
public class PenaltyOverviewService {

    private static final Logger LOG = Logger.getLogger(PenaltyOverviewService.class);
    private static final String UNKNOWN_MEMBER_NAME = "Unknown member";

    @Inject
    PenaltyRepository penaltyRepository;

    @Inject
    PenaltyTypeRepository penaltyTypeRepository;

    @Inject
    GlobalGuildConfigRepository globalGuildConfigRepository;

    @Inject
    JDAInstance jdaInstance;

    public record MemberRow(long memberId, String displayName, Map<String, Integer> countsByType, int totalCents) {
        public int countFor(String penaltyTypeName) {
            return countsByType.getOrDefault(penaltyTypeName, 0);
        }
    }

    public record OverviewTable(List<String> typeColumns, List<MemberRow> rows, Optional<String> paypalUsername) {}

    public record MemberPenaltyEntry(Instant timestamp, String penaltyTypeName, int amount, String authorName) {}

    public OverviewTable overview(long guildId, DateRange range) {
        Map<Long, List<PenaltyTypeSummary>> summaryByMember =
                penaltyRepository.aggregateByRangeForAllUsers(guildId, range.startInclusive(), range.endExclusive());

        List<String> typeColumns = typeColumns(guildId);
        List<MemberRow> rows = summaryByMember.entrySet().stream()
                .map(entry -> toMemberRow(guildId, entry.getKey(), entry.getValue()))
                .toList();

        Optional<String> paypalUsername = globalGuildConfigRepository.findByGuild(guildId)
                .map(GlobalGuildConfig::getPaypalMeUsername);

        return new OverviewTable(typeColumns, rows, paypalUsername);
    }

    /** Individual entries for one member within a range, newest first (matches repository order). */
    public List<MemberPenaltyEntry> memberEntries(long guildId, long memberId, DateRange range) {
        return penaltyRepository.findByGuildMemberAndRange(guildId, memberId, range.startInclusive(), range.endExclusive())
                .stream()
                .map(p -> new MemberPenaltyEntry(p.getTimestamp(), p.getPenaltyType().getDisplayName(), p.getAmount(),
                        resolveMemberName(guildId, p.getAuthorId())))
                .toList();
    }

    private List<String> typeColumns(long guildId) {
        return penaltyTypeRepository.findAllByGuild(guildId).stream()
                .sorted(Comparator.comparing(PenaltyType::isDefaultType).reversed()
                        .thenComparing(PenaltyType::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(PenaltyType::getDisplayName)
                .distinct()
                .toList();
    }

    private MemberRow toMemberRow(long guildId, long memberId, List<PenaltyTypeSummary> summaries) {
        Map<String, Integer> countsByType = new HashMap<>();
        int totalCents = 0;
        for (PenaltyTypeSummary summary : summaries) {
            countsByType.merge(summary.displayName(), summary.totalAmount(), Integer::sum);
            if (summary.totalPriceCents() != null) {
                totalCents += summary.totalPriceCents();
            }
        }
        return new MemberRow(memberId, resolveMemberName(guildId, memberId), countsByType, totalCents);
    }

    /** Resolves the member's server-specific display name (nickname on this guild, else global name). */
    private String resolveMemberName(long guildId, long memberId) {
        Guild guild = jdaInstance.getJda().getGuildById(guildId);
        if (guild == null) {
            return UNKNOWN_MEMBER_NAME;
        }
        try {
            return guild.retrieveMemberById(memberId).complete().getEffectiveName();
        } catch (Exception e) {
            LOG.debugf("Could not resolve member %d in guild %d: %s", memberId, guildId, e.getMessage());
            return UNKNOWN_MEMBER_NAME;
        }
    }
}
