package com.esemudeo.quarkus.penaltybot.penalty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * A half-open time range {@code [startInclusive, endExclusive)} used to aggregate
 * penalties. Aggregation is range-based so that a whole month (today's use case) and a
 * day-precise custom range (planned) share the same query path.
 */
public record DateRange(Instant startInclusive, Instant endExclusive) {

    public static DateRange ofMonth(YearMonth month) {
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DateRange(start, end);
    }

    /** Full days from {@code fromInclusive} to {@code toInclusive} (both inclusive), UTC. */
    public static DateRange ofDays(LocalDate fromInclusive, LocalDate toInclusive) {
        Instant start = fromInclusive.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toInclusive.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DateRange(start, end);
    }
}
