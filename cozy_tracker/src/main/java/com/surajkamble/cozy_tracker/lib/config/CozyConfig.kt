package com.surajkamble.cozy_tracker.lib.config

/**
 * Configuration for the visibility tracker.
 *
 * @property minimumVisiblePercent The percentage of an item that must be visible (0.0 to 1.0).
 * @property debounceIntervalMs The time to wait after scrolling stops before processing visibility.
 * @property minDwellTimeMs The minimum time an item must be visible to trigger a visibility event.
 * @property trackStaleContent If `false`, the tracker will only report the first time an item is viewed and then ignore it.
 * @property trackingMode Defines the core behavior, such as tracking total time or only the first impression.
 */
data class CozyConfig(
    val minimumVisiblePercent: Float = 0.5f,
    val debounceIntervalMs: Long = 250L,
    val minDwellTimeMs: Long = 0L,
    val trackStaleContent: Boolean = true,
    val trackingMode: TrackingMode = TrackingMode.DWELL_TIME
) {
    init {
        require(minimumVisiblePercent in 0.0f..1.0f) {
            "minimumVisiblePercent must be between 0.0 and 1.0"
        }
    }
}
