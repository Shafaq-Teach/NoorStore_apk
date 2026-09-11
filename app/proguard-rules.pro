# Noor Store ProGuard & R8 Optimization Rules

# 1. Kotlin & Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Android Compose & ViewModel
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.ViewModel { *; }

# 3. Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 4. Retrofit & OkHttp & Moshi
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers enum * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}
-keep class com.squareup.moshi.** { *; }
-keep class com.example.data.** { *; }

# 5. Coil Image Loading
-keep class io.coil-kt.** { *; }
-dontwarn io.coil-kt.**
