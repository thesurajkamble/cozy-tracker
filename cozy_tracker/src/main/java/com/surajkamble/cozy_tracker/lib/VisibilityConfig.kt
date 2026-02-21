package com.surajkamble.cozy_tracker.lib

import androidx.compose.runtime.Immutable

/**
 * Configuration for the visibility tracker.
 *
 * @property minimumVisiblePercent A value between 0.0 and 1.0, representing the percentage
 * of the item's height or width that must be visible to be considered "visible".
 * Defaults to 0.5f (50%).
 * @property debounceIntervalMs The time in milliseconds to wait after the last scroll event
 * before processing visibility changes. This helps to avoid processing rapid scroll events
 * and only calculate visibility when the list is settled. Defaults to 250ms.
 */
@Immutable
data class VisibilityConfig(
    val minimumVisiblePercent: Float = 0.5f,
    val debounceIntervalMs: Long = 250L
) {
    init {
        require(minimumVisiblePercent in 0.0f..1.0f) {
            "minimumVisiblePercent must be between 0.0 and 1.0"
        }
    }
}
