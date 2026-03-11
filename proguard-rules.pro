# DeafCall ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep models
-keep class com.deafcall.model.** { *; }

# Keep services (critical for Telecom)
-keep class com.deafcall.service.** { *; }

# Keep TTS/STT related
-keep class android.speech.** { *; }
-keep class android.telecom.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
