package com.surajkamble.cozy_tracker.lib.util

import android.util.Log
//import com.surajkamble.cozy_tracker.lib.BuildConfig
import com.surajkamble.cozy_tracker.lib.api.CozyConfig
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
        internalKey: Any,
        time: Long,
        scrollDirection: ScrollDirection,
        state: TrackerState,
        config: CozyConfig,
        onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)?
    ) {
        val sessionData = state.endSession(internalKey) ?: return
        val visibleDurationMs = time - sessionData.startTimeMs

        if (visibleDurationMs > 0 && visibleDurationMs >= config.minDwellTimeMs) {
            val totalSeenTimeMs = state.updateTotalSeenTime(internalKey, visibleDurationMs)
            val wasAlreadyDispatched = state.checkIfStaleAndMark(internalKey)

            // When stale content tracking is disabled, we only emit the first
            // event for a given key and drop subsequent ones entirely.
            if (wasAlreadyDispatched && !config.trackStaleContent) {
                return
            }

            val firstSeenTime = state.getFirstSeenTime(internalKey) ?: time

            val event = VisibilityEvent.DwellTime(
                key = sessionData.analyticsKey,
                isStaleContent = wasAlreadyDispatched && config.trackStaleContent,
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

//            if (BuildConfig.DEBUG) {
//                Log.d(INTERNAL_LOG_TAG, "Dispatching DwellTime Event: $event")
//            }

            try {
                onDwellTime?.invoke(event)
            } catch (t: Throwable) {
                Log.e(INTERNAL_LOG_TAG, "Error in onDwellTime callback", t)
            }
        }
    }

    /**
     * Dispatches a [VisibilityEvent.Impression] event, typically for [TrackingMode.FIRST_IMPRESSION].
     */
    fun dispatchImpressionEvent(
        internalKey: Any,
        time: Long,
        scrollDirection: ScrollDirection,
        state: TrackerState,
        config: CozyConfig,
        onImpression: ((VisibilityEvent.Impression) -> Unit)?
    ) {
        // Get session data, but don't remove yet if still active.
        val sessionData = state.activeSessions[internalKey] ?: return
        val wasAlreadyDispatched = state.checkIfStaleAndMark(internalKey)

        // Respect trackStaleContent by only emitting the first impression when
        // it is disabled.
        if (wasAlreadyDispatched && !config.trackStaleContent) {
            return
        }

        val firstSeenTime = state.getFirstSeenTime(internalKey) ?: time

        val event = VisibilityEvent.Impression(
            key = sessionData.analyticsKey,
            isStaleContent = wasAlreadyDispatched && config.trackStaleContent,
            firstSeenAtMs = firstSeenTime,
            lastSeenAtMs = time,
            contentMetadata = ContentMetadata(
                contentPosition = sessionData.itemInfo.index,
                scrollDirection = scrollDirection,
                contentType = sessionData.itemInfo.contentType
            )
        )

//        if (BuildConfig.DEBUG) {
//            Log.d(INTERNAL_LOG_TAG, "Dispatching Impression Event: $event")
//        }
        try {
            onImpression?.invoke(event)
        } catch (t: Throwable) {
            Log.e(INTERNAL_LOG_TAG, "Error in onImpression callback", t)
        }
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
