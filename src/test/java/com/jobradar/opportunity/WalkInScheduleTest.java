package com.jobradar.opportunity;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class WalkInScheduleTest {
    @Test void convertsLocalDriveTimeToUtcUsingItsPhysicalTimezone() {
        var instant = WalkInSchedule.toUtc(LocalDate.of(2026, 6, 2), LocalTime.of(9, 0), "Asia/Kolkata");
        assertEquals("2026-06-02T03:30:00Z", instant.toString());
    }

    @Test void rejectsDaylightSavingGapAndChoosesEarlierOffsetDuringOverlap() {
        assertThrows(com.jobradar.common.exception.BadRequestException.class, () -> WalkInSchedule.toUtc(
                LocalDate.of(2026, 3, 8), LocalTime.of(2, 30), "America/New_York"));
        var overlap = WalkInSchedule.toUtc(LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), "America/New_York");
        assertEquals("2026-11-01T05:30:00Z", overlap.toString());
    }
}
