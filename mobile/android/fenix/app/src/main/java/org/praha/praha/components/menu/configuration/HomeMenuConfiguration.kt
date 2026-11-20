/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components.menu.configuration

import androidx.annotation.VisibleForTesting
import org.praha.praha.components.menu.configuration.FenixMenuItem.Bookmarks
import org.praha.praha.components.menu.configuration.FenixMenuItem.CustomizeHomepage
import org.praha.praha.components.menu.configuration.FenixMenuItem.DefaultBrowserBanner
import org.praha.praha.components.menu.configuration.FenixMenuItem.Downloads
import org.praha.praha.components.menu.configuration.FenixMenuItem.Extensions
import org.praha.praha.components.menu.configuration.FenixMenuItem.History
import org.praha.praha.components.menu.configuration.FenixMenuItem.IPProtection
import org.praha.praha.components.menu.configuration.FenixMenuItem.MozillaAccount
import org.praha.praha.components.menu.configuration.FenixMenuItem.Passwords
import org.praha.praha.components.menu.configuration.FenixMenuItem.Quit
import org.praha.praha.components.menu.configuration.FenixMenuItem.Settings
import org.praha.praha.components.menu.configuration.MenuPresentationMode.Grid
import org.praha.praha.components.menu.configuration.MenuPresentationMode.Row

/** The sections of the menu shown on the home screen. */
object HomeMenuConfiguration {
    @VisibleForTesting internal val PROMO_BANNER_GROUP_ID = "promo_banner"
    @VisibleForTesting internal val VPN_GROUP_ID = "vpn"
    @VisibleForTesting internal val EXTENSIONS_GROUP_ID = "extensions"
    @VisibleForTesting internal val LIBRARY_GROUP_ID = "library"
    @VisibleForTesting internal val ACCOUNTS_AND_SETTINGS_GROUP_ID = "accounts_and_settings"
    @VisibleForTesting internal val QUIT_GROUP_ID = "quit"

    /** The menu shown on the home screen. */
    fun sections(): List<MenuSectionConfiguration> =
        listOf(
            MenuSectionConfiguration(
                id = PROMO_BANNER_GROUP_ID,
                presentationMode = Row,
                items = listOf(DefaultBrowserBanner),
            ),
            MenuSectionConfiguration(
                id = VPN_GROUP_ID,
                presentationMode = Row,
                items = listOf(IPProtection),
            ),
            MenuSectionConfiguration(
                id = EXTENSIONS_GROUP_ID,
                presentationMode = Row,
                items = listOf(Extensions),
            ),
            MenuSectionConfiguration(
                id = LIBRARY_GROUP_ID,
                presentationMode = Grid,
                items = listOf(History, Bookmarks, Downloads, Passwords),
            ),
            MenuSectionConfiguration(
                id = ACCOUNTS_AND_SETTINGS_GROUP_ID,
                presentationMode = Row,
                items = listOf(MozillaAccount, CustomizeHomepage, Settings),
            ),
            MenuSectionConfiguration(
                id = QUIT_GROUP_ID,
                presentationMode = Row,
                items = listOf(Quit),
            ),
        )
}
