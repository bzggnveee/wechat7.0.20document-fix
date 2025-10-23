-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn java.beans.Introspector
-dontwarn java.beans.VetoableChangeListener
-dontwarn java.beans.VetoableChangeSupport

# Keep ini4j Service Provider Interface
-keep,allowobfuscation,allowoptimization public class org.ini4j.spi.*

# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {  
    public static void checkNotNull(...);  
    public static void checkParameterIsNotNull(...);  
    public static void checkNotNullParameter(...);  
    public static void checkExpressionValueIsNotNull(...);  
    public static void checkNotNullExpressionValue(...);  
    public static void checkReturnedValueIsNotNull(...);  
    public static void throwUninitializedPropertyAccessException(...);  
}

-assumenosideeffects class android.util.Log {  
    public static *** d(...);  
    public static *** v(...);  
    public static *** i(...);  
}

-dontobfuscate
-allowaccessmodification
-renamesourcefileattribute ""  
-keepattributes !SourceFile,!LineNumberTable,Exceptions,Signature,InnerClasses,EnclosingMethod 
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-renamesourcefileattribute SourceFile

-keep class n3e.a7020.MainHook { *; }
-keep class n3e.a7020.R { *; }