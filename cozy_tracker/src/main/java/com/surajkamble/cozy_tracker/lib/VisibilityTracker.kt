package com.surajkamble.cozy_tracker.lib

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * A class responsible for the core logic of tracking item visibility and calculating dwell time.
 * This class is designed to be run off the main thread to avoid any performance impact on the UI.
 *
 * It maintains a record of items currently considered "visible" and tracks the start time of their
 * visibility sessions. When an item leaves the viewport or the visibility criteria are no longer met,
 * the session is ended, and the duration is accumulated.
 *
 * @param onItemViewed A callback lambda that is invoked when an item's visibility session ends,
 * providing the item's key and the total accumulated duration in milliseconds.
 */
class VisibilityTracker(
    private val onItemViewed: (key: Any, durationMs: Long) -> Unit
) {
    // A thread-safe map to store the start time (in milliseconds) of an active visibility session for each item.
    // Key: The unique key of the LazyColumn/LazyRow item.
    // Value: The System.currentTimeMillis() when the item became visible.
    private val activeSessionStartTimes = ConcurrentHashMap<Any, Long>()

    // A thread-safe map to store the total accumulated visibility duration for each item across multiple sessions.
    // Key: The unique key of the LazyColumn/LazyRow item.
    // Value: The total accumulated time in milliseconds.
    private val accumulatedDurations = ConcurrentHashMap<Any, Long>()

    private val trackerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null

    /**
     * Processes the current list of visible items from the LazyList's layout info.
     * This is the entry point for the visibility calculation logic. It determines which items
     * have entered or exited the viewport based on the provided configuration.
     *
     * This function is designed to be called from a background coroutine.
     *
     * @param layoutInfo The [LazyListLayoutInfo] from the list state.
     * @param config The [VisibilityConfig] to use for determining visibility.
     */
    fun processVisibility(
        layoutInfo: LazyListLayoutInfo,
        config: VisibilityConfig
    ) {
        processingJob?.cancel() // Cancel any ongoing processing
        processingJob = trackerScope.launch {
            val visibleKeys = mutableSetOf<Any>()
            val now = System.currentTimeMillis()

            // First, identify all items that are currently visible according to the config.
            for (item in layoutInfo.visibleItemsInfo) {
                if (item.isVisible(layoutInfo, config)) {
                    visibleKeys.add(item.key)
                }
            }

            // End sessions for items that are no longer visible.
            val inactiveKeys = activeSessionStartTimes.keys.filter { it !in visibleKeys }
            for (key in inactiveKeys) {
                endSession(key, now)
            }

            // Start sessions for new items that have just become visible.
            for (key in visibleKeys) {
                if (!activeSessionStartTimes.containsKey(key)) {
                    startSession(key, now)
                }
            }
        }
    }

    /**
     * Forces the end of all currently active visibility sessions. This is useful for scenarios
     * like the app going into the background, where we want to record the dwell time up to that point.
     */
    fun flush() {
        processingJob?.cancel() // Cancel any ongoing processing
        trackerScope.launch {
            val now = System.currentTimeMillis()
            val activeKeys = activeSessionStartTimes.keys.toList() // Create a copy to avoid ConcurrentModificationException
            activeKeys.forEach { key ->
                endSession(key, now)
            }
        }
    }

    private fun startSession(key: Any, time: Long) {
        activeSessionStartTimes[key] = time
    }

    private fun endSession(key: Any, time: Long) {
        val startTime = activeSessionStartTimes.remove(key) ?: return
        val duration = time - startTime
        if (duration > 0) {
            // Replaced getOrDefault with a null-safe call for minSdk < 24 compatibility.
            val newTotal = (accumulatedDurations[key] ?: 0L) + duration
            accumulatedDurations[key] = newTotal
            onItemViewed(key, newTotal)
        }
    }

    /**
     * Determines if a [LazyListItemInfo] is "visible" based on the [VisibilityConfig].
     *
     * @param layoutInfo The [LazyListLayoutInfo] used to get viewport and orientation details.
     * @param config The configuration to use for the visibility check.
     * @return `true` if the item meets the minimum visibility percentage, `false` otherwise.
     */
    private fun LazyListItemInfo.isVisible(layoutInfo: LazyListLayoutInfo, config: VisibilityConfig): Boolean {
        val viewportSize = layoutInfo.viewportSize
        val itemSize = this.size

        // The 'when' statement is now exhaustive and doesn't need an 'else' branch.
        val visibleFraction = when (layoutInfo.orientation) {
            Orientation.Vertical -> {
                val top = this.offset.toFloat()
                val bottom = top + itemSize
                val viewportEnd = viewportSize.height.toFloat()
                val visiblePart = max(0f, min(bottom, viewportEnd) - max(top, 0f))
                if (itemSize > 0) visiblePart / itemSize else 0f
            }
            Orientation.Horizontal -> {
                val left = this.offset.toFloat()
                val right = left + itemSize
                val viewportEnd = viewportSize.width.toFloat()
                val visiblePart = max(0f, min(right, viewportEnd) - max(left, 0f))
                if (itemSize > 0) visiblePart / itemSize else 0f
            }
        }

        return visibleFraction >= config.minimumVisiblePercent
    }
}
