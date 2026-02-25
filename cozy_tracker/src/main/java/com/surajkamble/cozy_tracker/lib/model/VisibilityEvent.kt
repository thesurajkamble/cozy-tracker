package com.surajkamble.cozy_tracker.lib.model

/**
 * Represents the different visibility events that Cozy can emit.
 *
 * The [key] on each event is the logical content identifier exposed by the
 * library. By default this is the Compose lazy list item's key, but when a
 * custom `keyExtractor` is supplied to `Modifier.cozyTrack` it will be the
 * result of that extractor instead.
 *
 * The [isStaleContent] flag is driven by [CozyConfig.trackStaleContent]:
 * - When `trackStaleContent = false`, only the first event for a key is
 *   emitted. Subsequent exposures are ignored and `isStaleContent` is always
 *   `false`.
 * - When `trackStaleContent = true`, subsequent events for a key are emitted
 *   with `isStaleContent = true`, allowing consumers to distinguish fresh vs
 *   stale content.
 */
sealed class VisibilityEvent {

    abstract val key: Any
    abstract val isStaleContent: Boolean
    abstract val firstSeenAtMs: Long
    abstract val lastSeenAtMs: Long
    abstract val contentMetadata: ContentMetadata

    /**
     * Event dispatched when an item's visibility session ends, reporting its dwell time.
     *
     * @param key The unique key of the item.
     * @param isStaleContent True if an event for this key has been dispatched before in the tracker's lifetime.
     * @param visibleDurationMs The duration of the most recent visibility session in milliseconds.
     * @param firstSeenAtMs The timestamp (ms) when this key was first considered visible.
     * @param lastSeenAtMs The timestamp (ms) when this key stopped being visible.
     * @param totalSeenTimeMs The total accumulated visible time for this key across all sessions.
     * @param contentMetadata Additional metadata about the content, including its position, scroll direction, and content type.
     */
    data class DwellTime(
        override val key: Any,
        override val isStaleContent: Boolean,
        val visibleDurationMs: Long,
        override val firstSeenAtMs: Long,
        override val lastSeenAtMs: Long,
        val totalSeenTimeMs: Long,
        override val contentMetadata: ContentMetadata
    ) : VisibilityEvent()

    /**
     * Event dispatched when an item first becomes visible (an impression event).
     * This is typically used with [TrackingMode.FIRST_IMPRESSION].
     *
     * @param key The unique key of the item.
     * @param isStaleContent True if an event for this key has been dispatched before in the tracker's lifetime.
     * @param firstSeenAtMs The timestamp (ms) when this key was first considered visible.
     * @param lastSeenAtMs The timestamp (ms) when this event was dispatched (should be same as firstSeenAtMs).
     * @param contentMetadata Additional metadata about the content, including its position, scroll direction, and content type.
     */
    data class Impression( // New event type for impressions
        override val key: Any,
        override val isStaleContent: Boolean,
        override val firstSeenAtMs: Long,
        override val lastSeenAtMs: Long, // Should be same as firstSeenAtMs for impression
        override val contentMetadata: ContentMetadata
    ) : VisibilityEvent()

    /**
     * Event dispatched when the application moves to the background (ON_PAUSE).
     * This signals that all active visibility sessions were flushed.
     *
     * @param timestampMs The timestamp (ms) when the app went to the background.
     * @param flushedItems A list of keys for items that had their sessions ended during this flush.
     */
    data class AppBackgrounded(
        val timestampMs: Long,
        val flushedItems: List<Any>
    ) : VisibilityEvent() {
        // These abstract properties are not directly applicable to AppBackgrounded event
        // However, making them nullable or providing a default could be an alternative
        // For simplicity in this example, they are not used for AppBackgrounded.
        override val key: Any get() = "APP_LIFECYCLE"
        override val isStaleContent: Boolean get() = false
        override val firstSeenAtMs: Long get() = timestampMs
        override val lastSeenAtMs: Long get() = timestampMs
        override val contentMetadata: ContentMetadata get() = ContentMetadata(0, ScrollDirection.NONE, null)
    }
}
