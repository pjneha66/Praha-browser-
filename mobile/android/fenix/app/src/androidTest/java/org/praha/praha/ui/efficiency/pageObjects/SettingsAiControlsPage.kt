/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.SettingsAiControlsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsAiControlsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsAiControlsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )
    }

    override val selectorCatalog = SettingsAiControlsSelectors
}
