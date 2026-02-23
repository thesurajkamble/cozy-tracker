package com.surajkamble.cozy_tracker.lib.util

import com.surajkamble.cozy_tracker.lib.config.VisibilityConfig
import com.surajkamble.cozy_tracker.lib.model.ContentMetadata
import com.surajkamble.cozy_tracker.lib.model.ScrollDirection
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent

/**
 * A utility responsible for creating and dispatching visibility events.
 */
internal object EventDispatcher {

    /**
     * Ends a session, constructs the visibility event, and dispatches it if it meets the config criteria.
     */
    fun dispatchEndSessionEvent(
        key: Any,
        time: Long,
        scrollDirection: ScrollDirection,
        state: TrackerState,
        config: VisibilityConfig,
        onEvent: (VisibilityEvent) -> Unit
    ) {
        val sessionData = state.endSession(key) ?: return
        val visibleDurationMs = time - sessionData.startTimeMs

        if (visibleDurationMs > 0 && visibleDurationMs >= config.minDwellTimeMs) {
            val totalSeenTimeMs = state.updateTotalSeenTime(key, visibleDurationMs)
            val wasAlreadyDispatched = state.isStaleAndMark(key)
            val firstSeenTime = state.getFirstSeenTime(key) ?: time

            val event = VisibilityEvent(
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
            onEvent(event)
        }
    }
}
