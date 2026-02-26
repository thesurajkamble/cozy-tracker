# Cozy Tracker
A lightweight, non-intrusive Jetpack Compose library for tracking item visibility and dwell time in `LazyColumn` and `LazyRow`.

[![JitPack](https://jitpack.io/v/surajkamble/cozy_tracker.svg)](https://jitpack.io/#surajkamble/cozy_tracker)

<img width="2048" height="1363" alt="banner_img" src="https://github.com/user-attachments/assets/09cdf4ff-2a4a-45d5-86b4-9cc98a35282b" />

## Features

-   **Performance First:** All calculations are performed on a background thread, ensuring smooth 60fps scrolling.
-   **Idiomatic API:** Integrates seamlessly with Jetpack Compose using a `.cozyTrack()` modifier.
-   **Type-Safe Events:** Provides structured `DwellTime` and `Impression` events, not generic maps.
-   **Flexible Configuration:** Set global defaults and override them locally for each list.
-   **Automatic Lifecycle Handling:** Automatically handles app lifecycle events to ensure data is not lost.

## Setup

1.  **Add JitPack repository**

    Add JitPack to your project's `settings.gradle.kts` file:

    ```kotlin
    dependencyResolutionManagement {
        repositories {
            // ... other repositories
            maven { url 'https://jitpack.io' }
        }
    }
    ```

2.  **Add the dependency**

    Add the Cozy Tracker dependency to your module's `build.gradle.kts` file. Make sure to replace `cozy_trackerV1.0.1` with the latest release tag.

    ```kotlin
    dependencies {
        implementation("com.github.surajkamble:cozy_tracker:cozy_trackerV1.0.1")
    }
    ```

## Usage

Cozy Tracker works by attaching a modifier to your `LazyColumn` or `LazyRow`.

1.  **Create a `LazyListState`** as you normally would.
2.  **Apply the `.cozyTracker()` modifier** to your list, passing the `listState` and the event callbacks you are interested in (`onDwellTime` or `onImpression`).
3.  **(Optional)** Provide a local `CozyConfig` to override default settings for a specific list.

### Example

```kotlin
import com.surajkamble.cozy_tracker.lib.api.CozyConfig
import com.surajkamble.cozy_tracker.lib.api.TrackingMode
import com.surajkamble.cozy_tracker.lib.api.cozyTracker

// ...

@Composable
private fun MyTrackedList() {
    val listState = rememberLazyListState()
    val items = (1..100).map { "Item #$it" }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .cozyTracker(
                listState = listState,
                // Listen for dwell time events
                onDwellTime = { event ->
                    Log.d("MyApp", "Dwell Time Event: ${event.key} was visible for ${event.visibleDurationMs}ms")
                },
                // Listen for first-impression events
                onImpression = { event ->
                    Log.d("MyApp", "Impression Event: ${event.key} was first seen at ${event.firstSeenAtMs}")
                },
                // Locally override the configuration for this list
                config = CozyConfig(
                    minimumVisiblePercent = 0.7f, // 70% must be visible
                    trackingMode = TrackingMode.DWELL_TIME
                )
            )
    ) {
        items(items, key = { it }) { item ->
            // Your item composable
            Text(text = item, modifier = Modifier.padding(16.dp).fillMaxWidth())
        }
    }
}
```

### A Note on Keys

**Providing a stable and unique `key` for each item in your `LazyColumn` or `LazyRow` is mandatory for Cozy Tracker to work correctly.**

-   **Why?** Cozy Tracker uses this `key` as the fundamental identifier to track an item's state across recompositions and scroll events. It allows the library to accumulate dwell time accurately and know when an item is being seen for the first time.

-   **What happens if keys are not provided?** If you do not provide keys, `LazyColumn` will use the item's position as its key. This is unstable. If an item is added, removed, or reordered in your list, its position changes, and Cozy Tracker will incorrectly treat it as a brand new item, losing all its previous tracking history. This will lead to inaccurate `totalSeenTimeMs` and `isStaleContent` values.

-   **Best Practice:** Always use a stable and unique identifier from your data model as the key. For example, if you are loading data from an API, use the `articleId`, `productId`, or `videoId` from your API response.

```kotlin
// GOOD: Using a stable ID from your data
items(items, key = { item -> item.id }) { ... }

// AVOID: Relying on the unstable item index
itemsIndexed(items) { index, item ->
    // Using index as a key can lead to tracking errors!
    // ...
}
```

## Configuration

You can set global defaults for all trackers by wrapping your app's content with `ProvideCozyConfig` compositionLocal.

```kotlin
import com.surajkamble.cozy_tracker.lib.api.CozyConfig
import com.surajkamble.cozy_tracker.lib.api.ProvideCozyConfig

// In your MainActivity or root Composable
setContent {
    ProvideCozyConfig(config = CozyConfig(minDwellTimeMs = 500L)) {
        // Your app's content
    }
}
```
