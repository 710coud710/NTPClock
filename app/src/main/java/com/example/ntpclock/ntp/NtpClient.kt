package com.example.ntpclock.ntp

import com.example.ntpclock.core.time.ClockProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class NtpQueryConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    val maxRttMs: Long = DEFAULT_MAX_RTT_MS,
) {
    companion object {
        const val DEFAULT_HOST = "192.168.4.2"
        const val DEFAULT_PORT = 123
        const val DEFAULT_TIMEOUT_MS = 2500
        const val DEFAULT_MAX_RTT_MS = 2000L
    }
}

data class NtpSample(
    val serverHost: String,
    val serverPort: Int,
    val version: Int,
    val stratum: Int,
    val leapIndicator: Int,
    val t1EpochMillis: Long,
    val t2EpochMillis: Long,
    val t3EpochMillis: Long,
    val t4EpochMillis: Long,
    val t4ElapsedNanos: Long,
    val offsetMillis: Long,
    val roundTripDelayMillis: Long,
    val estimatedServerTimeAtT4Millis: Long,
)

sealed interface NtpQueryOutcome {
    data class Success(val sample: NtpSample) : NtpQueryOutcome
    data class Failure(val reason: String, val cause: Throwable? = null) : NtpQueryOutcome
}

interface NtpClient {
    suspend fun query(config: NtpQueryConfig): NtpQueryOutcome
}

/**
 * UDP NTPv4 client. Captures T1/T4 adjacent to socket I/O and measures RTT via monotonic clock.
 */
class UdpNtpClient(
    private val clockProvider: ClockProvider,
) : NtpClient {

    override suspend fun query(config: NtpQueryConfig): NtpQueryOutcome = withContext(Dispatchers.IO) {
        if (config.host.isBlank()) {
            return@withContext NtpQueryOutcome.Failure("Địa chỉ NTP host không được để trống")
        }
        if (config.port !in 1..65535) {
            return@withContext NtpQueryOutcome.Failure("Port NTP không hợp lệ: ${config.port}")
        }

        try {
            val serverAddress = InetAddress.getByName(config.host.trim())
            DatagramSocket().use { socket ->
                socket.soTimeout = config.timeoutMs
                // Connect socket to restrict incoming datagrams to the configured NTP endpoint.
                socket.connect(serverAddress, config.port)

                val t1EpochMillis = clockProvider.currentTimeMillis()
                val t1Nanos = clockProvider.elapsedRealtimeNanos()
                val originateNtp = NtpTimestamp.toNtpTimestamp(t1EpochMillis)
                // Reconstruct T1 from the transmitted 64-bit NTP value so T1 matches server's echoed originate timestamp.
                val t1QuantizedMillis = NtpTimestamp.toEpochMillis(originateNtp)

                val requestBytes = NtpPacket.encodeClientRequest(originateNtp)
                val requestPacket = DatagramPacket(requestBytes, requestBytes.size)
                val responseBytes = ByteArray(NtpPacket.PACKET_SIZE_BYTES)
                val responsePacket = DatagramPacket(responseBytes, responseBytes.size)

                socket.send(requestPacket)
                socket.receive(responsePacket)

                val t4Nanos = clockProvider.elapsedRealtimeNanos()
                val localElapsedMillis = ((t4Nanos - t1Nanos).coerceAtLeast(0L)) / 1_000_000L
                val t4WallEstimateMillis = t1QuantizedMillis + localElapsedMillis

                val packet = NtpPacket.decode(responseBytes.copyOf(responsePacket.length))
                val validation = NtpPacket.validateResponse(
                    packet = packet,
                    expectedOriginateNtpTimestamp = originateNtp,
                    rttMillis = localElapsedMillis,
                    maxRttMillis = config.maxRttMs,
                )
                if (validation is NtpValidationResult.Invalid) {
                    return@withContext NtpQueryOutcome.Failure(validation.reason)
                }

                val t2EpochMillis = NtpTimestamp.toEpochMillis(packet.receiveTimestamp)
                val t3EpochMillis = NtpTimestamp.toEpochMillis(packet.transmitTimestamp)
                val serverProcessingMillis = (t3EpochMillis - t2EpochMillis).coerceAtLeast(0L)
                val delayMillis = (localElapsedMillis - serverProcessingMillis).coerceAtLeast(0L)
                val offsetMillis = ((t2EpochMillis - t1QuantizedMillis) + (t3EpochMillis - t4WallEstimateMillis)) / 2L
                val estimatedServerAtT4Millis = t4WallEstimateMillis + offsetMillis

                NtpQueryOutcome.Success(
                    NtpSample(
                        serverHost = config.host.trim(),
                        serverPort = config.port,
                        version = packet.version,
                        stratum = packet.stratum,
                        leapIndicator = packet.leapIndicator,
                        t1EpochMillis = t1QuantizedMillis,
                        t2EpochMillis = t2EpochMillis,
                        t3EpochMillis = t3EpochMillis,
                        t4EpochMillis = t4WallEstimateMillis,
                        t4ElapsedNanos = t4Nanos,
                        offsetMillis = offsetMillis,
                        roundTripDelayMillis = delayMillis,
                        estimatedServerTimeAtT4Millis = estimatedServerAtT4Millis,
                    )
                )
            }
        } catch (e: Exception) {
            NtpQueryOutcome.Failure("Lỗi kết nối NTP (${config.host}:${config.port}): ${e.message ?: e::class.simpleName}", e)
        }
    }
}

