# Add project specific ProGuard rules here.

# Keep Retrofit generated classes
-keepattributes Signature
-keepattributes *Annotation*
-keepparameternames

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn javax.annotation.**