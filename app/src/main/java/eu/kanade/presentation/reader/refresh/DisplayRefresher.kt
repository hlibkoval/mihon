package eu.kanade.presentation.reader.refresh

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Clears e-ink ghosting after the reader changes page. */
interface DisplayRefresher {

    /** Refreshes the screen showing [window], the reader's root view, suspending until it's done. */
    suspend fun refresh(window: View)

    /** Drawn over the reader, for refreshers that draw the refresh themselves. */
    @Composable
    fun Overlay(modifier: Modifier) {}
}
