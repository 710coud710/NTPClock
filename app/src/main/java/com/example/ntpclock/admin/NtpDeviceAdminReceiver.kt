package com.example.ntpclock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Policy Controller receiver used for Device Owner time & timezone management.
 */
class NtpDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled for ${context.packageName}")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device admin disabled for ${context.packageName}")
    }

    companion object {
        private const val TAG = "NtpDeviceAdminReceiver"
    }
}

