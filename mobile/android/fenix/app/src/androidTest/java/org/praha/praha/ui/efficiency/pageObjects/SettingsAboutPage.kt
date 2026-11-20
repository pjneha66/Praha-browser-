/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.selectors.SettingsAboutSelectors

class SettingsAboutPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsAboutPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        //        builder.register(
        //            from = "HomePage",
        //            to = pageName,
        //            steps = listOf(
        //                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
        //                NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
        //                NavigationStep.Swipe(SettingsSelectors.DATA_COLLECTION_BUTTON),
        //                NavigationStep.Click(SettingsSelectors.DATA_COLLECTION_BUTTON),
        //            ),
        //        )
    }

    override val selectorCatalog = SettingsAboutSelectors
}
