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
import org.praha.praha.ui.efficiency.selectors.SettingsPasswordsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSavePasswordsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsSavePasswordsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsSavePasswordsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                    NavigationStep.Click(SettingsSelectors.PASSWORDS_BUTTON),
                    NavigationStep.Click(SettingsPasswordsSelectors.SAVE_PASSWORDS_OPTION),
                ),
        )

        // Back out of settings to Home (each "Navigate up" pops one level: save-passwords -> passwords ->
        // settings -> home). Lets tests that set a passwords option then load a page route home -> browser.
        builder.register(
            from = pageName,
            to = "HomePage",
            steps =
                listOf(
                    NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
                    NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
                    NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
                ),
        )
    }

    fun clickNeverSaveOption(): SettingsSavePasswordsPage {
        mozClick(SettingsSavePasswordsSelectors.NEVER_SAVE_OPTION)
        return this
    }

    override val selectorCatalog = SettingsSavePasswordsSelectors
}
