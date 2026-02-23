package com.surajkamble.cozy_tracker.lib.util

import android.util.Log
import com.surajkamble.cozy_tracker.lib.BuildConfig
import com.surajkamble.cozy_tracker.lib.config.CozyConfig
import com.surajkamble.cozy_tracker.lib.config.TrackingMode
import com.surajkamble.cozy_tracker.lib.model.ContentMetadata
import com.surajkamble.cozy_tracker.lib.model.ScrollDirection
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent

/**
 * A utility responsible for creating and dispatching visibility events in the desired format.
 */
internal object EventDispatcher {

    private const val INTERNAL_LOG_TAG = "CozyTrackerInternal"

    /**
     * Dispatches a [VisibilityEvent.DwellTime] event when an item's session ends.
     */
    fun dispatchDwellTimeEvent(
        key: Any,
        time: Long,
        scrollDirection: ScrollDirection,
        state: TrackerState,
        config: CozyConfig,
        onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)?
    ) {
        val sessionData = state.endSession(key) ?: return
        val visibleDurationMs = time - sessionData.startTimeMs

        if (visibleDurationMs > 0 && visibleDurationMs >= config.minDwellTimeMs) {
            val totalSeenTimeMs = state.updateTotalSeenTime(key, visibleDurationMs)
            val wasAlreadyDispatched = state.isStaleAndMark(key)
            val firstSeenTime = state.getFirstSeenTime(key) ?: time

            val event = VisibilityEvent.DwellTime(
                key = key,
                isStaleContent = wasAlreadyDispatched,
                visibleDurationMs = visibleDurationMs,
                firstSeenAtMs = firstSeenTime,
                lastSeenAtMs = time,
                totalSeenTimeMs = totalSeenTimeMs,
                contentMetadata = ContentMetadata(
                    contentPosition = sessionData.itemInfo.index,
                    scrollDirection = scrollDirection,
                    contentType = sessionData.itemInfo.contentType
                )
            )

            if (BuildConfig.DEBUG) {
                Log.d(INTERNAL_LOG_TAG, "Dispatching DwellTime Event: $event")
            }
            onDwellTime?.invoke(event)
        }
    }

    /**
     * Dispatches a [VisibilityEvent.Impression] event, typically for [TrackingMode.FIRST_IMPRESSION].
     */
    fun dispatchImpressionEvent(
        itemKey: Any,
        time: Long,
        scrollDirection: ScrollDirection,
        state: TrackerState,
        config: CozyConfig,
        onImpression: ((VisibilityEvent.Impression) -> Unit)?
    ) {
        val sessionData = state.activeSessions[itemKey] ?: return // Get session data, but don't remove yet if still active
        val wasAlreadyDispatched = state.isStaleAndMark(itemKey)
        val firstSeenTime = state.getFirstSeenTime(itemKey) ?: time

        val event = VisibilityEvent.Impression(
            key = itemKey,
            isStaleContent = wasAlreadyDispatched,
            firstSeenAtMs = firstSeenTime,
            lastSeenAtMs = time,
            contentMetadata = ContentMetadata(
                contentPosition = sessionData.itemInfo.index,
                scrollDirection = scrollDirection,
                contentType = sessionData.itemInfo.contentType
            )
        )

        if (BuildConfig.DEBUG) {
            Log.d(INTERNAL_LOG_TAG, "Dispatching Impression Event: $event")
        }
        onImpression?.invoke(event)
    }

    /**
     * Converts a VisibilityEvent data class into a simple Map for analytics.
     * This is no longer used internally but kept for reference if needed elsewhere.
     */
    private fun VisibilityEvent.toMap(): Map<String, Any?> {
        val baseMap = mapOf(
            "key" to key,
            "is_stale_content" to isStaleContent,
            "first_seen_at_ms" to firstSeenAtMs,
            "last_seen_at_ms" to lastSeenAtMs,
            "content_position" to contentMetadata.contentPosition,
            "content_type" to contentMetadata.contentType,
            "scroll_direction" to contentMetadata.scrollDirection.name
        )

        return when (this) {
            is VisibilityEvent.DwellTime -> baseMap + mapOf(
                "event_type" to "dwell_time",
                "visible_duration_ms" to visibleDurationMs,
                "total_seen_time_ms" to totalSeenTimeMs
            )
            is VisibilityEvent.Impression -> baseMap + mapOf(
                "event_type" to "impression"
            )
            is VisibilityEvent.AppBackgrounded -> mapOf(
                "event_type" to "app_backgrounded",
                "timestamp_ms" to timestampMs,
                "flushed_items_count" to flushedItems.size
            )
        }
    }
}
