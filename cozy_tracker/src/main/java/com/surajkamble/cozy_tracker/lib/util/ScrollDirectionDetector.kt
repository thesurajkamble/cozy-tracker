package com.surajkamble.cozy_tracker.lib.util

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import com.surajkamble.cozy_tracker.lib.model.ScrollDirection

/**
 * A stateful utility for detecting the direction of scroll in a LazyList.
 */
internal class ScrollDirectionDetector {
    private var lastFirstVisibleItemScrollOffset: Int = -1
    private var lastScrollDirection: ScrollDirection = ScrollDirection.NONE

    /**
     * Determines the scroll direction by comparing the current scroll offset
     * with the offset from the previous check.
     *
     * @param layoutInfo The current layout info from the list.
     * @return The detected [ScrollDirection].
     */
    fun determineScrollDirection(layoutInfo: LazyListLayoutInfo): ScrollDirection {
        val firstItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return ScrollDirection.NONE
        val offset = firstItem.offset

        // If this is the first time, we can't determine direction yet.
        if (lastFirstVisibleItemScrollOffset == -1) {
            lastFirstVisibleItemScrollOffset = offset
            return ScrollDirection.NONE
        }

        val direction = when {
            offset > lastFirstVisibleItemScrollOffset -> ScrollDirection.BACKWARD
            offset < lastFirstVisibleItemScrollOffset -> ScrollDirection.FORWARD
            else -> ScrollDirection.NONE
        }

        lastFirstVisibleItemScrollOffset = offset
        lastScrollDirection = direction
        return direction
    }

    /**
     * Returns the last scroll direction that was detected.
     */
    fun getLastScrollDirection(): ScrollDirection {
        return lastScrollDirection
    }
}
