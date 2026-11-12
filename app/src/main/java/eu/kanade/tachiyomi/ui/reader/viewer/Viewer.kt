package eu.kanade.tachiyomi.ui.reader.viewer

import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Interface for implementing a viewer.
 */
interface Viewer {

    /**
     * Draws this viewer.
     */
    @Composable
    fun Content(modifier: Modifier)

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    fun destroy() {}

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    fun setChapters(chapters: ViewerChapters)

    /**
     * Tells this viewer to move to the given [page].
     */
    fun moveToPage(page: ReaderPage)

    /**
     * Moves forward through the pages.
     */
    fun moveForward()

    /**
     * Moves backward through the pages.
     */
    fun moveBackward()

    /**
     * Suspends until the page the reader just changed to is fully drawn in [window], the reader's root view, so
     * an e-ink refresh lands on it rather than on an earlier frame. Waits for the window to stop drawing, which
     * covers page transitions, loading pages and tiles that sharpen after the first frame.
     */
    suspend fun awaitPageDrawn(window: View) {
        window.awaitDrawsSettled()
    }
}

/** Gives up after [DRAWS_TIMEOUT], as an animated page never stops drawing. */
private suspend fun View.awaitDrawsSettled() {
    val observer = viewTreeObserver
    var lastDraw = SystemClock.uptimeMillis()
    val listener = ViewTreeObserver.OnDrawListener { lastDraw = SystemClock.uptimeMillis() }
    observer.addOnDrawListener(listener)
    try {
        withTimeoutOrNull(DRAWS_TIMEOUT) {
            while (true) {
                val quietFor = (SystemClock.uptimeMillis() - lastDraw).milliseconds
                if (quietFor >= DRAWS_QUIET) break
                delay(DRAWS_QUIET - quietFor)
            }
        }
    } finally {
        if (observer.isAlive) observer.removeOnDrawListener(listener)
    }
}

// Long enough to span the gap while full resolution tiles decode after the first frame
private val DRAWS_QUIET = 100.milliseconds
private val DRAWS_TIMEOUT = 2.seconds

/**
 * The reader's grayscale and inverted colours, for page images only: transition, loading and error pages keep
 * theirs. Null when both are off.
 */
val LocalPageColorFilter = compositionLocalOf<ColorFilter?> { null }

/**
 * Turns pages with the mouse wheel instead of letting the content scroll.
 */
fun Modifier.mouseWheelPaging(viewer: Viewer): Modifier = pointerInput(viewer) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue
            val delta = event.changes.first().scrollDelta.y
            when {
                delta > 0 -> viewer.moveForward()
                delta < 0 -> viewer.moveBackward()
                else -> continue
            }
            event.changes.forEach { it.consume() }
        }
    }
}
