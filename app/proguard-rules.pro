# Keep Flexmark classes and AST nodes from being stripped or obfuscated
-keep class com.vladsch.flexmark.** { *; }
-dontwarn com.vladsch.flexmark.**

# Keep FileProvider and components referenced in AndroidManifest
-keep class androidx.core.content.FileProvider { *; }
-keep class it.ilpelle.mdtools.** { *; }
-keep class android.print.** { *; }
