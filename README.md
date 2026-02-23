An Android library to track lazyColumn and lazyRow view time.


<img width="2048" height="1363" alt="banner_img" src="https://github.com/user-attachments/assets/74e25b0c-2349-46c4-a7d9-44eef9f9db1f" />


# CozyTracker Library — Sequence Diagrams

A detailed sequence diagram documenting the flow of the `cozy_tracker` library for Jetpack Compose `LazyList` visibility tracking.

---

## 1. Initialization Flow

How the tracker is set up when a Composable uses `rememberCozyListState`.

```mermaid
sequenceDiagram
    autonumber
    participant App as App/LazyColumn
    participant CozyListState as CozyListState
    participant Builder as Cozy.Builder
    participant Cozy as Cozy Engine
    participant TrackVis as TrackVisibility

    App->>CozyListState: rememberCozyListState(onEvent, config)
    CozyListState->>CozyListState: rememberLazyListState()
    CozyListState->>Builder: Cozy.Builder(onEvent).config(config).build()
    Builder->>Cozy: new Cozy(config, onEvent)
    Note over Cozy: Creates TrackerState, ScrollDirectionDetector
    Builder-->>CozyListState: tracker instance
    CozyListState->>TrackVis: TrackVisibility(listState, tracker)
    TrackVis->>TrackVis: DisposableEffect: lifecycle observer
    TrackVis->>TrackVis: LaunchedEffect: snapshotFlow(listState.layoutInfo)
    CozyListState-->>App: LazyListState (to pass to LazyColumn/LazyRow)
```

---

## 2. Layout Change → Visibility Processing (Main Flow)

When the user scrolls, layout info changes flow through the pipeline.

```mermaid
sequenceDiagram
    autonumber
    participant LazyList as LazyColumn/LazyRow
    participant TrackVis as TrackVisibility
    participant SnapshotFlow as snapshotFlow
    participant Cozy as Cozy Engine
    participant ScrollDetector as ScrollDirectionDetector
    participant State as TrackerState
    participant VisibilityCalc as VisibilityCalculator
    participant EventDisp as EventDispatcher
    participant App as App (onEvent)

    LazyList->>LazyList: User scrolls / list renders
    Note over LazyList: listState.layoutInfo updates

    SnapshotFlow->>SnapshotFlow: distinctUntilChanged()
    SnapshotFlow->>SnapshotFlow: filter(visibleItemsInfo.isNotEmpty())
    SnapshotFlow->>SnapshotFlow: debounce(config.debounceIntervalMs)
    SnapshotFlow->>Cozy: processVisibility(layoutInfo)

    Cozy->>Cozy: Cancel previous processingJob
    Cozy->>Cozy: launch on Dispatchers.Default
    Cozy->>Cozy: visibleItemsMap = visibleItemsInfo.associateBy(key)
    Cozy->>ScrollDetector: determineScrollDirection(layoutInfo)
    ScrollDetector-->>Cozy: ScrollDirection (FORWARD/BACKWARD/NONE)

    rect rgb(255, 240, 240)
        Note over Cozy,EventDisp: Phase 1: End sessions for items no longer visible
        Cozy->>Cozy: endedSessionKeys = activeSessions.keys.filter(not visible)
        loop For each ended key
            Cozy->>EventDisp: dispatchEndSessionEvent(key, now, scrollDir, state, config, onEvent)
            EventDisp->>State: endSession(key)
            State-->>EventDisp: SessionData
            EventDisp->>EventDisp: Check visibleDurationMs >= minDwellTimeMs
            alt Meets criteria
                EventDisp->>State: updateTotalSeenTime(key, duration)
                EventDisp->>State: isStaleAndMark(key)
                EventDisp->>State: getFirstSeenTime(key)
                EventDisp->>EventDisp: Build VisibilityEvent
                EventDisp->>App: onEvent(VisibilityEvent)
            end
        end
    end

    rect rgb(240, 255, 240)
        Note over Cozy,VisibilityCalc: Phase 2: Start sessions for newly visible items
        loop For each visible item
            Cozy->>Cozy: Check isAlreadyDone (dispatchedKeys + config)
            Cozy->>Cozy: Check !activeSessions.containsKey(key)
            Cozy->>VisibilityCalc: isVisible(itemInfo, layoutInfo, config)
            VisibilityCalc->>VisibilityCalc: Compute visibleFraction vs viewport
            VisibilityCalc-->>Cozy: true if >= minimumVisiblePercent
            alt Item is new and visible
                Cozy->>State: startSession(itemInfo, now)
                alt TrackingMode.FIRST_IMPRESSION
                    Cozy->>EventDisp: dispatchEndSessionEvent(...)
                    EventDisp->>App: onEvent(VisibilityEvent)
                end
            end
        end
    end
```

---

## 3. Visibility Calculation Detail

How `VisibilityCalculator` determines if an item is "visible."

