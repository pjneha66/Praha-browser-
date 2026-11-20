/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.selectors

import org.praha.praha.R
import org.praha.praha.helpers.DataGenerationHelper.getStringResource
import org.praha.praha.ui.efficiency.helpers.SelectorContainer

object SettingsOpenLinksInAppsSelectors : SelectorContainer {

    val TOOLBAR_TITLE =
        navigationToolbarTitle(
            title = getStringResource(R.string.preferences_open_links_in_apps),
            description = "Open links in apps toolbar title",
        )
}
