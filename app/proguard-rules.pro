# Proguard rules for SOH3IL
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
