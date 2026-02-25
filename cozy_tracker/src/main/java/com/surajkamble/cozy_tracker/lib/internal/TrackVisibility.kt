package com.surajkamble.cozy_tracker.lib.internal

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.surajkamble.cozy_tracker.lib.internal.Cozy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect

@OptIn(FlowPreview::class)
@Composable
internal fun TrackVisibility(listState: LazyListState, tracker: Cozy) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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
            // All interaction with Compose UI types happens on the main thread.
            // The tracking engine itself only works with these types on this
            // coroutine to keep things main-safe.
            .collect { layoutInfo ->
                tracker.processVisibility(layoutInfo)
            }
    }
}
