# Keep TFLite
-keep class org.tensorflow.lite.** { *; }

# Keep OpenCV
-keep class org.opencv.** { *; }
-keep class org.opencv.core.** { *; }
-keep class org.opencv.imgproc.** { *; }

# Keep Moshi
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# Kotlin metadata
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
