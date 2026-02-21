package com.surajkamble.cozy_tracker.lib

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A Composable function that observes a [LazyListState] and tracks the visibility of its items.
 * It uses a [VisibilityTracker] instance to perform the actual calculations off the main thread.
 *
 * This function is the main entry point for using the visibility tracking library. It should be
 * called from within a Composable function that has access to the [LazyListState] of the list
 * you want to track.
 *
 * KDoc Highlights:
 * - Why `snapshotFlow` is used: `snapshotFlow` is a key performance feature of Compose.
 *   It converts Compose state objects (like `lazyListState.layoutInfo`) into a cold Flow.
 *   This is vastly more efficient than `Modifier.onGloballyPositioned` because it doesn't
 *   require extra composition or layout passes. It directly taps into the state system,
 *   emitting new values only when the underlying state actually changes.
 * - `distinctUntilChanged`: This is used to prevent redundant calculations. The `layoutInfo`
 *   can emit multiple times for a single frame. This operator ensures that we only process
 *   the layout information when it has meaningfully changed.
 * - `debounce`: This operator is crucial for performance. It waits for a specified period of
 *   quiescence (no new scroll events) before emitting the latest value. This prevents the
 *   tracker from running calculations during a fast fling, saving significant resources.
 *
 * @param listState The [LazyListState] of the `LazyColumn` or `LazyRow` to be observed.
 * @param config The [VisibilityConfig] to be used for tracking.
 * @param onItemViewed A lambda that will be invoked with the item's key and its accumulated
 * visible duration in milliseconds whenever a visibility session ends.
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberVisibilityTracker(
    listState: LazyListState,
    config: VisibilityConfig = VisibilityConfig(),
    onItemViewed: (key: Any, durationMs: Long) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Remember a single instance of the VisibilityTracker across recompositions.
    val tracker = remember { VisibilityTracker(onItemViewed) }

    // This effect handles the lifecycle of the observer.
    DisposableEffect(lifecycleOwner, listState) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // When the app is paused (e.g., user navigates away or backgrounds the app),
                // we must flush all active sessions to ensure we don't lose the dwell time
                // for items that were visible right before pausing.
                tracker.flush()
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            // Clean up by flushing one last time and removing the observer.
            tracker.flush()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // This is the core of the observation logic.
    LaunchedEffect(listState, config) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged() // Only process when the layout info has actually changed.
            .filter { it.visibleItemsInfo.isNotEmpty() } // Only process if there are visible items.
            .debounce(config.debounceIntervalMs) // Wait for scroll to settle.
            .onEach {
                // Delegate the actual processing to the tracker on a background thread.
                tracker.processVisibility(it.visibleItemsInfo, config)
            }
            .flowOn(Dispatchers.Default) // Perform all upstream operations on the default dispatcher.
            .launchIn(this) // Launch the flow in the LaunchedEffect's coroutine scope.
    }
}
