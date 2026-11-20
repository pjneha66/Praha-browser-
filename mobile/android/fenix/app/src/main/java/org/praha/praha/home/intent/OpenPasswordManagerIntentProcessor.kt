/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.intent

import android.content.Intent
import androidx.navigation.NavController
import org.praha.praha.HomeActivity
import org.praha.praha.NavGraphDirections
import org.praha.praha.ext.nav
import org.praha.praha.utils.Settings

/** When the open password manager shortcut is tapped, Fenix should open to the logins list fragment. */
class OpenPasswordManagerIntentProcessor : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent, settings: Settings): Boolean {
        return if (intent.extras?.getBoolean(HomeActivity.OPEN_PASSWORD_MANAGER) == true) {
            out.removeExtra(HomeActivity.OPEN_PASSWORD_MANAGER)

            // The shortcut is declared statically in the manifest and cannot be removed at runtime, so devices without
            // password management support are simply left on the homepage.
            if (settings.isAutofillSupported) {
                val directions = NavGraphDirections.actionLoginsListFragment()
                navController.nav(null, directions)
            }
            true
        } else {
            false
        }
    }
}
