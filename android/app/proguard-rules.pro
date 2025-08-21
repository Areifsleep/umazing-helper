# Keep ScreenCaptureService methods
-keep class com.example.umazing_helper.ScreenCaptureService { *; }
-keep class com.example.umazing_helper.CaptureResult { *; }

# Keep image processing related classes
-keep class android.graphics.** { *; }
-keep class android.media.** { *; }