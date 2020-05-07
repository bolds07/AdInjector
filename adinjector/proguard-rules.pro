 #================CRASHLYTICS=====================
 -keepattributes *Annotation*
 -keepattributes SourceFile,LineNumberTable
 -keep public class * extends java.lang.Exception
 #-keep class com.FirebaseCrashlytics.** { *; }
 #-dontwarn com.FirebaseCrashlytics.**
 #================END=============================