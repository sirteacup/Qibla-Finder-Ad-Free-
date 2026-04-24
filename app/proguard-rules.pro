# Preserve stack trace line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Lifecycle (ViewModel, LiveData, etc.)
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Android Location & Sensors (used by Qibla calculation)
-keep class android.location.** { *; }
-keep class android.hardware.** { *; }

# Keep app's own classes (safe baseline — R8 will still optimize internals)
-keep class com.example.qiblafinder.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
