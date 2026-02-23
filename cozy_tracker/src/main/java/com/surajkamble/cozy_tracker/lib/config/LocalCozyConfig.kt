package com.surajkamble.cozy_tracker.lib.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A [androidx.compose.runtime.CompositionLocal] for providing a default [CozyConfig] throughout the Composition tree.
 */
internal val LocalCozyConfig = staticCompositionLocalOf {
    CozyConfig()
}

/**
 * Internal implementation for providing a [CozyConfig] to the Composition tree.
 */
@Composable
internal fun ProvideCozyConfig(
    config: CozyConfig = CozyConfig(),
    content: @Composable () -> Unit
) {
    val rememberedConfig = remember(config) { config }
    CompositionLocalProvider(LocalCozyConfig provides rememberedConfig, content = content)
}
