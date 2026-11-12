package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.reader.refresh.FlashRefresher
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.util.collectAsState

@Stable
class DisplayRefreshHost(readerPreferences: ReaderPreferences) {

    internal val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    internal val flashRefresher = FlashRefresher(readerPreferences)

    internal val flashIntervalPref = readerPreferences.flashPageInterval

    // Internal State for Flash
    private var flashInterval = flashIntervalPref.get()
    private var timesCalled = 0

    fun flash() {
        if (timesCalled % flashInterval == 0) {
            requests.tryEmit(Unit)
        }
        timesCalled += 1
    }

    fun setInterval(interval: Int) {
        flashInterval = interval
        timesCalled = 0
    }
}

@Composable
fun DisplayRefreshHost(
    hostState: DisplayRefreshHost,
    modifier: Modifier = Modifier,
) {
    val flashInterval by hostState.flashIntervalPref.collectAsState()
    val view = LocalView.current

    LaunchedEffect(hostState) {
        // Latest, so turning pages quickly refreshes once, after the last one
        hostState.requests.collectLatest {
            hostState.flashRefresher.refresh(view.rootView)
        }
    }

    LaunchedEffect(flashInterval) {
        hostState.setInterval(flashInterval)
    }

    hostState.flashRefresher.Overlay(modifier)
}
