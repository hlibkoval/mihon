package eu.kanade.presentation.reader.refresh

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View

/**
 * The full (GC) refresh of Onyx BOOX displays, the one the system's refresh button runs. It's requested with a
 * broadcast that SystemUI handles, because the SDK's refresh methods are blocked for apps on current firmware
 * and `View.refreshScreen` only does a partial update, which leaves faint ghosting. It refreshes the panel
 * rather than redrawing views, so it also covers renderers that draw to their own surface.
 */
class BooxRefresher private constructor() : DisplayRefresher {

    override val name = "Onyx BOOX"

    override suspend fun refresh(window: View): Boolean {
        window.context.sendBroadcast(Intent(ACTION_REFRESH_SCREEN))
        return true
    }

    companion object : DisplayRefresher.Vendor {
        // android.onyx.BroadcastHelper.ONYX_REFRESH_SCREEN_ACTION
        private const val ACTION_REFRESH_SCREEN = "onyx.android.intent.action.REFRESH_SCREEN"

        @SuppressLint("PrivateApi")
        override fun create(): BooxRefresher? {
            return try {
                Class.forName("android.onyx.BroadcastHelper")
                BooxRefresher()
            } catch (_: ClassNotFoundException) {
                null
            }
        }
    }
}
