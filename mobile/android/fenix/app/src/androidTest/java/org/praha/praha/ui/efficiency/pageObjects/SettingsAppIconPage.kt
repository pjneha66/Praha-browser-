/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestHelper
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.helpers.PageStateTracker
import org.praha.praha.ui.efficiency.helpers.Selector
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationOptions
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.SettingsAppIconSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsCustomizeSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsAppIconPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsAppIconPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "SettingsCustomizePage",
            to = pageName,
            steps = listOf(NavigationStep.Click(SettingsCustomizeSelectors.SELECT_APP_ICON_TITLE)),
        )
        builder.register(
            from = pageName,
            to = "SettingsCustomizePage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )
    }

    override fun navigateToPage(
        url: String,
        forceNavigation: Boolean,
        navigationOptions: NavigationOptions,
    ): SettingsAppIconPage {
        super.navigateToPage(url, forceNavigation, navigationOptions)
        return this
    }

    fun clickAppIconOption(selector: Selector): SettingsAppIconPage {
        mozClick(selector)
        return this
    }

    fun clickChangeIconButton(): SettingsAppIconPage {
        mozClick(SettingsAppIconSelectors.CHANGE_ICON_DIALOG_CHANGE_BUTTON)
        return this
    }

    fun restartApp(): SettingsAppIconPage {
        TestHelper.restartApp(composeRule.activityRule)
        // A restart puts the app back at its entry point, so navigation's breadcrumb has to go
        // with it or every later step reasons from a page that is no longer on screen.
        PageStateTracker.reset()
        return this
    }

    override val selectorCatalog = SettingsAppIconSelectors
}
