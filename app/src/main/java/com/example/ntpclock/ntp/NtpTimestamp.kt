package com.example.ntpclock.ntp

/**
 * Converts between 64-bit NTP timestamps (epoch 1900-01-01) and Unix epoch milliseconds (1970-01-01).
 */
object NtpTimestamp {
    const val NTP_EPOCH_OFFSET_SECONDS: Long = 2_208_988_800L
    private const val ERA_OFFSET_SECONDS: Long = 0x1_0000_0000L
    private const val MSB_MASK: Long = 0x8000_0000L
    private const val UINT32_MASK: Long = 0xFFFF_FFFFL

    fun toNtpTimestamp(epochMillis: Long): Long {
        val unixSeconds = Math.floorDiv(epochMillis, 1000L)
        val millisPart = Math.floorMod(epochMillis, 1000L).toLong()
        val ntpSeconds = (unixSeconds + NTP_EPOCH_OFFSET_SECONDS) and UINT32_MASK
        val fraction = (millisPart * ERA_OFFSET_SECONDS) / 1000L
        return (ntpSeconds shl 32) or (fraction and UINT32_MASK)
    }

    /**
     * Converts an NTP 64-bit timestamp to Unix epoch milliseconds using RFC 4330/5905 MSB era convention:
     * MSB == 1 maps to 1968..2036 (era 0), MSB == 0 maps to 2036..2104 (era 1).
     */
    fun toEpochMillis(ntpTimestamp: Long): Long {
        val ntpSeconds = (ntpTimestamp ushr 32) and UINT32_MASK
        val fraction = ntpTimestamp and UINT32_MASK
        val secondsSince1900 = if ((ntpSeconds and MSB_MASK) != 0L) {
            ntpSeconds
        } else {
            ntpSeconds + ERA_OFFSET_SECONDS
        }
        val unixSeconds = secondsSince1900 - NTP_EPOCH_OFFSET_SECONDS
        val millisPart = (fraction * 1000L + MSB_MASK) ushr 32
        return unixSeconds * 1000L + millisPart
    }
}

