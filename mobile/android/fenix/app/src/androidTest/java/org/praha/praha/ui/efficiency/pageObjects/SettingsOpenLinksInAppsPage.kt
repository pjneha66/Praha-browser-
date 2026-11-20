/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.HomeSelectors
import org.praha.praha.ui.efficiency.selectors.MainMenuSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsOpenLinksInAppsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsOpenLinksInAppsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsOpenLinksInAppsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                    NavigationStep.Swipe(SettingsSelectors.OPEN_LINKS_IN_APPS_BUTTON),
                    NavigationStep.Click(SettingsSelectors.OPEN_LINKS_IN_APPS_BUTTON),
                ),
        )
    }

    override val selectorCatalog = SettingsOpenLinksInAppsSelectors
}
