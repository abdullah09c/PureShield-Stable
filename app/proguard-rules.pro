# PureShield ProGuard Rules

# Keep accessibility service
-keep class com.abdullah09c.pureshield.service.BlockerService { *; }

# Keep receivers
-keep class com.abdullah09c.pureshield.receiver.** { *; }

# Keep all activities
-keep class com.abdullah09c.pureshield.ui.** { *; }

# Keep data classes
-keep class com.abdullah09c.pureshield.util.** { *; }

# Standard Android rules
-keepattributes *Annotation*
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
