# WEBINO Release ProGuard / R8 Rules
-keep class org.eclipse.jdt.** { *; }
-dontwarn org.eclipse.jdt.**
-keep class com.android.dx.** { *; }
-dontwarn com.android.dx.**
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.example.ui.navigation.** { *; }
-keep class com.example.data.repository.ProjectHistoryEntry { *; }

# Fix for missing javax.lang.model classes on Android (ECJ Compiler dependency)
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn javax.annotation.**