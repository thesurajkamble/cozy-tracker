package com.surajkamble.cozy_tracker.lib

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * The single entry point for using the CozyTracker library.
 *
 * Import this object to access all library features:
 * - `CozyTracker.rememberCozyListState`: The main Composable to track a list.
 * - `CozyTracker.Config`: The configuration data class.
 * - `CozyTracker.Event`: The output event data class.
 */
object CozyTracker {

    data class Config(
        val minimumVisiblePercent: Float = 0.5f,
        val debounceIntervalMs: Long = 250L
    ) {
        init {
            require(minimumVisiblePercent in 0.0f..1.0f) { "minimumVisiblePercent must be between 0.0 and 1.0" }
        }
    }

    enum class ScrollDirection {
        FORWARD, BACKWARD, NONE
    }

    data class ContentMetadata(
        val contentPosition: Int,
        val scrollDirection: ScrollDirection
    )

    data class Event(
        val key: Any,
        val contentType: Any?,
        val isStaleContent: Boolean,
        val visibleDurationMs: Long,
        val firstSeenAtMs: Long,
        val lastSeenAtMs: Long,
        val totalSeenTimeMs: Long,
        val contentMetadata: ContentMetadata
    )

    @Composable
    fun rememberCozyListState(
        onEvent: (Event) -> Unit,
        config: Config = Config()
    ): LazyListState {
        val listState = rememberLazyListState()

        val tracker = remember(config) {
            Engine.Builder(onEvent)
                .minimumVisiblePercent(config.minimumVisiblePercent)
                .debounceIntervalMs(config.debounceIntervalMs)
                .build()
        }

        TrackVisibility(listState = listState, tracker = tracker)

        return listState
    }
}

@Composable
private fun TrackVisibility(listState: LazyListState, tracker: Engine) {
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

private class Engine private constructor(
    val config: CozyTracker.Config,
    private val onEvent: (CozyTracker.Event) -> Unit
) {
    private data class SessionData(val startTimeMs: Long, val itemInfo: LazyListItemInfo)

    private val activeSessions = ConcurrentHashMap<Any, SessionData>()
    private val totalSeenTimes = ConcurrentHashMap<Any, Long>()
    private val firstSeenTimestamps = ConcurrentHashMap<Any, Long>()
    // API 23 compatible way to create a thread-safe Set-like structure.
    private val dispatchedKeys = ConcurrentHashMap<Any, Boolean>()
    private var lastFirstVisibleItemScrollOffset: Int = 0
    private val trackerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null

    class Builder(private val onEvent: (CozyTracker.Event) -> Unit) {
        private var minimumVisiblePercent: Float = 0.5f
        private var debounceIntervalMs: Long = 250L

        fun minimumVisiblePercent(percent: Float) = apply { this.minimumVisiblePercent = percent }
        fun debounceIntervalMs(ms: Long) = apply { this.debounceIntervalMs = ms }

        fun build(): Engine {
            val config = CozyTracker.Config(
                minimumVisiblePercent = minimumVisiblePercent,
                debounceIntervalMs = debounceIntervalMs
            )
            return Engine(config, onEvent)
        }
    }

    fun processVisibility(layoutInfo: LazyListLayoutInfo) {
        processingJob?.cancel()
        processingJob = trackerScope.launch {
            val now = System.currentTimeMillis()
            val visibleItemsMap = layoutInfo.visibleItemsInfo.associateBy { it.key }
            val scrollDirection = determineScrollDirection(layoutInfo)

            val endedSessionKeys = activeSessions.keys.filter { key ->
                val itemInfo = visibleItemsMap[key]
                itemInfo == null || !itemInfo.isVisible(layoutInfo, config)
            }
            endedSessionKeys.forEach { endSession(it, now, scrollDirection) }

            visibleItemsMap.values.forEach { itemInfo ->
                if (!activeSessions.containsKey(itemInfo.key) && itemInfo.isVisible(layoutInfo, config)) {
                    startSession(itemInfo, now)
                }
            }
        }
    }

    fun flush() {
        processingJob?.cancel()
        trackerScope.launch {
            val now = System.currentTimeMillis()
            val activeKeys = activeSessions.keys.toList()
            activeKeys.forEach { endSession(it, now, CozyTracker.ScrollDirection.NONE) }
        }
    }

    private fun startSession(itemInfo: LazyListItemInfo, time: Long) {
        activeSessions[itemInfo.key] = SessionData(time, itemInfo)
        firstSeenTimestamps.putIfAbsent(itemInfo.key, time)
    }

    private fun endSession(key: Any, time: Long, scrollDirection: CozyTracker.ScrollDirection) {
        val sessionData = activeSessions.remove(key) ?: return
        val visibleDurationMs = time - sessionData.startTimeMs

        if (visibleDurationMs > 0) {
            val totalSeenTimeMs = (totalSeenTimes[key] ?: 0L) + visibleDurationMs
            totalSeenTimes[key] = totalSeenTimeMs

            // If putIfAbsent returns null, the key was new. If it returns a value, it was already there.
            val wasAlreadyDispatched = dispatchedKeys.putIfAbsent(key, true) != null

            val event = CozyTracker.Event(
                key = key,
                contentType = sessionData.itemInfo.contentType,
                isStaleContent = wasAlreadyDispatched,
                visibleDurationMs = visibleDurationMs,
                firstSeenAtMs = firstSeenTimestamps[key] ?: time,
                lastSeenAtMs = time,
                totalSeenTimeMs = totalSeenTimeMs,
                contentMetadata = CozyTracker.ContentMetadata(
                    contentPosition = sessionData.itemInfo.index,
                    scrollDirection = scrollDirection
                )
            )
            onEvent(event)
        }
    }

    private fun determineScrollDirection(layoutInfo: LazyListLayoutInfo): CozyTracker.ScrollDirection {
        val firstItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return CozyTracker.ScrollDirection.NONE
        val offset = firstItem.offset
        return when {
            offset > lastFirstVisibleItemScrollOffset -> CozyTracker.ScrollDirection.BACKWARD
            offset < lastFirstVisibleItemScrollOffset -> CozyTracker.ScrollDirection.FORWARD
            else -> CozyTracker.ScrollDirection.NONE
        }.also { lastFirstVisibleItemScrollOffset = offset }
    }

    private fun LazyListItemInfo.isVisible(layoutInfo: LazyListLayoutInfo, config: CozyTracker.Config): Boolean {
        val itemSize = this.size.toFloat()
        if (itemSize <= 0) return false

        val viewportStart = if (layoutInfo.orientation == Orientation.Vertical) layoutInfo.viewportStartOffset else 0
        val viewportEnd = if (layoutInfo.orientation == Orientation.Vertical) layoutInfo.viewportEndOffset else layoutInfo.viewportSize.width

        val itemStart = this.offset.toFloat()
        val itemEnd = itemStart + itemSize

        val visiblePart = max(0f, min(itemEnd, viewportEnd.toFloat()) - max(itemStart, viewportStart.toFloat()))
        return (visiblePart / itemSize) >= config.minimumVisiblePercent
    }
}