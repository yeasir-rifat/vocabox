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
