/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.SettingsHomepageSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsHomepagePage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsHomepagePage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )

        // Depth-independent: this screen can be reached one level deep (home menu -> Customize homepage) or two
        // levels deep (Settings -> Homepage), so back out until the settings toolbar is gone rather than pressing
        // back a fixed number of times.
        builder.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.PressBackUntilGone(SettingsSelectors.NAVIGATION_TOOLBAR)),
        )
    }

    override val selectorCatalog = SettingsHomepageSelectors
}
