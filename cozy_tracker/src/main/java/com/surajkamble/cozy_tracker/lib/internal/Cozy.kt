package com.surajkamble.cozy_tracker.lib.internal

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import com.surajkamble.cozy_tracker.lib.config.CozyConfig
import com.surajkamble.cozy_tracker.lib.config.TrackingMode
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
    val config: CozyConfig,
    private val onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)?,
    private val onImpression: ((VisibilityEvent.Impression) -> Unit)?
) {
    private val state = TrackerState()
    private val scrollDirectionDetector = ScrollDirectionDetector()

    private val trackerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null

    internal class Builder(
        private val onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)? = null,
        private val onImpression: ((VisibilityEvent.Impression) -> Unit)? = null
    ) {
        private var config = CozyConfig()

        fun config(config: CozyConfig) = apply { this.config = config }

        fun build(): Cozy {
            return Cozy(config, onDwellTime, onImpression)
        }
    }

    fun processVisibility(layoutInfo: LazyListLayoutInfo) {
        processingJob?.cancel()
        processingJob = trackerScope.launch {
            val now = System.currentTimeMillis()
            val visibleItemsMap = layoutInfo.visibleItemsInfo.associateBy { it.key }
            val scrollDirection = scrollDirectionDetector.determineScrollDirection(layoutInfo)

            val endedSessionKeys = state.activeSessions.keys.filter { key ->
                val itemInfo = visibleItemsMap[key]
                itemInfo == null || !VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)
            }
            endedSessionKeys.forEach { key ->
                EventDispatcher.dispatchDwellTimeEvent(key, now, scrollDirection, state, config, onDwellTime)
            }

            visibleItemsMap.values.forEach { itemInfo ->
                val key = itemInfo.key
                val isAlreadyDone = state.dispatchedKeys.containsKey(key) &&
                        (!config.trackStaleContent || config.trackingMode == TrackingMode.FIRST_IMPRESSION)

                if (!isAlreadyDone && !state.activeSessions.containsKey(key) && VisibilityCalculator.isVisible(itemInfo, layoutInfo, config)) {
                    state.startSession(itemInfo, now)


                    if (config.trackingMode == TrackingMode.FIRST_IMPRESSION) {
                        EventDispatcher.dispatchImpressionEvent(itemInfo.key, now, scrollDirection, state, config, onImpression)
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
                EventDispatcher.dispatchDwellTimeEvent(key, now, scrollDirectionDetector.getLastScrollDirection(), state, config, onDwellTime)
            }
        }
    }
}