# MapLibre uses JNI; keep its classes.
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Release builds drop debug, info and warning log lines, so nothing about
# listeners, billing or file paths reaches logcat on riders' phones.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
