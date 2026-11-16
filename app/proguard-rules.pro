# Room, Hilt, WorkManager and kotlinx.serialization ship their own consumer rules.
# The seed importer decodes words.json with kotlinx.serialization, so keep its serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.vocabulary.app.data.seed.** {
    *** Companion;
}
-keepclasseswithmembers class com.vocabulary.app.data.seed.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Release hardening: drop debug logging and hide source file names in stack traces.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-repackageclasses ''
