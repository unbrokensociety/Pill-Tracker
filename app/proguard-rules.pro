-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepclassmembers enum com.aistudio.meditracker.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.aistudio.meditracker.data.** { *; }

-dontwarn org.jetbrains.annotations.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
