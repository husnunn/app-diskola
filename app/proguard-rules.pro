# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Room (keep generated implementations and annotated elements) ---
# Keep Room-generated database implementations and their inner classes
-keep class **_Impl { *; }
-keep class id.app.education.database.LocalDatabase_Impl { *; }

# Keep classes extending RoomDatabase (prevents stripping abstract DB)
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep entities, DAOs, and Room annotations members
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Silence Room warnings (optional)
-dontwarn androidx.room.**
 
