package com.example.ntpclock.core.common

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object VendorOptimizationHelper {

    private const val TAG = "VendorOptimization"

    /**
     * Checks whether the app is exempt from Battery Optimization (Doze Mode).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens system dialog to request ignoring battery optimizations for this app.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open request ignore battery optimizations", e)
            openAppSettings(context)
        }
    }

    /**
     * Detects device manufacturer brand name formatted nicely for display.
     */
    fun getVendorBrandName(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Xiaomi / Redmi / POCO"
            manufacturer.contains("samsung") -> "Samsung"
            manufacturer.contains("lenovo") -> "Lenovo"
            manufacturer.contains("asus") -> "Asus"
            manufacturer.contains("oppo") || manufacturer.contains("realme") || model.contains("oppo") -> "OPPO / Realme"
            manufacturer.contains("vivo") -> "Vivo"
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> "Huawei / Honor"
            else -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Attempts to open vendor-specific Autostart / Background Management settings screen
     * for popular manufacturers (Samsung, Xiaomi/Redmi, Lenovo, Asus, Oppo, Vivo, Huawei).
     */
    fun openVendorAutostartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intents = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                intents.add(
                    Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                        )
                        putExtra("extra_pkgname", context.packageName)
                    }
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.powercenter.PowerSettings"
                        )
                    )
                )
            }

            manufacturer.contains("samsung") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.looper",
                            "com.samsung.android.looper.com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.sm_cn",
                            "com.samsung.android.sm.ui.ram.AutoRunActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
            }

            manufacturer.contains("lenovo") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.lenovo.powermgr",
                            "com.lenovo.powermgr.ui.AutoRunManagementActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.lenovo.security",
                            "com.lenovo.security.purebackground.PureBackgroundActivity"
                        )
                    )
                )
            }

            manufacturer.contains("asus") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.asus.mobilemanager",
                            "com.asus.mobilemanager.autostart.AutoStartActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.asus.mobilemanager",
                            "com.asus.mobilemanager.entry.FunctionActivity"
                        )
                    )
                )
            }

            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.oppo.safe",
                            "com.oppo.safe.permission.startup.StartupAppListActivity"
                        )
                    )
                )
            }

            manufacturer.contains("vivo") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                    )
                )
            }

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity"
                        )
                    )
                )
            }
        }

        // Try OEM-specific intents first
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Intent failed for component: ${intent.component}", e)
            }
        }

        // Fallback to standard app detail settings page
        return openAppSettings(context)
    }

    /**
     * Opens system App Info page for this package as a fallback.
     */
    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open App Settings", e)
            false
        }
    }

    /**
     * Opens system Date & Time settings page directly.
     */
    fun openSystemDateSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_DATE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open System Date Settings", e)
            false
        }
    }
}
