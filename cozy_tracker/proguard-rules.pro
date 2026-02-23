# Keep the public Composable function that the app needs to call.
-keep class com.surajkamble.cozy_tracker.lib.internal.CozyListStateKt {
    public static final androidx.compose.foundation.lazy.LazyListState rememberCozyListState(kotlin.jvm.functions.Function1, com.surajkamble.cozy_tracker.lib.config.VisibilityConfig, androidx.compose.runtime.Composer, int);
}

# Keep the public data classes and their properties.
# The -keepclassmembers rule ensures that the properties can be accessed.
-keep class com.surajkamble.cozy_tracker.lib.config.VisibilityConfig { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.VisibilityEvent { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.ContentMetadata { *; }
-keep class com.surajkamble.cozy_tracker.lib.model.ScrollDirection { *; }

# For data classes, we must also keep the component functions (e.g., component1(), component2())
# which are used for destructuring declarations.
-keepclassmembers class com.surajkamble.cozy_tracker.lib.config.VisibilityConfig {
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
