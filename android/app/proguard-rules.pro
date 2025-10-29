# Keep ScreenCaptureService methods
-keep class com.example.umazing_helper.ScreenCaptureService { *; }
-keep class com.example.umazing_helper.CaptureResult { *; }

# Keep image processing related classes
-keep class android.graphics.** { *; }
-keep class android.media.** { *; }

# Keep ML Kit Text Recognition (Latin/English only)
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.** { *; }

# Suppress warnings for unused language models
-dontwarn com.google.mlkit.vision.text.japanese.**
-dontwarn com.google.mlkit.vision.text.chinese.**
-dontwarn com.google.mlkit.vision.text.korean.**
-dontwarn com.google.mlkit.vision.text.devanagari.**

# Keep Flutter wrapper classes
-keep class io.flutter.** { *; }
-keep class io.flutter.embedding.** { *; }
-keep class io.flutter.plugin.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile