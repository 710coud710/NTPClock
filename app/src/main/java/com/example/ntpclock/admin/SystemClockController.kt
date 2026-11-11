package com.example.ntpclock.admin

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings

sealed interface PolicyActionResult {
    data class Success(val detail: String) : PolicyActionResult
    data class Denied(val reason: String) : PolicyActionResult
    data class Unsupported(val reason: String) : PolicyActionResult
    data class Failed(val reason: String) : PolicyActionResult
}

sealed interface SetTimeOutcome {
    data object Success : SetTimeOutcome
    data class Denied(val reason: String) : SetTimeOutcome
    data class Failed(val reason: String) : SetTimeOutcome
}

interface SystemClockController {
    fun isAutoTimeEnabled(): Boolean
    fun isAutoTimeZoneEnabled(): Boolean
    fun disableAutoTime(): PolicyActionResult
    fun setTime(epochMillis: Long): SetTimeOutcome
    fun setTimeZone(timeZoneId: String): PolicyActionResult
}

/**
 * Wraps DevicePolicyManager time/timezone APIs with pre-flight Device Owner and AUTO_TIME checks.
 */
class AndroidSystemClockController(
    context: Context,
    private val deviceOwnerManager: DeviceOwnerManager,
) : SystemClockController {

    private val appContext = context.applicationContext
    private val devicePolicyManager =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override fun isAutoTimeEnabled(): Boolean {
        val globalAutoTime =
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AUTO_TIME, 1) != 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && deviceOwnerManager.isDeviceOwner()) {
            return try {
                devicePolicyManager.getAutoTimeEnabled(deviceOwnerManager.adminComponent) || globalAutoTime
            } catch (_: SecurityException) {
                globalAutoTime
            }
        }
        return globalAutoTime
    }

    override fun isAutoTimeZoneEnabled(): Boolean {
        val globalAutoTz =
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) != 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && deviceOwnerManager.isDeviceOwner()) {
            return try {
                devicePolicyManager.getAutoTimeZoneEnabled(deviceOwnerManager.adminComponent) || globalAutoTz
            } catch (_: SecurityException) {
                globalAutoTz
            }
        }
        return globalAutoTz
    }

    override fun disableAutoTime(): PolicyActionResult {
        if (!deviceOwnerManager.isDeviceOwner()) {
            return PolicyActionResult.Denied("Ứng dụng chưa phải Device Owner")
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return if (!isAutoTimeEnabled()) {
                PolicyActionResult.Success("AUTO_TIME đang tắt sẵn")
            } else {
                PolicyActionResult.Unsupported(
                    "Android API ${Build.VERSION.SDK_INT} chưa hỗ trợ DPM.setAutoTimeEnabled; hãy tắt thủ công trong Settings -> Date & time"
                )
            }
        }
        return try {
            devicePolicyManager.setAutoTimeEnabled(deviceOwnerManager.adminComponent, false)
            if (!isAutoTimeEnabled()) {
                PolicyActionResult.Success("Đã tắt Automatic Date & Time (AUTO_TIME = 0)")
            } else {
                PolicyActionResult.Failed("Đã gọi setAutoTimeEnabled(false) nhưng AUTO_TIME vẫn bật")
            }
        } catch (e: SecurityException) {
            PolicyActionResult.Denied("SecurityException khi tắt AUTO_TIME: ${e.message}")
        } catch (e: Exception) {
            PolicyActionResult.Failed("Lỗi khi tắt AUTO_TIME: ${e.message}")
        }
    }

    override fun setTime(epochMillis: Long): SetTimeOutcome {
        if (!deviceOwnerManager.isDeviceOwner()) {
            return SetTimeOutcome.Denied("Ứng dụng chưa được cấp quyền Device Owner")
        }
        if (isAutoTimeEnabled()) {
            return SetTimeOutcome.Denied("Automatic Date & Time đang bật (AUTO_TIME != 0)")
        }
        return try {
            val updated = devicePolicyManager.setTime(deviceOwnerManager.adminComponent, epochMillis)
            if (updated) {
                SetTimeOutcome.Success
            } else {
                SetTimeOutcome.Failed("DevicePolicyManager.setTime() trả về false")
            }
        } catch (e: SecurityException) {
            SetTimeOutcome.Denied("SecurityException khi gọi setTime(): ${e.message}")
        } catch (e: Exception) {
            SetTimeOutcome.Failed("Exception khi gọi setTime(): ${e.message}")
        }
    }

    override fun setTimeZone(timeZoneId: String): PolicyActionResult {
        if (!deviceOwnerManager.isDeviceOwner()) {
            return PolicyActionResult.Denied("Ứng dụng chưa phải Device Owner")
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isAutoTimeZoneEnabled()) {
                devicePolicyManager.setAutoTimeZoneEnabled(deviceOwnerManager.adminComponent, false)
            }
            val updated = devicePolicyManager.setTimeZone(deviceOwnerManager.adminComponent, timeZoneId)
            if (updated) {
                PolicyActionResult.Success("Đã đặt timezone thành $timeZoneId")
            } else {
                PolicyActionResult.Failed("DevicePolicyManager.setTimeZone() trả về false (kiểm tra Auto Time Zone)")
            }
        } catch (e: SecurityException) {
            PolicyActionResult.Denied("SecurityException khi đặt timezone: ${e.message}")
        } catch (e: Exception) {
            PolicyActionResult.Failed("Lỗi khi đặt timezone: ${e.message}")
        }
    }
}

