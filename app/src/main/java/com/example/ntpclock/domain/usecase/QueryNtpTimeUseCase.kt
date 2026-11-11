package com.example.ntpclock.domain.usecase

import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.ntp.NtpMultiSampleOutcome
import com.example.ntpclock.ntp.NtpSampleSelector

class QueryNtpTimeUseCase(
    private val sampleSelector: NtpSampleSelector,
) {
    suspend operator fun invoke(settings: NtpSettings): NtpMultiSampleOutcome =
        sampleSelector.queryBestSample(
            config = settings.toQueryConfig(),
            sampleCount = settings.sampleCount,
            sampleIntervalMs = settings.sampleIntervalMs,
        )
}

