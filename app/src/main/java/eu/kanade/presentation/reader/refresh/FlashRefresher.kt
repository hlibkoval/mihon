package eu.kanade.presentation.reader.refresh

import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Covers the reader with a solid color for a moment, which works on any display. */
@Stable
class FlashRefresher(private val readerPreferences: ReaderPreferences) : DisplayRefresher {

    override val name = "Flash"

    private var color by mutableStateOf<Color?>(null)

    override suspend fun refresh(window: View): Boolean {
        val half = readerPreferences.flashDurationMillis.get().milliseconds / 2
        val flashColor = readerPreferences.flashColor.get()
        try {
            color = if (flashColor == ReaderPreferences.FlashColor.BLACK) Color.Black else Color.White
            delay(half)
            if (flashColor == ReaderPreferences.FlashColor.WHITE_BLACK) {
                color = Color.Black
            }
            delay(half)
        } finally {
            color = null
        }
        return true
    }

    @Composable
    override fun Overlay(modifier: Modifier) {
        Canvas(modifier = modifier.fillMaxSize()) {
            color?.let { drawRect(it) }
        }
    }
}
