# Add project specific ProGuard rules here.

# Keep WebView JavaScript interface (nếu sau này bổ sung JavascriptInterface)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep MainActivity / WavyHeaderView
-keep class com.quangcaoktd.app.MainActivity { *; }
-keep class com.quangcaoktd.app.WavyHeaderView { *; }

# Androidx
-keep class androidx.** { *; }
-dontwarn androidx.**
