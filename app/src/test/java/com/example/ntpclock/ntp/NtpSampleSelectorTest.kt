package com.example.ntpclock.ntp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NtpSampleSelectorTest {

    @Test
    fun `queryBestSample computes median offset and selects lowest RTT anchor`() = runTest {
        val samples = listOf(
            sample(offset = 100L, rtt = 25L, t4Wall = 1_000L),
            sample(offset = 500L, rtt = 30L, t4Wall = 1_250L), // high outlier
            sample(offset = 104L, rtt = 8L, t4Wall = 1_500L),  // lowest RTT
            sample(offset = 102L, rtt = 15L, t4Wall = 1_750L),
            sample(offset = 106L, rtt = 18L, t4Wall = 2_000L),
        )
        var idx = 0
        val fakeClient = object : NtpClient {
            override suspend fun query(config: NtpQueryConfig): NtpQueryOutcome =
                NtpQueryOutcome.Success(samples[idx++])
        }

        val selector = NtpSampleSelector(fakeClient)
        val result = selector.queryBestSample(
            config = NtpQueryConfig(),
            sampleCount = 5,
            sampleIntervalMs = 0L,
            maxOffsetSpreadMs = 1_000L,
        )

        assertTrue(result is NtpMultiSampleOutcome.Success)
        val summary = (result as NtpMultiSampleOutcome.Success).summary
        // Sorted offsets: [100, 102, 104, 106, 500] -> median = 104
        assertEquals(104L, summary.medianOffsetMillis)
        assertEquals(8L, summary.minRttMillis)
        assertEquals(5, summary.samplesValid)
        assertEquals(1_500L + 104L, summary.selectedSample.estimatedServerTimeAtT4Millis)
    }

    @Test
    fun `queryBestSample fails when valid sample count is below minimum`() = runTest {
        val outcomes = listOf(
            NtpQueryOutcome.Success(sample(10L, 10L, 1_000L)),
            NtpQueryOutcome.Failure("timeout"),
            NtpQueryOutcome.Failure("timeout"),
            NtpQueryOutcome.Success(sample(12L, 12L, 1_500L)),
            NtpQueryOutcome.Failure("timeout"),
        )
        var idx = 0
        val fakeClient = object : NtpClient {
            override suspend fun query(config: NtpQueryConfig): NtpQueryOutcome = outcomes[idx++]
        }

        val selector = NtpSampleSelector(fakeClient)
        val result = selector.queryBestSample(
            config = NtpQueryConfig(),
            sampleCount = 5,
            sampleIntervalMs = 0L,
        )

        assertTrue(result is NtpMultiSampleOutcome.Failure)
        val failure = result as NtpMultiSampleOutcome.Failure
        assertEquals(2, failure.samplesValid)
        assertEquals(5, failure.samplesAttempted)
    }

    @Test
    fun `queryBestSample fails when offset spread exceeds threshold`() = runTest {
        val samples = listOf(
            sample(offset = 10L, rtt = 10L, t4Wall = 1_000L),
            sample(offset = 3_000L, rtt = 10L, t4Wall = 1_250L),
            sample(offset = 20L, rtt = 10L, t4Wall = 1_500L),
        )
        var idx = 0
        val fakeClient = object : NtpClient {
            override suspend fun query(config: NtpQueryConfig): NtpQueryOutcome =
                NtpQueryOutcome.Success(samples[idx++])
        }

        val selector = NtpSampleSelector(fakeClient)
        val result = selector.queryBestSample(
            config = NtpQueryConfig(),
            sampleCount = 3,
            sampleIntervalMs = 0L,
            maxOffsetSpreadMs = 1_500L,
        )

        assertTrue(result is NtpMultiSampleOutcome.Failure)
    }

    private fun sample(offset: Long, rtt: Long, t4Wall: Long): NtpSample = NtpSample(
        serverHost = "192.168.4.2",
        serverPort = 123,
        version = 4,
        stratum = 2,
        leapIndicator = 0,
        t1EpochMillis = t4Wall - rtt,
        t2EpochMillis = t4Wall - rtt / 2 + offset,
        t3EpochMillis = t4Wall - rtt / 2 + offset,
        t4EpochMillis = t4Wall,
        t4ElapsedNanos = t4Wall * 1_000_000L,
        offsetMillis = offset,
        roundTripDelayMillis = rtt,
        estimatedServerTimeAtT4Millis = t4Wall + offset,
    )
}

