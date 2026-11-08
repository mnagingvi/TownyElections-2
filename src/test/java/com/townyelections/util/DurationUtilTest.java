package com.townyelections.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for parsing and formatting human-friendly durations. */
class DurationUtilTest {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60L * SECOND;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    private static final long WEEK = 7L * DAY;

    @Test
    void parsesSingleUnits() {
        assertEquals(30 * SECOND, DurationUtil.parseMillis("30s", -1));
        assertEquals(10 * MINUTE, DurationUtil.parseMillis("10m", -1));
        assertEquals(2 * HOUR, DurationUtil.parseMillis("2h", -1));
        assertEquals(3 * DAY, DurationUtil.parseMillis("3d", -1));
        assertEquals(1 * WEEK, DurationUtil.parseMillis("1w", -1));
    }

    @Test
    void parsesCombinedUnits() {
        assertEquals(WEEK + 3 * DAY + 12 * HOUR, DurationUtil.parseMillis("1w3d12h", -1));
        assertEquals(DAY + 2 * HOUR, DurationUtil.parseMillis("1d 2h", -1));
    }

    @Test
    void fallsBackOnMissingOrUnparsableInput() {
        assertEquals(5000L, DurationUtil.parseMillis(null, 5000L));
        assertEquals(5000L, DurationUtil.parseMillis("", 5000L));
        assertEquals(5000L, DurationUtil.parseMillis("soon", 5000L));
    }

    @Test
    void treatsZeroAsDisabled() {
        assertEquals(0L, DurationUtil.parseMillis("0", 12345L));
    }

    @Test
    void formatsDurationsCompactly() {
        assertEquals("0s", DurationUtil.format(0L));
        assertEquals("0s", DurationUtil.format(-1000L));
        assertEquals("45s", DurationUtil.format(45 * SECOND));
        assertEquals("1h", DurationUtil.format(HOUR));
        assertEquals("1m 1s", DurationUtil.format(MINUTE + SECOND));
        assertEquals("1d 1h 1m", DurationUtil.format(DAY + HOUR + MINUTE + SECOND));
    }
}
