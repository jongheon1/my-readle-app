# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.jongheon.myreadle.**$$serializer { *; }
-keepclassmembers class com.jongheon.myreadle.** {
    *** Companion;
}
-keepclasseswithmembers class com.jongheon.myreadle.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn io.netty.**
-dontwarn org.slf4j.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
