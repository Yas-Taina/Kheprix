# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson (data classes)
-keep class com.kheprix.models.** { *; }
-keepclassmembers class com.kheprix.models.** { *; }

# ZXing QR
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Google Play Services Location
-keep class com.google.android.gms.** { *; }
