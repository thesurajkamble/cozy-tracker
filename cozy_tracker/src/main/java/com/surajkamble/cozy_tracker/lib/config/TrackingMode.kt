package com.surajkamble.cozy_tracker.lib.config

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
