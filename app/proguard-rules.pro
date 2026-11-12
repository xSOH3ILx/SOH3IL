# Proguard rules for SOH3IL
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room database entities and DAOs
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Keep Manifest-declared BroadcastReceivers
-keep class com.soh3il.core.scheduler.AlarmReceiver { *; }
-keep class com.soh3il.core.scheduler.MidnightReceiver { *; }
-keep class com.soh3il.core.scheduler.SystemEventReceiver { *; }

# Keep Updater, FileProvider and Domain Models
-keep class com.soh3il.core.updater.** { *; }
-keep class com.soh3il.core.model.** { *; }
-keep class androidx.core.content.FileProvider { *; }

-dontwarn okhttp3.**
-dontwarn okio.**

