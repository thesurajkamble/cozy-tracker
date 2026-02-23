package com.surajkamble.cozy_tracker.lib.internal

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner // Correct, non-deprecated import
import com.surajkamble.cozy_tracker.lib.api.Cozy
import com.surajkamble.cozy_tracker.lib.config.VisibilityConfig
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun rememberCozyListState(
    onEvent: (VisibilityEvent) -> Unit,
    config: VisibilityConfig = VisibilityConfig()
): LazyListState {
    val listState = rememberLazyListState()

    val tracker = remember(config) {
        Cozy.Builder(onEvent)
            .config(config)
            .build()
    }

    TrackVisibility(listState = listState, tracker = tracker)

    return listState
}

