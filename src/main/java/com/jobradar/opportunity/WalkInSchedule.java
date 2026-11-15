package com.jobradar.opportunity;

import com.jobradar.common.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.List;

final class WalkInSchedule {
    private WalkInSchedule() {}

    static Instant toUtc(java.time.LocalDate date, LocalTime time, String timeZoneId) {
        try {
            LocalDateTime local = LocalDateTime.of(date, time.withNano(0));
            ZoneId zone = ZoneId.of(timeZoneId);
            ZoneRules rules = zone.getRules();
            List<ZoneOffset> offsets = rules.getValidOffsets(local);
            if (offsets.isEmpty()) throw new BadRequestException("Drive time falls inside a daylight-saving time gap");
            // When clocks repeat, choose the earlier offset consistently.
            return local.toInstant(offsets.getFirst());
        } catch (java.time.DateTimeException exception) {
            throw new BadRequestException("A valid IANA time zone and drive date/time are required");
        }
    }
}
