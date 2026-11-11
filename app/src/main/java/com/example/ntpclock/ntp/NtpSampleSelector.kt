package com.example.ntpclock.ntp

import kotlinx.coroutines.delay

data class NtpMultiSampleSummary(
    val selectedSample: NtpSample,
    val medianOffsetMillis: Long,
    val minRttMillis: Long,
    val maxRttMillis: Long,
    val offsetSpreadMillis: Long,
    val samplesAttempted: Int,
    val samplesValid: Int,
)

sealed interface NtpMultiSampleOutcome {
    data class Success(val summary: NtpMultiSampleSummary) : NtpMultiSampleOutcome
    data class Failure(
        val reason: String,
        val samplesAttempted: Int,
        val samplesValid: Int,
    ) : NtpMultiSampleOutcome
}

/**
 * Queries multiple NTP samples spaced by [sampleIntervalMs], filters invalid/high-spread samples,
 * and derives a robust offset using the median offset anchored to the lowest-RTT sample.
 */
class NtpSampleSelector(
    private val ntpClient: NtpClient,
) {
    suspend fun queryBestSample(
        config: NtpQueryConfig,
        sampleCount: Int = DEFAULT_SAMPLE_COUNT,
        sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
        maxOffsetSpreadMs: Long = DEFAULT_MAX_OFFSET_SPREAD_MS,
    ): NtpMultiSampleOutcome {
        val count = sampleCount.coerceIn(1, MAX_SAMPLE_COUNT)
        val minRequired = if (count >= 3) 3 else count
        val validSamples = mutableListOf<NtpSample>()
        var lastFailureReason: String? = null

        for (index in 0 until count) {
            when (val outcome = ntpClient.query(config)) {
                is NtpQueryOutcome.Success -> validSamples += outcome.sample
                is NtpQueryOutcome.Failure -> lastFailureReason = outcome.reason
            }
            if (index < count - 1 && sampleIntervalMs > 0L) {
                delay(sampleIntervalMs)
            }
        }

        if (validSamples.size < minRequired) {
            val detail = lastFailureReason?.let { " ($it)" }.orEmpty()
            return NtpMultiSampleOutcome.Failure(
                reason = "Không đủ mẫu NTP hợp lệ: ${validSamples.size}/$count (cần >= $minRequired)$detail",
                samplesAttempted = count,
                samplesValid = validSamples.size,
            )
        }

        val sortedOffsets = validSamples.map { it.offsetMillis }.sorted()
        val medianOffset = if (sortedOffsets.size % 2 == 1) {
            sortedOffsets[sortedOffsets.size / 2]
        } else {
            val mid = sortedOffsets.size / 2
            (sortedOffsets[mid - 1] + sortedOffsets[mid]) / 2L
        }
        val offsetSpread = sortedOffsets.last() - sortedOffsets.first()

        if (offsetSpread > maxOffsetSpreadMs) {
            return NtpMultiSampleOutcome.Failure(
                reason = "Độ phân tán offset giữa các mẫu quá lớn (${offsetSpread}ms > ${maxOffsetSpreadMs}ms)",
                samplesAttempted = count,
                samplesValid = validSamples.size,
            )
        }

        val lowestRttSample = validSamples.minBy { it.roundTripDelayMillis }
        val rtts = validSamples.map { it.roundTripDelayMillis }
        val anchoredSample = lowestRttSample.copy(
            offsetMillis = medianOffset,
            estimatedServerTimeAtT4Millis = lowestRttSample.t4EpochMillis + medianOffset,
        )

        return NtpMultiSampleOutcome.Success(
            NtpMultiSampleSummary(
                selectedSample = anchoredSample,
                medianOffsetMillis = medianOffset,
                minRttMillis = rtts.min(),
                maxRttMillis = rtts.max(),
                offsetSpreadMillis = offsetSpread,
                samplesAttempted = count,
                samplesValid = validSamples.size,
            )
        )
    }

    companion object {
        const val DEFAULT_SAMPLE_COUNT = 5
        const val MAX_SAMPLE_COUNT = 10
        const val DEFAULT_SAMPLE_INTERVAL_MS = 250L
        const val DEFAULT_MAX_OFFSET_SPREAD_MS = 1_500L
    }
}

