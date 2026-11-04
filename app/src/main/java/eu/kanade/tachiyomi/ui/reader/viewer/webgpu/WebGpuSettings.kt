package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TileUpdateMode

/**
 * Settings both WebGPU viewers use, kept in each of their own settings.
 */
data class WebGpuSettings(
    val doubleTapGesturesDisabled: Boolean,
    val tileUpdateMode: TileUpdateMode,
    val imageFilters: List<WebGpuShader>,
    val tileFilters: List<WebGpuShader>,
)
