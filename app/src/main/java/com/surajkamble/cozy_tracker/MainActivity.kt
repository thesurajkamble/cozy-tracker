package com.surajkamble.cozy_tracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.surajkamble.cozy_tracker.lib.VisibilityConfig
import com.surajkamble.cozy_tracker.lib.rememberVisibilityTracker
import com.surajkamble.cozy_tracker.ui.theme.Cozy_trackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cozy_trackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DwellTimeTrackingSample(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class ListItem(val id: Int, val text: String)

@Composable
fun DwellTimeTrackingSample(modifier: Modifier = Modifier) {
    // A map to hold the accumulated dwell time for each item.
    // In a real app, this would likely be managed by a ViewModel.
    var accumulatedDwellTime by remember {
        mutableStateOf(mapOf<Any, Long>())
    }

    val lazyListState = rememberLazyListState()
    val items = remember {
        List(100) { ListItem(id = it, text = "Item #$it") }
    }

    // Attach the visibility tracker to the LazyListState.
    rememberVisibilityTracker(
        listState = lazyListState,
        config = VisibilityConfig(
            minimumVisiblePercent = 0.5f, // 50% of the item must be visible
            debounceIntervalMs = 500L      // Wait 500ms after scroll stops
        ),
        onItemViewed = { key, durationMs ->
            // This callback is invoked on a background thread.
            // Update the state by creating a new map.
            accumulatedDwellTime = accumulatedDwellTime.toMutableMap().apply {
                this[key] = durationMs
            }
            Log.d("DwellTime", "Item '$key' visible for ${durationMs}ms")
        }
    )

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            val dwellTime = accumulatedDwellTime[item.id] ?: 0L
            ListItemView(
                text = item.text,
                dwellTimeMs = dwellTime
            )
        }
    }
}

@Composable
fun ListItemView(text: String, dwellTimeMs: Long, modifier: Modifier = Modifier) {
    val dwellTimeSeconds = dwellTimeMs / 1000.0
    val backgroundColor = if (dwellTimeSeconds > 0) Color.LightGray else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Viewed for: ${String.format("%.2f", dwellTimeSeconds)}s",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
