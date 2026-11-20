/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.selectors

import org.praha.praha.R
import org.praha.praha.helpers.DataGenerationHelper.getStringResource
import org.praha.praha.helpers.TestHelper.appName
import org.praha.praha.ui.efficiency.helpers.Selector
import org.praha.praha.ui.efficiency.helpers.SelectorContainer
import org.praha.praha.ui.efficiency.helpers.SelectorStrategy

object LauncherSelectors : SelectorContainer {

    val FIREFOX_APP_ICON =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR2_BY_TEXT,
            value = appName,
            description = "Firefox app icon on the device launcher",
        )

    val NEW_TAB_SHORTCUT =
        Selector(
            strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
            value = getStringResource(R.string.home_screen_shortcut_open_new_tab_2),
            description = "New tab app shortcut in the launcher popup",
        )
}
