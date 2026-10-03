# Sovereign Ledger release rules (R8).
# Libraries in use (Sentry, Ktor, SQLDelight, Compose) ship their own
# consumer rules; below is what they don't cover.

# --- Readable crash reports -------------------------------------------------
# Keep line numbers so GlitchTip stack traces are symbolicated without the
# mapping file (mapping.txt is still archived with each GitHub release for
# full deobfuscation of obfuscated names).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization --------------------------------------------------
# Official rules from the kotlinx.serialization README. The compiler plugin
# generates serializers it references directly (usually safe under R8), but
# @Serializable classes looked up via reflection (generic star-projections)
# need these keeps.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
