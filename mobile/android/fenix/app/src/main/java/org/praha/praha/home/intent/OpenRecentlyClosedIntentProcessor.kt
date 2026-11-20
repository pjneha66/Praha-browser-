/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.intent

import android.content.Intent
import androidx.navigation.NavController
import org.praha.praha.NavGraphDirections
import org.praha.praha.ext.nav
import org.praha.praha.utils.Settings

/** Opens the "recently closed tabs" fragment when the user taps on a "synced tabs closed" notification. */
class OpenRecentlyClosedIntentProcessor : HomeIntentProcessor {
    override fun process(intent: Intent, navController: NavController, out: Intent, settings: Settings): Boolean {
        return if (intent.action == ACTION_OPEN_RECENTLY_CLOSED) {
            val directions = NavGraphDirections.actionGlobalRecentlyClosed()
            navController.nav(null, directions)
            true
        } else {
            false
        }
    }

    companion object {
        const val ACTION_OPEN_RECENTLY_CLOSED = "org.praha.praha.open_recently_closed"
    }
}
