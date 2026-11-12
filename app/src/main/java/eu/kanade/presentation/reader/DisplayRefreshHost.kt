package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.reader.refresh.DisplayRefresher
import eu.kanade.presentation.reader.refresh.FlashRefresher
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Duration.Companion.milliseconds

@Stable
class DisplayRefreshHost(readerPreferences: ReaderPreferences) {

    internal val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    internal val flashRefresher = FlashRefresher(readerPreferences)
    internal val hardwareRefreshPref = readerPreferences.hardwareRefresh
    internal val hardwareRefreshTwicePref = readerPreferences.hardwareRefreshTwice

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
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(hostState) {
        // Latest, so turning pages quickly refreshes once, after the last one
        hostState.requests.collectLatest {
            val hardware = DisplayRefresher.hardware?.takeIf { hostState.hardwareRefreshPref.get() }
            if (hardware != null) {
                // The page change lands before the new page is fully on screen
                delay(HARDWARE_REFRESH_DELAY)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@collectLatest
                if (hardware.refresh(view.rootView)) {
                    if (hostState.hardwareRefreshTwicePref.get()) {
                        // One pass can leave faint ghosting
                        delay(SECOND_PASS_DELAY)
                        hardware.refresh(view.rootView)
                    }
                    return@collectLatest
                }
            }
            hostState.flashRefresher.refresh(view.rootView)
        }
    }

    LaunchedEffect(flashInterval) {
        hostState.setInterval(flashInterval)
    }

    DisplayRefresher.hardware?.Overlay(modifier)
    hostState.flashRefresher.Overlay(modifier)
}

private val HARDWARE_REFRESH_DELAY = 500.milliseconds

private val SECOND_PASS_DELAY = 250.milliseconds
