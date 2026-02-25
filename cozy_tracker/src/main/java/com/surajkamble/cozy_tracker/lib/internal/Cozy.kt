package com.surajkamble.cozy_tracker.lib.internal

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListItemInfo
import com.surajkamble.cozy_tracker.lib.api.CozyConfig
import com.surajkamble.cozy_tracker.lib.api.TrackingMode
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import com.surajkamble.cozy_tracker.lib.util.EventDispatcher
import com.surajkamble.cozy_tracker.lib.internal.ScrollDirectionDetector
import com.surajkamble.cozy_tracker.lib.util.TrackerState
import com.surajkamble.cozy_tracker.lib.internal.VisibilityCalculator


internal class Cozy private constructor(
    val config: CozyConfig,
    /**
     * Optional extractor that allows callers to control the logical key
     * surfaced in [VisibilityEvent.key]. When `null`, the underlying
     * `LazyListItemInfo.key` is used for analytics as well.
     */
    private val keyExtractor: ((LazyListItemInfo) -> Any)?,
    private val onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)?,
    private val onImpression: ((VisibilityEvent.Impression) -> Unit)?
) {
    private val state = TrackerState()
    private val scrollDirectionDetector = ScrollDirectionDetector()

    // Reusable buffers to avoid per-frame allocations while processing visibility.
    private val visibleItemsByKey = mutableMapOf<Any, LazyListItemInfo>()
    private val endedSessionKeysBuffer = mutableListOf<Any>()

    internal class Builder(
        private var config: CozyConfig = CozyConfig(),
        private val keyExtractor: ((LazyListItemInfo) -> Any)? = null,
        private val onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)? = null,
        private val onImpression: ((VisibilityEvent.Impression) -> Unit)? = null
    ) {
        fun config(config: CozyConfig) = apply { this.config = config }

        fun build(): Cozy {
            return Cozy(config, keyExtractor, onDwellTime, onImpression)
        }
    }

    fun processVisibility(layoutInfo: LazyListLayoutInfo) {
        val now = System.currentTimeMillis()

        // Build a map of currently visible items keyed by their Compose keys,
        // reusing the same backing map to reduce allocations.
        visibleItemsByKey.clear()
        layoutInfo.visibleItemsInfo.forEach { itemInfo ->
            visibleItemsByKey[itemInfo.key] = itemInfo
        }
        if (visibleItemsByKey.isEmpty()) return

        val scrollDirection = scrollDirectionDetector.determineScrollDirection(layoutInfo)

        // Determine which sessions have ended because their items are no longer
        // visible enough according to the current configuration.
        endedSessionKeysBuffer.clear()
        state.activeSessions.keys.forEach { key ->
            val itemInfo = visibleItemsByKey[key]
            if (itemInfo == null || !VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)) {
                endedSessionKeysBuffer.add(key)
            }
        }
        endedSessionKeysBuffer.forEach { key ->
            EventDispatcher.dispatchDwellTimeEvent(
                internalKey = key,
                time = now,
                scrollDirection = scrollDirection,
                state = state,
                config = config,
                onDwellTime = onDwellTime
            )
        }

        // Start new sessions for items that have just become visible enough.
        layoutInfo.visibleItemsInfo.forEach { itemInfo ->
            val internalKey = itemInfo.key
            if (state.activeSessions.containsKey(internalKey)) return@forEach

            if (!VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)) return@forEach

            // The analytics key is what will be exposed to consumers in events.
            val analyticsKey = keyExtractor?.invoke(itemInfo) ?: internalKey
            state.startSession(itemInfo, now, analyticsKey)

            if (config.trackingMode == TrackingMode.FIRST_IMPRESSION) {
                EventDispatcher.dispatchImpressionEvent(
                    internalKey = internalKey,
                    time = now,
                    scrollDirection = scrollDirection,
                    state = state,
                    config = config,
                    onImpression = onImpression
                )
            }
        }
    }

    fun flush() {
        val now = System.currentTimeMillis()
        val activeKeys = state.endAllActiveSessions()
        activeKeys.forEach { key ->
            EventDispatcher.dispatchDwellTimeEvent(
                internalKey = key,
                time = now,
                scrollDirection = scrollDirectionDetector.getLastScrollDirection(),
                state = state,
                config = config,
                onDwellTime = onDwellTime
            )
        }
    }
}