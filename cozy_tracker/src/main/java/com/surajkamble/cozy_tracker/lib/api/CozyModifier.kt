package com.surajkamble.cozy_tracker.lib.api

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.surajkamble.cozy_tracker.lib.internal.Cozy
import com.surajkamble.cozy_tracker.lib.internal.TrackVisibility
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
/**
 * Attaches Cozy's visibility tracking to a lazy list.
 *
 * This is the main entrypoint for consumers and is designed to be the
 * single, obvious way to integrate the library.
 *
 * Configuration precedence:
 * - A [CozyConfig] can be provided globally via [ProvideCozyConfig].
 * - The [configure] lambda is applied on top of the current global config.
 *   This means local overrides always win over global defaults.
 *
 * Key semantics:
 * - By default, the tracker uses `LazyListItemInfo.key` as the identity for items.
 *   Callers MUST provide stable keys to the lazy list (`key = { id }`) to get
 *   meaningful analytics.
 * - When [keyExtractor] is provided, the resulting value will be surfaced as
 *   [VisibilityEvent.key] in callbacks, while internal tracking still uses
 *   the underlying Compose item key for correctness.
 */
fun Modifier.cozyTrack(
    listState: LazyListState,
    onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)? = null,
    onImpression: ((VisibilityEvent.Impression) -> Unit)? = null,
    keyExtractor: ((LazyListItemInfo) -> Any)? = null,
    configure: CozyConfig.() -> Unit = {},
): Modifier = composed {

    // Start from the globally provided configuration (if any) and apply
    // local overrides. This makes precedence explicit and deterministic.
    val globalConfig = LocalCozyConfig.current
    val finalConfig = remember(globalConfig, configure) {
        globalConfig.copy().apply(configure)
    }

    val tracker = remember(finalConfig, onDwellTime, onImpression, keyExtractor) {
        Cozy.Builder(
            config = finalConfig,
            keyExtractor = keyExtractor,
            onDwellTime = onDwellTime,
            onImpression = onImpression
        ).build()
    }
    TrackVisibility(listState = listState, tracker = tracker)
    this
}

/**
 * Configuration for Cozy's visibility tracking engine.
 *
 * The properties interact as follows:
 * - [minimumVisiblePercent]: an item must meet this visibility threshold to
 *   start or continue a visibility session. Dropping below ends the session.
 * - [minDwellTimeMs]: a session shorter than this duration is ignored and no
 *   dwell event is emitted.
 * - [debounceIntervalMs]: controls how often the lazy list layout is sampled.
 *   Larger values mean fewer tracking updates and lower overhead.
 * - [trackingMode]: controls which events are emitted:
 *   - [TrackingMode.DWELL_TIME]: dwell events are emitted when items leave the viewport.
 *   - [TrackingMode.FIRST_IMPRESSION]: an additional impression event is emitted
 *     the first time an item becomes visible. Dwell events are still emitted
 *     when sessions end.
 * - [trackStaleContent]: if `false`, only the first event for a given key is
 *   emitted; subsequent exposures are dropped. If `true`, subsequent events
 *   are emitted with [VisibilityEvent.isStaleContent] set to `true`.
 */
data class CozyConfig(
    var minimumVisiblePercent: Float = 0.5f,
    var debounceIntervalMs: Long = 250L,
    var minDwellTimeMs: Long = 0L,
    var trackStaleContent: Boolean = true,
    var trackingMode: TrackingMode = TrackingMode.DWELL_TIME
) {
    init {
        require(minimumVisiblePercent in 0.0f..1.0f) {
            "minimumVisiblePercent must be between 0.0 and 1.0"
        }
        require(minDwellTimeMs >= 0L) {
            "minDwellTimeMs cannot be negative"
        }
        require(debounceIntervalMs >= 0L) {
            "debounceIntervalMs cannot be negative"
        }
    }
}

internal val LocalCozyConfig = staticCompositionLocalOf {
    CozyConfig()
}

/**
 * Internal implementation for providing a [CozyConfig] to the Composition tree.
 */
@Composable
internal fun ProvideCozyConfig(
    config: CozyConfig = CozyConfig(),
    content: @Composable () -> Unit
) {
    val rememberedConfig = remember(config) { config }
    CompositionLocalProvider(LocalCozyConfig provides rememberedConfig, content = content)
}

/**
 * Defines the core behavior of the visibility tracker.
 */
enum class TrackingMode {
    /**
     * Accumulates the total time an item is visible across multiple sessions.
     * An event is fired every time an item leaves the viewport.
     */
    DWELL_TIME,

    /**
     * Fires an event only for the very first time an item becomes visible.
     * Once the event is fired, the item is no longer tracked.
     */
    FIRST_IMPRESSION
}
