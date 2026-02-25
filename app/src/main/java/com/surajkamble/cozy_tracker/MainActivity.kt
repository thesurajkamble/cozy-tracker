package com.surajkamble.cozy_tracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.surajkamble.cozy_tracker.lib.api.CozyConfig
import com.surajkamble.cozy_tracker.lib.api.TrackingMode
import com.surajkamble.cozy_tracker.lib.api.cozyTrack
import com.surajkamble.cozy_tracker.lib.model.VisibilityEvent
import com.surajkamble.cozy_tracker.ui.theme.Cozy_trackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cozy_trackerTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        TrackingSampleScreen(modifier = Modifier.padding(innerPadding))
                    }
            }
        }
    }
}

/**
 * A reusable callback function to handle and log visibility events.
 */
private fun onVisibilityEvent(listName: String, event: VisibilityEvent) {
    when (event) {
        is VisibilityEvent.DwellTime -> {
            Log.d("CozyTracker - $listName", "[DWELL TIME]: Key=${event.key}, Total=${event.totalSeenTimeMs}ms")
        }
        is VisibilityEvent.Impression -> {
            Log.d("CozyTracker - $listName", "[IMPRESSION]: Key=${event.key}, FirstSeen=${event.firstSeenAtMs}")
        }
        is VisibilityEvent.AppBackgrounded -> {
            Log.d("CozyTracker - $listName", "[APP BACKGROUNDED]: ${event.flushedItems.size} items flushed. Timestamp: ${event.timestampMs}")
        }
    }
}

data class SampleItem(val id: String)

@Composable
fun TrackingSampleScreen(modifier: Modifier = Modifier) {
    val verticalItems = remember { List(200) { SampleItem(id = "V-$it") } }
    val horizontalItems = remember { List(200) { SampleItem(id = "H-$it") } }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Vertical List (Dwell Time)",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        VerticalList(items = verticalItems)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Horizontal List (First Impression)",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalList(items = horizontalItems)
    }
}

@Composable
private fun VerticalList(items: List<SampleItem>) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .cozyTrack(
                listState = listState,
                onDwellTime = { event -> onVisibilityEvent("Vertical", event) },
                configure = {
                    minimumVisiblePercent = 0.6f
                }
            )
    ) {
        items(items, key = { it.id }, contentType = { "vertical-item" }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 4.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Item ${item.id}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HorizontalList(items: List<SampleItem>) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .cozyTrack(
                listState = listState,
                onImpression = { event -> onVisibilityEvent("Horizontal", event) },
                configure = {
                    minimumVisiblePercent = 0.75f
                    trackingMode = TrackingMode.FIRST_IMPRESSION
                }
            ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(items, key = { it.id }, contentType = { "horizontal-item" }) { item ->
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(100.dp)
                    .padding(horizontal = 4.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Item ${item.id}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
