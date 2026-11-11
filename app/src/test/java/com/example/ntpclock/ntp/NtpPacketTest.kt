package com.example.ntpclock.ntp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NtpPacketTest {

    @Test
    fun `encodeClientRequest produces 48-byte NTPv4 client packet with transmit timestamp`() {
        val t1Ntp = NtpTimestamp.toNtpTimestamp(1_794_380_000_250L)
        val bytes = NtpPacket.encodeClientRequest(t1Ntp)

        assertEquals(48, bytes.size)
        assertEquals(0x23.toByte(), bytes[0])

        val decoded = NtpPacket.decode(bytes)
        assertEquals(0, decoded.leapIndicator)
        assertEquals(4, decoded.version)
        assertEquals(3, decoded.mode)
        assertEquals(t1Ntp, decoded.transmitTimestamp)
    }

    @Test
    fun `validateResponse accepts valid server response`() {
        val originate = NtpTimestamp.toNtpTimestamp(1_794_380_000_000L)
        val serverTime = NtpTimestamp.toNtpTimestamp(1_794_380_005_000L)
        val packet = buildServerPacket(
            originate = originate,
            receive = serverTime,
            transmit = serverTime,
        )

        val result = NtpPacket.validateResponse(
            packet = packet,
            expectedOriginateNtpTimestamp = originate,
            rttMillis = 15L,
            maxRttMillis = 2000L,
        )
        assertEquals(NtpValidationResult.Valid, result)
    }

    @Test
    fun `validateResponse rejects mismatched originate timestamp`() {
        val expected = NtpTimestamp.toNtpTimestamp(1_794_380_000_000L)
        val wrong = NtpTimestamp.toNtpTimestamp(1_794_380_001_000L)
        val serverTime = NtpTimestamp.toNtpTimestamp(1_794_380_005_000L)
        val packet = buildServerPacket(originate = wrong, receive = serverTime, transmit = serverTime)

        val result = NtpPacket.validateResponse(packet, expected, rttMillis = 10L, maxRttMillis = 2000L)
        assertTrue(result is NtpValidationResult.Invalid)
    }

    @Test
    fun `validateResponse rejects unsynchronized leap indicator and stratum 0`() {
        val expected = NtpTimestamp.toNtpTimestamp(1_794_380_000_000L)
        val serverTime = NtpTimestamp.toNtpTimestamp(1_794_380_005_000L)

        val unsyncPacket = buildServerPacket(
            leapIndicator = 3,
            originate = expected,
            receive = serverTime,
            transmit = serverTime,
        )
        assertTrue(
            NtpPacket.validateResponse(unsyncPacket, expected, 10L, 2000L) is NtpValidationResult.Invalid
        )

        val stratumZeroPacket = buildServerPacket(
            stratum = 0,
            originate = expected,
            receive = serverTime,
            transmit = serverTime,
        )
        assertTrue(
            NtpPacket.validateResponse(stratumZeroPacket, expected, 10L, 2000L) is NtpValidationResult.Invalid
        )
    }

    private fun buildServerPacket(
        leapIndicator: Int = 0,
        version: Int = 4,
        mode: Int = 4,
        stratum: Int = 2,
        originate: Long,
        receive: Long,
        transmit: Long,
    ): NtpPacket = NtpPacket(
        leapIndicator = leapIndicator,
        version = version,
        mode = mode,
        stratum = stratum,
        poll = 6,
        precision = -20,
        rootDelay = 0,
        rootDispersion = 0,
        referenceId = 0,
        referenceTimestamp = receive,
        originateTimestamp = originate,
        receiveTimestamp = receive,
        transmitTimestamp = transmit,
    )
}

