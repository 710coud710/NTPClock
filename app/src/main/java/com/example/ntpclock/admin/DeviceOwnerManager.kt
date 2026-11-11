package com.example.ntpclock.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

data class AdminStatus(
    val isDeviceOwner: Boolean,
    val isAdminActive: Boolean,
    val packageName: String,
    val receiverComponentName: String,
)

interface DeviceOwnerManager {
    val adminComponent: ComponentName
    fun isDeviceOwner(): Boolean
    fun isAdminActive(): Boolean
    fun getStatus(): AdminStatus
}

class AndroidDeviceOwnerManager(context: Context) : DeviceOwnerManager {
    private val appContext = context.applicationContext
    private val devicePolicyManager =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    override val adminComponent: ComponentName =
        ComponentName(appContext, NtpDeviceAdminReceiver::class.java)

    override fun isDeviceOwner(): Boolean =
        devicePolicyManager.isDeviceOwnerApp(appContext.packageName)

    override fun isAdminActive(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent)

    override fun getStatus(): AdminStatus = AdminStatus(
        isDeviceOwner = isDeviceOwner(),
        isAdminActive = isAdminActive(),
        packageName = appContext.packageName,
        receiverComponentName = adminComponent.flattenToShortString(),
    )
}

