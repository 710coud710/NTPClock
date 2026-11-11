package com.example.ntpclock.core.time

import android.os.SystemClock

/**
 * Abstracts wall clock and monotonic clock reads so NTP and sync math can be unit tested.
 */
interface ClockProvider {
    fun currentTimeMillis(): Long
    fun elapsedRealtimeNanos(): Long
}

class SystemClockProvider : ClockProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeNanos(): Long = SystemClock.elapsedRealtimeNanos()
}

