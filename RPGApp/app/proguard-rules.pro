# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Exclude Google Play Services components that cause authentication errors
-dontwarn com.google.android.gms.**
-dontnote com.google.android.gms.**
-keep class !com.google.android.gms.** { *; }

# Keep Firebase classes but exclude Google Play Services auth
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.auth.api.**
-dontwarn com.google.android.gms.auth.**
-dontwarn com.google.android.gms.common.**

# Completely ignore Google Play Services authentication
-assumenosideeffects class com.google.android.gms.** {
    *;
}
