# ProGuard rules for DRPSPHCA project.

# --- General ---
# Keep attributes for debugging and for reflection-based libraries.
-keepattributes Signature,InnerClasses,*Annotation*,SourceFile,LineNumberTable

# --- Kotlin ---
# Preserve Kotlin metadata, which is crucial for reflection on Kotlin classes.
-keep,allowobfuscation,allowshrinking class kotlin.Metadata { *; }

# --- Gson ---
# This is the standard, recommended configuration for Gson.
# It prevents ProGuard from stripping information needed for serialization/deserialization.
# The most important rules are for TypeToken, which is how Gson handles generics (e.g., List<Post>).
# This is the key fix for the "ParameterizedType" crash.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Keep all of your data model classes in the 'data' package.
-keep class com.drpsphca.app.data.** { *; }

# --- Retrofit & OkHttp ---
# Keep networking library classes.
-dontwarn retrofit2.BuiltInConverters
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# --- Coroutines ---
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    public static *;
}
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler

# --- Coil ---
-keep class coil.** { *; }
