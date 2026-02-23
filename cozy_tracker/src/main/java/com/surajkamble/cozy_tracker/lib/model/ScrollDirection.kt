package com.surajkamble.cozy_tracker.lib.model

/**
 * Represents the direction of scroll.
 */
enum class ScrollDirection {
    FORWARD, // e.g., Down in a LazyColumn, Right in a LazyRow
    BACKWARD, // e.g., Up in a LazyColumn, Left in a LazyRow
    NONE // Initial state or no scroll detected
}
