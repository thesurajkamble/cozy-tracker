package com.surajkamble.cozy_tracker.lib

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.surajkamble.cozy_tracker.lib.config.VisibilityConfig
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A Composable function that observes a [LazyListState] and uses a provided [CozyTracker]
 * to track the visibility of its items.
 *
 * @param listState The [LazyListState] of the list to be observed.
 * @param tracker The pre-configured [CozyTracker] instance that will handle all logic.
 */
@Composable
fun TrackVisibility(
    listState: LazyListState,
    tracker: CozyTracker
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, tracker) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                tracker.flush()
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            tracker.flush()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(listState, tracker) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .filter { it.visibleItemsInfo.isNotEmpty() }
            .debounce(tracker.config.debounceIntervalMs)
            .onEach { layoutInfo ->
                // Delegate the processing to the tracker on a background thread.
                tracker.processVisibility(layoutInfo)
            }
            .flowOn(Dispatchers.Default)
            .launchIn(this)
    }
}

/**
 * A convenient Composable helper that creates and remembers a [LazyListState] and automatically
 * attaches a [CozyTracker] to it.
 *
 * This function streamlines the setup process by encapsulating the creation of the tracker
 * and the `TrackVisibility` side-effect. It returns a `LazyListState` that you can
 * pass directly to your `LazyColumn` or `LazyRow`.
 *
 * @param onEvent The mandatory callback to be invoked when a visibility event is ready.
 * @param config The configuration for the visibility tracker.
 * @return A `LazyListState` instance that is ready to be used and is being tracked.
 */
@Composable
fun rememberCozyListState(
    onEvent: (VisibilityEvent) -> Unit,
    config: VisibilityConfig = VisibilityConfig()
): LazyListState {
    val listState = rememberLazyListState()

    // The tracker will be re-remembered if the config object changes.
    val tracker = remember(config) {
        CozyTracker.Builder(onEvent)
            .minimumVisiblePercent(config.minimumVisiblePercent)
            .debounceIntervalMs(config.debounceIntervalMs)
            .build()
    }

    TrackVisibility(listState = listState, tracker = tracker)

    return listState
}
