package com.surajkamble.cozy_tracker.lib.model

/**
 * Contains metadata about the state of the content when a visibility event was fired.
 *
 * @param contentPosition The index of the item within the list.
 * @param scrollDirection The direction of the scroll when the event was triggered.
 * @param contentType The content type of the item, derived from the LazyList DSL.
 */
data class ContentMetadata(
    val contentPosition: Int,
    val scrollDirection: ScrollDirection,
    val contentType: Any?
)
