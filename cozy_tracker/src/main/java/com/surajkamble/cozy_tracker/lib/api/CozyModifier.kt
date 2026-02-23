package com.surajkamble.cozy_tracker.lib.api

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.surajkamble.cozy_tracker.lib.config.LocalCozyConfig
import com.surajkamble.cozy_tracker.lib.config.CozyConfig
import com.surajkamble.cozy_tracker.lib.internal.Cozy
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Internal implementation of the cozyTracker modifier.
 */
fun Modifier.cozyTracker(
    listState: LazyListState,
    onDwellTime: ((VisibilityEvent.DwellTime) -> Unit)? = null,
    onImpression: ((VisibilityEvent.Impression) -> Unit)? = null,
    config: CozyConfig? = null
): Modifier = composed {

    val globalConfig = LocalCozyConfig.current
    val finalConfig = config ?: globalConfig

    val tracker = remember(finalConfig, onDwellTime, onImpression) {
        Cozy.Builder(
            onDwellTime = onDwellTime,
            onImpression = onImpression
        )
            .config(finalConfig)
            .build()
    }
    TrackVisibility(listState = listState, tracker = tracker)
    this
}

@Composable
private fun TrackVisibility(listState: LazyListState, tracker: Cozy) {

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
            .onEach { layoutInfo -> tracker.processVisibility(layoutInfo) }
            .flowOn(Dispatchers.Default)
            .launchIn(this)
    }
}
