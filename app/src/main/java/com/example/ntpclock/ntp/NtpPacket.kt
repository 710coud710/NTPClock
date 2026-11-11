package com.example.ntpclock.ntp

/**
 * Represents a 48-byte NTPv4 packet (RFC 5905) and its validation rules.
 */
data class NtpPacket(
    val leapIndicator: Int,
    val version: Int,
    val mode: Int,
    val stratum: Int,
    val poll: Int,
    val precision: Int,
    val rootDelay: Int,
    val rootDispersion: Int,
    val referenceId: Int,
    val referenceTimestamp: Long,
    val originateTimestamp: Long,
    val receiveTimestamp: Long,
    val transmitTimestamp: Long,
) {
    companion object {
        const val PACKET_SIZE_BYTES = 48
        const val NTP_VERSION_4 = 4
        const val MODE_CLIENT = 3
        const val MODE_SERVER = 4
        const val LEAP_UNSYNCHRONIZED = 3

        fun encodeClientRequest(transmitNtpTimestamp: Long): ByteArray {
            val buffer = ByteArray(PACKET_SIZE_BYTES)
            // LI = 0 (no warning), VN = 4, Mode = 3 (client) -> 0x23
            buffer[0] = ((0 shl 6) or (NTP_VERSION_4 shl 3) or MODE_CLIENT).toByte()
            writeLongBigEndian(buffer, 40, transmitNtpTimestamp)
            return buffer
        }

        fun decode(bytes: ByteArray): NtpPacket {
            require(bytes.size >= PACKET_SIZE_BYTES) {
                "NTP packet too short: ${bytes.size} bytes (expected >= $PACKET_SIZE_BYTES)"
            }
            val firstByte = bytes[0].toInt() and 0xFF
            return NtpPacket(
                leapIndicator = (firstByte ushr 6) and 0x03,
                version = (firstByte ushr 3) and 0x07,
                mode = firstByte and 0x07,
                stratum = bytes[1].toInt() and 0xFF,
                poll = bytes[2].toInt(),
                precision = bytes[3].toInt(),
                rootDelay = readIntBigEndian(bytes, 4),
                rootDispersion = readIntBigEndian(bytes, 8),
                referenceId = readIntBigEndian(bytes, 12),
                referenceTimestamp = readLongBigEndian(bytes, 16),
                originateTimestamp = readLongBigEndian(bytes, 24),
                receiveTimestamp = readLongBigEndian(bytes, 32),
                transmitTimestamp = readLongBigEndian(bytes, 40),
            )
        }

        fun validateResponse(
            packet: NtpPacket,
            expectedOriginateNtpTimestamp: Long,
            rttMillis: Long,
            maxRttMillis: Long,
        ): NtpValidationResult {
            if (packet.version !in 3..4) {
                return NtpValidationResult.Invalid("NTP version không hợp lệ: ${packet.version}")
            }
            if (packet.mode != MODE_SERVER) {
                return NtpValidationResult.Invalid("NTP mode không phải server (4): ${packet.mode}")
            }
            if (packet.leapIndicator == LEAP_UNSYNCHRONIZED) {
                return NtpValidationResult.Invalid("Server báo Leap Indicator = 3 (unsynchronized)")
            }
            if (packet.stratum !in 1..15) {
                return NtpValidationResult.Invalid("Stratum ngoài phạm vi 1..15: ${packet.stratum}")
            }
            if (packet.originateTimestamp != expectedOriginateNtpTimestamp) {
                return NtpValidationResult.Invalid("Originate timestamp không khớp request")
            }
            if (packet.receiveTimestamp == 0L || packet.transmitTimestamp == 0L) {
                return NtpValidationResult.Invalid("Server timestamp (T2/T3) bằng 0")
            }
            if (rttMillis < 0L || rttMillis > maxRttMillis) {
                return NtpValidationResult.Invalid("RTT vượt ngưỡng cho phép: ${rttMillis}ms (max ${maxRttMillis}ms)")
            }
            return NtpValidationResult.Valid
        }

        private fun readIntBigEndian(buffer: ByteArray, offset: Int): Int =
            ((buffer[offset].toInt() and 0xFF) shl 24) or
                ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
                ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
                (buffer[offset + 3].toInt() and 0xFF)

        private fun readLongBigEndian(buffer: ByteArray, offset: Int): Long {
            var value = 0L
            for (i in 0 until 8) {
                value = (value shl 8) or (buffer[offset + i].toLong() and 0xFFL)
            }
            return value
        }

        private fun writeLongBigEndian(buffer: ByteArray, offset: Int, value: Long) {
            for (i in 0 until 8) {
                val shift = (7 - i) * 8
                buffer[offset + i] = ((value ushr shift) and 0xFFL).toByte()
            }
        }
    }
}

sealed interface NtpValidationResult {
    data object Valid : NtpValidationResult
    data class Invalid(val reason: String) : NtpValidationResult
}

