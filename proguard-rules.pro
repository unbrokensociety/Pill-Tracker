# Pill Tracker — R8 keep-rules for the release build (minifyEnabled = true).
#
# The stack is plain Compose + Room + DataStore + coroutines: everything is
# compile-time resolved and ships its own consumer rules inside the AARs,
# so only the app's own reflection-adjacent spots need explicit keeps.

# Readable stack traces in any future crash report — worth the few KB.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Enum values are persisted BY NAME (GlassModeStore stores
# LiquidGlassQuality.name and reads it back via valueOf).
-keepclassmembers enum com.aistudio.meditracker.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room entities/DAOs are consumed by KSP-generated code; keep the data
# layer's shape so schema + converters can never drift.
-keep class com.aistudio.meditracker.data.** { *; }

# Compose compiler metadata and the entry points are handled by AGP itself.
# Nothing else in this app touches reflection at runtime.

# Libs that are optional at runtime — silence benign warnings.
-dontwarn org.jetbrains.annotations.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
