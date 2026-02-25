package com.surajkamble.cozy_tracker.lib.util

import androidx.compose.foundation.lazy.LazyListItemInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * A data class holding the state for an active visibility session.
 */
internal data class SessionData(
    val startTimeMs: Long,
    val itemInfo: LazyListItemInfo,
    /**
     * Logical key that will be exposed to consumers via [VisibilityEvent.key].
     * This may differ from [LazyListItemInfo.key] when a custom key extractor
     * is used, but internal tracking still uses the Compose key.
     */
    val analyticsKey: Any
)

/**
 * Encapsulates the complete, thread-safe state management for the tracking engine.
 */
internal class TrackerState {
    /**
     * Stores the start time and item info for all currently visible items.
     * Key: The unique key of the item.
     */
    val activeSessions = ConcurrentHashMap<Any, SessionData>()

    /**
     * Stores the total accumulated dwell time for all items ever seen.
     * Key: The unique key of the item.
     */
    val totalSeenTimes = ConcurrentHashMap<Any, Long>()

    /**
     * Stores the timestamp when an item was first seen.
     * Key: The unique key of the item.
     */
    val firstSeenTimestamps = ConcurrentHashMap<Any, Long>()

    /**
     * Stores a set of all keys for which an event has already been dispatched.
     * This is used to determine if content is "stale".
     */
    val dispatchedKeys = ConcurrentHashMap<Any, Boolean>()

    /**
     * Starts a new visibility session for a given item.
     */
    fun startSession(itemInfo: LazyListItemInfo, time: Long, analyticsKey: Any) {
        activeSessions[itemInfo.key] = SessionData(time, itemInfo, analyticsKey)
        firstSeenTimestamps.putIfAbsent(itemInfo.key, time)
    }

    /**
     * Ends a visibility session for a given item and returns its data.
     */
    fun endSession(key: Any): SessionData? {
        return activeSessions.remove(key)
    }

    /**
     * Updates the total seen time for an item and returns the new total.
     */
    fun updateTotalSeenTime(key: Any, duration: Long): Long {
        val newTotal = (totalSeenTimes[key] ?: 0L) + duration
        totalSeenTimes[key] = newTotal
        return newTotal
    }

    /**
     * Checks if a key has been dispatched before and marks it as dispatched.
     * @return `true` if the key had been dispatched previously, `false` otherwise.
     */
    fun checkIfStaleAndMark(key: Any): Boolean {
        return dispatchedKeys.putIfAbsent(key, true) != null
    }

    /**
     * Retrieves the timestamp when a key was first seen.
     */
    fun getFirstSeenTime(key: Any): Long? {
        return firstSeenTimestamps[key]
    }
    /**
     * Ends all active sessions and returns them in a list.
     */
    fun endAllActiveSessions(): List<Any> {
        return activeSessions.keys().toList()
    }
}
