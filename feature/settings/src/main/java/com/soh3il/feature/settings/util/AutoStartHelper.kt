package com.soh3il.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

data class ManufacturerGuide(
    val brandName: String,
    val instructions: List<String>
)

object AutoStartHelper {

    fun getDeviceGuide(): ManufacturerGuide {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
        val brand = (Build.BRAND ?: "").lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                ManufacturerGuide(
                    brandName = "شیائومی (Xiaomi / MIUI / HyperOS)",
                    instructions = listOf(
                        "در صفحه‌ای که باز می‌شود، کلید شروع خودکار (Autostart) را برای برنامه SOH3IL روشن کنید.",
                        "در بخش صرفه‌جویی باتری (Battery Saver)، حالت را روی «بدون محدودیت (No restrictions)» قرار دهید تا یادآورها بدون کوچک‌ترین تاخیر زنگ بزنند.",
                        "در صورت امکان، آیکون برنامه را در صفحه برنامه‌های اخیر (Recent Apps) قفل کنید."
                    )
                )
            }
            manufacturer.contains("samsung") || brand.contains("samsung") -> {
                ManufacturerGuide(
                    brandName = "سامسونگ (Samsung / One UI)",
                    instructions = listOf(
                        "در بخش مراقبت دستگاه (Device care) وارد «باتری» شوید.",
                        "در بخش «محدودیت‌های استفاده در پس‌زمینه (Background usage limits)»، برنامه SOH3IL را به «برنامه‌های هرگز در حالت خواب (Never sleeping apps)» اضافه نمایید.",
                        "در بخش بهینه‌سازی مصرف باتری برنامه، حالت را روی «نامحدود (Unrestricted)» قرار دهید."
                    )
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") ||
            brand.contains("huawei") || brand.contains("honor") -> {
                ManufacturerGuide(
                    brandName = "هواوی و آنر (Huawei / Honor / EMUI)",
                    instructions = listOf(
                        "در بخش راه‌اندازی برنامه (App Launch)، کلید خودکار را برای SOH3IL خاموش و به «دستی (Manage manually)» تغییر دهید.",
                        "هر ۳ گزینه «راه‌اندازی خودکار (Auto-launch)»، «راه‌اندازی ثانویه (Secondary launch)» و «اجرا در پس‌زمینه (Run in background)» را فعال کنید."
                    )
                )
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
            brand.contains("oppo") || brand.contains("realme") -> {
                ManufacturerGuide(
                    brandName = "اوپو و ریلمی (Oppo / Realme / ColorOS)",
                    instructions = listOf(
                        "در بخش مدیریت برنامه (App Management)، وارد «شروع خودکار (Auto-launch)» شوید.",
                        "کلید شروع خودکار را برای SOH3IL فعال کنید و مجوز فعالیت در پس‌زمینه را روشن بگذارید."
                    )
                )
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ||
            brand.contains("vivo") || brand.contains("iqoo") -> {
                ManufacturerGuide(
                    brandName = "ویوو (Vivo / iQOO / Funtouch OS)",
                    instructions = listOf(
                        "در برنامه iManager یا بخش مدیریت دسترسی‌ها، وارد «Autostart» شوید.",
                        "کلید برنامه SOH3IL را روشن کنید و در مصرف بالای پس‌زمینه اجازه فعالیت بدهید."
                    )
                )
            }
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> {
                ManufacturerGuide(
                    brandName = "وان‌پلاس (OnePlus / OxygenOS)",
                    instructions = listOf(
                        "در بخش باتری، بهینه‌سازی باتری (Battery Optimization) را برای SOH3IL روی Don't optimize قرار دهید.",
                        "در بخش Auto-launch برنامه را مجاز کنید."
                    )
                )
            }
            else -> {
                ManufacturerGuide(
                    brandName = "اندروید استاندارد (${Build.MANUFACTURER.capitalizeFirst()})",
                    instructions = listOf(
                        "در صفحه تنظیمات برنامه، وارد بخش باتری (Battery) شوید.",
                        "حالت استفاده از باتری را روی «بدون محدودیت (Unrestricted)» تنظیم کنید تا سیستم‌عامل در زمان خواب گوشی آلارم‌ها را به تعویق نیندازد."
                    )
                )
            }
        }
    }

    /**
     * Attempts to launch the specific OEM autostart / background management activity.
     * Returns true if a specific or fallback screen was launched.
     */
    fun openAutoStartScreen(context: Context): Boolean {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
        val brand = (Build.BRAND ?: "").lowercase()

        val intents = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intents.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")).putExtra("extra_pkgname", context.packageName))
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")).putExtra("extra_pkgname", context.packageName))
            }
            manufacturer.contains("samsung") || brand.contains("samsung") -> {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")))
                intents.add(Intent("com.samsung.android.sm.ACTION_BATTERY"))
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") ||
            brand.contains("huawei") || brand.contains("honor") -> {
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.mainscreen.MainScreenActivity")))
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") ||
            brand.contains("oppo") || brand.contains("realme") -> {
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity")))
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ||
            brand.contains("vivo") || brand.contains("iqoo") -> {
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.safeguard.PurviewTabActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")))
            }
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> {
                intents.add(Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListAct")))
            }
            manufacturer.contains("asus") || brand.contains("asus") -> {
                intents.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")))
                intents.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings")))
            }
        }

        // Standard App Details fallback
        intents.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Try next candidate
            }
        }

        return false
    }

    private fun String?.capitalizeFirst(): String {
        if (this.isNullOrEmpty()) return "دستگاه شما"
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
