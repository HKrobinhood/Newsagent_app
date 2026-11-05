# GlobalNewsAI Proguard / R8 Optimization Rules

# Preserve Data Transfer Objects (DTOs) and Domain Models serialized via Gson
-keep class com.example.newsagnet.model.** { *; }
-keep class com.example.newsagnet.network.dto.** { *; }

# Gson configuration
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Retrofit 2 configuration
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp configuration
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines configuration
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Android Jetpack Compose rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
