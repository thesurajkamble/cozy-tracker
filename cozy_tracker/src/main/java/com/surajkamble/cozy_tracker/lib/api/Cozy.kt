package com.surajkamble.cozy_tracker.lib.api

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import com.surajkamble.cozy_tracker.lib.config.TrackingMode
import com.surajkamble.cozy_tracker.lib.config.VisibilityConfig
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import com.surajkamble.cozy_tracker.lib.util.EventDispatcher
import com.surajkamble.cozy_tracker.lib.util.ScrollDirectionDetector
import com.surajkamble.cozy_tracker.lib.util.TrackerState
import com.surajkamble.cozy_tracker.lib.util.VisibilityCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The internal engine for the CozyTracker library. This class is not public-facing.
 * It orchestrates the work between the state manager and various utility calculators.
 */
internal class Cozy private constructor(
    val config: VisibilityConfig,
    private val onEvent: (VisibilityEvent) -> Unit
) {
    // --- Utilities ---
    private val state = TrackerState()
    private val scrollDirectionDetector = ScrollDirectionDetector()

    // --- Coroutine Scope ---
    private val trackerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null

    internal class Builder(private val onEvent: (VisibilityEvent) -> Unit) {
        private var config = VisibilityConfig()

        fun config(config: VisibilityConfig) = apply { this.config = config }

        fun build(): Cozy {
            return Cozy(config, onEvent)
        }
    }

    fun processVisibility(layoutInfo: LazyListLayoutInfo) {
        processingJob?.cancel()
        processingJob = trackerScope.launch {
            val now = System.currentTimeMillis()
            val visibleItemsMap = layoutInfo.visibleItemsInfo.associateBy { it.key }
            val scrollDirection = scrollDirectionDetector.determineScrollDirection(layoutInfo)

            // End sessions for items that are no longer visible
            val endedSessionKeys = state.activeSessions.keys.filter { key ->
                val itemInfo = visibleItemsMap[key]
                itemInfo == null || !VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)
            }
            endedSessionKeys.forEach { key ->
                EventDispatcher.dispatchEndSessionEvent(key, now, scrollDirection, state, config, onEvent)
            }

            // Start sessions for new items, respecting the configuration
            visibleItemsMap.values.forEach { itemInfo ->
                val key = itemInfo.key
                val isAlreadyDone = state.dispatchedKeys.containsKey(key) &&
                        (!config.trackStaleContent || config.trackingMode == TrackingMode.FIRST_IMPRESSION)

                if (!isAlreadyDone && !state.activeSessions.containsKey(key) && VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)) {
                    state.startSession(itemInfo, now)

                    if (config.trackingMode == TrackingMode.FIRST_IMPRESSION) {
                        EventDispatcher.dispatchEndSessionEvent(key, now, scrollDirection, state, config, onEvent)
                    }
                }
            }
        }
    }

    fun flush() {
        processingJob?.cancel()
        trackerScope.launch {
            val now = System.currentTimeMillis()
            val activeKeys = state.endAllActiveSessions()
            activeKeys.forEach { key ->
                EventDispatcher.dispatchEndSessionEvent(key, now, scrollDirectionDetector.getLastScrollDirection(), state, config, onEvent)
            }
        }
    }
}
