package eu.kanade.presentation.reader.refresh

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Clears e-ink ghosting after the reader changes page. A display vendor's own full refresh is a [Vendor] in
 * [vendors]; [FlashRefresher] is the fallback that works on any display.
 */
interface DisplayRefresher {

    /** What settings call this refresh, like a display vendor's brand. */
    val name: String

    /**
     * Refreshes the screen showing [window], the reader's root view, once the new page is on screen.
     * Returns false if the refresh failed, so the reader can flash instead.
     */
    suspend fun refresh(window: View): Boolean

    /** Drawn over the reader, for refreshers that draw the refresh themselves. */
    @Composable
    fun Overlay(modifier: Modifier) {}

    /** Detects one vendor's display. */
    fun interface Vendor {
        /** This vendor's refresher, or null if the device isn't theirs or lacks the API. */
        fun create(): DisplayRefresher?
    }

    companion object {
        private val vendors: List<Vendor> = listOf(BooxRefresher)

        /** The display's own full refresh, or null if Mihon doesn't support it on this device. */
        val hardware: DisplayRefresher? by lazy { vendors.firstNotNullOfOrNull { it.create() } }
    }
}
