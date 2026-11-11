package com.example.ntpclock.core.common

import android.os.Build

/**
 * Snapshot of hardware/firmware metadata required by Phase 0 compatibility checks.
 */
data class DeviceProfile(
    val model: String,
    val manufacturer: String,
    val device: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildFingerprint: String,
) {
    companion object {
        fun current(): DeviceProfile = DeviceProfile(
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
        )
    }
}

