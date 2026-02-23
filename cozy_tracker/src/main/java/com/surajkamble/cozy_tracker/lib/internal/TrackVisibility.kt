package com.surajkamble.cozy_tracker.lib.internal

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.surajkamble.cozy_tracker.lib.api.Cozy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
            .onEach { layoutInfo -> tracker.processVisibility(layoutInfo) }
            .flowOn(Dispatchers.Default)
            .launchIn(this)
    }
}
