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
import com.surajkamble.cozy_tracker.lib.CozyTracker
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

data class SampleItem(val id: String)

@Composable
fun TrackingSampleScreen(modifier: Modifier = Modifier) {
    val verticalItems = remember { List(200) { SampleItem(id = "V-$it") } }
    val horizontalItems = remember { List(200) { SampleItem(id = "H-$it") } }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Vertical List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        VerticalList(items = verticalItems)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Horizontal List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalList(items = horizontalItems)
    }
}

@Composable
private fun VerticalList(items: List<SampleItem>) {
    // Note the clean API: All calls are accessed via the CozyTracker object.
    val listState = CozyTracker.rememberCozyListState(
        onEvent = { event ->
            Log.d("CozyTracker - Vertical", "Event: $event")
        },
        config = CozyTracker.Config(
            minimumVisiblePercent = 0.6f,
            debounceIntervalMs = 300L
        )
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
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
    // Using the single-import API with a different configuration.
    val listState = CozyTracker.rememberCozyListState(
        onEvent = { event ->
            Log.d("CozyTracker - Horizontal", "Event: $event")
        },
        config = CozyTracker.Config(minimumVisiblePercent = 0.75f)
    )

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
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
