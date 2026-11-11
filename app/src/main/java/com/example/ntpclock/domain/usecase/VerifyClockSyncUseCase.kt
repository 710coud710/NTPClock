package com.example.ntpclock.domain.usecase

import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.ntp.NtpSample
import kotlin.math.abs

data class ClockVerificationResult(
    val afterEpochMillis: Long,
    val expectedEpochMillis: Long,
    val diffMillis: Long,
    val isWithinTolerance: Boolean,
)

class VerifyClockSyncUseCase(
    private val clockProvider: ClockProvider,
    private val toleranceMillis: Long = DEFAULT_TOLERANCE_MILLIS,
) {
    operator fun invoke(sample: NtpSample): ClockVerificationResult {
        val afterElapsedNanos = clockProvider.elapsedRealtimeNanos()
        val afterEpochMillis = clockProvider.currentTimeMillis()
        val elapsedFromT4Millis =
            ((afterElapsedNanos - sample.t4ElapsedNanos).coerceAtLeast(0L)) / 1_000_000L
        val expectedNowMillis = sample.estimatedServerTimeAtT4Millis + elapsedFromT4Millis
        val diffMillis = afterEpochMillis - expectedNowMillis
        return ClockVerificationResult(
            afterEpochMillis = afterEpochMillis,
            expectedEpochMillis = expectedNowMillis,
            diffMillis = diffMillis,
            isWithinTolerance = abs(diffMillis) <= toleranceMillis,
        )
    }

    companion object {
        const val DEFAULT_TOLERANCE_MILLIS = 2_000L
    }
}