```mermaid
sequenceDiagram
    participant Cozy as Cozy Engine
    participant VisibilityCalc as VisibilityCalculator
    participant Config as VisibilityConfig

    Cozy->>VisibilityCalc: isVisible(itemInfo, layoutInfo, config)
    VisibilityCalc->>VisibilityCalc: itemSize = itemInfo.size
    alt itemSize <= 0
        VisibilityCalc-->>Cozy: false
    end

    alt Orientation.Vertical
        VisibilityCalc->>VisibilityCalc: viewportStart/End from layoutInfo
        VisibilityCalc->>VisibilityCalc: itemStart/End from itemInfo.offset + size
        VisibilityCalc->>VisibilityCalc: visiblePart = overlap(viewport, item)
        VisibilityCalc->>VisibilityCalc: visibleFraction = visiblePart / itemSize
    else Orientation.Horizontal
        VisibilityCalc->>VisibilityCalc: Same logic with width
    end

    VisibilityCalc->>Config: minimumVisiblePercent
    VisibilityCalc->>VisibilityCalc: visibleFraction >= minimumVisiblePercent?
    VisibilityCalc-->>Cozy: Boolean result
```

---

## 4. Lifecycle: Flush (ON_PAUSE / onDispose)

When the app goes to background or the Composable is disposed.

```mermaid
sequenceDiagram
    autonumber
    participant Lifecycle as Lifecycle
    participant TrackVis as TrackVisibility
    participant Cozy as Cozy Engine
    participant State as TrackerState
    participant EventDisp as EventDispatcher
    participant ScrollDetector as ScrollDirectionDetector
    participant App as App (onEvent)

    alt ON_PAUSE
        Lifecycle->>TrackVis: LifecycleEventObserver(ON_PAUSE)
        TrackVis->>Cozy: flush()
    else Composable disposed
        TrackVis->>Cozy: flush() [in onDispose]
    end

    Cozy->>Cozy: Cancel processingJob
    Cozy->>Cozy: launch on trackerScope
    Cozy->>State: endAllActiveSessions()
    Note over State: Returns list of active keys
    State->>State: activeSessions.keys().toList()
    State-->>Cozy: activeKeys

    loop For each active key
        Cozy->>ScrollDetector: getLastScrollDirection()
        ScrollDetector-->>Cozy: lastScrollDirection
        Cozy->>EventDisp: dispatchEndSessionEvent(key, now, scrollDir, state, config, onEvent)
        EventDisp->>State: endSession(key)
        EventDisp->>EventDisp: Build VisibilityEvent if meets minDwellTime
        EventDisp->>App: onEvent(VisibilityEvent)
    end

    alt onDispose
        TrackVis->>Lifecycle: removeObserver(lifecycleObserver)
    end
```

---

## 5. Component Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              App / Consumer                                  │
│  rememberCozyListState(onEvent, config) → LazyListState                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CozyListState (Composable)                           │
│  - Creates LazyListState                                                     │
│  - Builds Cozy via Builder                                                   │
│  - Renders TrackVisibility(listState, tracker)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
┌──────────────────────────────┐        ┌──────────────────────────────────────┐
│    TrackVisibility           │        │           Cozy (Engine)               │
│  - snapshotFlow(layoutInfo)  │───────▶│  - processVisibility(layoutInfo)      │
│  - debounce, filter          │        │  - flush()                            │
│  - Lifecycle observer        │        │  - TrackerState, ScrollDirectionDetector│
│  - ON_PAUSE → flush          │        │  - VisibilityCalculator, EventDispatcher│
└──────────────────────────────┘        └──────────────────────────────────────┘
                                                          │
                        ┌─────────────────┬───────────────┼───────────────┐
                        ▼                 ▼               ▼               ▼
            ┌───────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐
            │ TrackerState  │  │ScrollDir     │  │ Visibility  │  │   Event      │
            │ - activeSess  │  │ Detector     │  │ Calculator  │  │ Dispatcher   │
            │ - totalSeen   │  │ - direction  │  │ - isVisible │  │ - dispatch   │
            │ - firstSeen   │  │              │  │             │  │   EndSession │
            │ - dispatched  │  │              │  │             │  │              │
            └───────────────┘  └──────────────┘  └─────────────┘  └──────────────┘
```

---

## 6. Data Flow Summary

| Stage        | Input                          | Output                          |
|-------------|---------------------------------|----------------------------------|
| Init        | `onEvent`, `VisibilityConfig`   | `Cozy` instance, `LazyListState` |
| Layout      | `LazyListLayoutInfo`           | `snapshotFlow` emission          |
| Debounce    | Flow emission                  | Single `layoutInfo` after idle   |
| Process     | `layoutInfo`                   | `VisibilityEvent`s via `onEvent` |
| End Session | `key`, `SessionData`           | `VisibilityEvent` (if minDwell)  |
| Flush       | Lifecycle / Dispose            | End all sessions → events        |
