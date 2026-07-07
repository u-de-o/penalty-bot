package com.esemudeo.quarkus.penaltybot.configuration;

import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService.MemberPenaltyEntry;
import com.esemudeo.quarkus.penaltybot.penalty.PenaltyOverviewService.OverviewTable;
import com.vaadin.flow.server.VaadinSession;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches already-fetched penalty overview data (per guild + month) and member detail
 * entries (per guild + member + month) for the lifetime of the browser session.
 *
 * Vaadin is server-rendered rather than a REST-backed SPA, so there is no browser HTTP
 * cache to lean on. Backing this by {@link VaadinSession} attributes (the same pattern
 * as {@link AuthSession}) instead of a view-instance field means the cache survives
 * navigating away (e.g. to the member detail page) and back, since Vaadin creates a
 * fresh view instance per navigation but keeps the same session.
 */
@ApplicationScoped
public class PenaltyOverviewCache {

    private static final String OVERVIEW_KEY = "penaltyOverview.cache.overview";
    private static final String MEMBER_ENTRIES_KEY = "penaltyOverview.cache.memberEntries";

    private record OverviewCacheKey(long guildId, YearMonth month) {}

    private record MemberEntriesCacheKey(long guildId, long memberId, YearMonth month) {}

    public Optional<OverviewTable> getOverview(long guildId, YearMonth month) {
        return Optional.ofNullable(overviewCache().get(new OverviewCacheKey(guildId, month)));
    }

    public void putOverview(long guildId, YearMonth month, OverviewTable table) {
        overviewCache().put(new OverviewCacheKey(guildId, month), table);
    }

    public Optional<List<MemberPenaltyEntry>> getMemberEntries(long guildId, long memberId, YearMonth month) {
        return Optional.ofNullable(memberEntriesCache().get(new MemberEntriesCacheKey(guildId, memberId, month)));
    }

    public void putMemberEntries(long guildId, long memberId, YearMonth month, List<MemberPenaltyEntry> entries) {
        memberEntriesCache().put(new MemberEntriesCacheKey(guildId, memberId, month), entries);
    }

    /**
     * Drops the cached overview and any per-member detail entries for one guild + month,
     * so the next access re-fetches fresh data. Used by the "Refresh data" actions.
     */
    public void invalidate(long guildId, YearMonth month) {
        overviewCache().remove(new OverviewCacheKey(guildId, month));
        memberEntriesCache().keySet().removeIf(key -> key.guildId() == guildId && key.month().equals(month));
    }

    @SuppressWarnings("unchecked")
    private Map<OverviewCacheKey, OverviewTable> overviewCache() {
        VaadinSession session = VaadinSession.getCurrent();
        Map<OverviewCacheKey, OverviewTable> cache = (Map<OverviewCacheKey, OverviewTable>) session.getAttribute(OVERVIEW_KEY);
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            session.setAttribute(OVERVIEW_KEY, cache);
        }
        return cache;
    }

    @SuppressWarnings("unchecked")
    private Map<MemberEntriesCacheKey, List<MemberPenaltyEntry>> memberEntriesCache() {
        VaadinSession session = VaadinSession.getCurrent();
        Map<MemberEntriesCacheKey, List<MemberPenaltyEntry>> cache =
                (Map<MemberEntriesCacheKey, List<MemberPenaltyEntry>>) session.getAttribute(MEMBER_ENTRIES_KEY);
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            session.setAttribute(MEMBER_ENTRIES_KEY, cache);
        }
        return cache;
    }
}
