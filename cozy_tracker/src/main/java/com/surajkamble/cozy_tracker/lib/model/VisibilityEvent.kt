package com.surajkamble.cozy_tracker.lib.model

import androidx.compose.foundation.lazy.LazyListItemInfo

/**
 * Represents a visibility event triggered by the tracker.
 *
 * @param key The unique key of the item.
 * @param contentType The content type of the item, derived from [LazyListItemInfo.contentType].
 * @param isStaleContent True if an event for this key has been dispatched before in the tracker's lifetime.
 * @param visibleDurationMs The duration of the most recent visibility session in milliseconds.
 * @param firstSeenAtMs The timestamp (ms) when this key was first considered visible.
 * @param lastSeenAtMs The timestamp (ms) when this key stopped being visible.
 * @param totalSeenTimeMs The total accumulated visible time for this key across all sessions.
 * @param contentMetadata Additional metadata about the content and scroll state.
 */
data class VisibilityEvent(
    val key: Any,
    val contentType: Any?,
    val isStaleContent: Boolean,
    val visibleDurationMs: Long,
    val firstSeenAtMs: Long,
    val lastSeenAtMs: Long,
    val totalSeenTimeMs: Long,
    val contentMetadata: ContentMetadata
)
