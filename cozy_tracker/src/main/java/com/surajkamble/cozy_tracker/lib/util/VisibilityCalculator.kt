package com.surajkamble.cozy_tracker.lib.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import com.surajkamble.cozy_tracker.lib.config.VisibilityConfig
import kotlin.math.max
import kotlin.math.min

/**
 * A utility for calculating the visibility of a list item.
 */
internal object VisibilityCalculator {

    /**
     * Determines if a [LazyListItemInfo] is "visible" based on the [VisibilityConfig].
     *
     * @param itemInfo The item to check.
     * @param layoutInfo The [LazyListLayoutInfo] used to get viewport and orientation details.
     * @param config The configuration to use for the visibility check.
     * @return `true` if the item meets the minimum visibility percentage, `false` otherwise.
     */
    fun isVisible(
        itemInfo: LazyListItemInfo,
        layoutInfo: LazyListLayoutInfo,
        config: VisibilityConfig
    ): Boolean {
        val itemSize = itemInfo.size.toFloat()
        if (itemSize <= 0) return false

        val visibleFraction = when (layoutInfo.orientation) {
            Orientation.Vertical -> {
                val viewportStart = layoutInfo.viewportStartOffset.toFloat()
                val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
                val itemStart = itemInfo.offset.toFloat()
                val itemEnd = itemStart + itemSize
                val visiblePart = max(0f, min(itemEnd, viewportEnd) - max(itemStart, viewportStart))
                visiblePart / itemSize
            }
            Orientation.Horizontal -> {
                val viewportStart = 0f
                val viewportEnd = layoutInfo.viewportSize.width.toFloat()
                val itemStart = itemInfo.offset.toFloat()
                val itemEnd = itemStart + itemSize
                val visiblePart = max(0f, min(itemEnd, viewportEnd) - max(itemStart, viewportStart))
                visiblePart / itemSize
            }
        }

        return visibleFraction >= config.minimumVisiblePercent
    }
}
