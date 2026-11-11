package com.example.ntpclock.ntp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NtpTimestampTest {

    @Test
    fun `toNtpTimestamp and toEpochMillis round trip within 1ms for 2026 epoch`() {
        // 2026-11-11T06:53:16.750Z
        val originalMillis = 1_794_380_000_750L
        val ntp = NtpTimestamp.toNtpTimestamp(originalMillis)
        val decodedMillis = NtpTimestamp.toEpochMillis(ntp)

        assertTrue(
            "Expected decoded=$decodedMillis within 1ms of $originalMillis",
            abs(decodedMillis - originalMillis) <= 1L,
        )
    }

    @Test
    fun `toEpochMillis handles NTP era 1 rollover after 2036`() {
        // 2037-01-01T00:00:00Z -> unixSeconds = 2_114_380_800L
        val unixSeconds2037 = 2_114_380_800L
        val epochMillis2037 = unixSeconds2037 * 1000L
        val ntp2037 = NtpTimestamp.toNtpTimestamp(epochMillis2037)

        // MSB bit 63 should be 0 after 2036-02-07 rollover
        val ntpSeconds = (ntp2037 ushr 32) and 0xFFFF_FFFFL
        assertTrue((ntpSeconds and 0x8000_0000L) == 0L)

        val decoded = NtpTimestamp.toEpochMillis(ntp2037)
        assertEquals(epochMillis2037, decoded)
    }
}

