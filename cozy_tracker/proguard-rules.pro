# Keep the public Composable function that the app needs to call.
-keep class com.surajkamble.cozy_tracker.lib.internal.CozyListStateKt {
    public static final androidx.compose.foundation.lazy.LazyListState rememberCozyListState(kotlin.jvm.functions.Function1, com.surajkamble.cozy_tracker.lib.config.VisibilityConfig, androidx.compose.runtime.Composer, int);
}

# Keep the public data classes and their properties.
-keep class com.surajkamble.cozy_tracker.lib.config.CozyConfig { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.VisibilityEvent { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.ContentMetadata { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.ScrollDirection { *; }

# For data classes, we must also keep the component functions (e.g., component1(), component2())
-keepclassmembers class com.surajkamble.cozy_tracker.lib.config.CozyConfig {
    public <methods>;
}
-keepclassmembers class com.surajkamble.cozy_tracker.lib.model.VisibilityEvent {
    public <methods>;
}
-keepclassmembers class com.surajkamble.cozy_tracker.lib.model.ContentMetadata {
    public <methods>;
}
-keepclassmembers class com.surajkamble.cozy_tracker.lib.model.ScrollDirection {
    public <methods>;
}

# Keep all public methods of enums that are part of the public API
-keepclassmembers enum com.surajkamble.cozy_tracker.lib.model.ScrollDirection {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.surajkamble.cozy_tracker.lib.config.TrackingMode {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- SCRIPT TO REMOVE LOGGING FROM RELEASE BUILDS ---
# This rule tells the R8 compiler that calls to the Log class have no side effects.
# This allows R8 to completely strip all Log.d, Log.v, etc., calls from the final release APK.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
