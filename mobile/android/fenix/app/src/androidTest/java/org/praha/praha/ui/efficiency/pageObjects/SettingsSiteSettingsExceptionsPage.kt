/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.helpers.PageReadinessCondition
import org.praha.praha.ui.efficiency.helpers.PageReadinessProfiles
import org.praha.praha.ui.efficiency.helpers.PageReadinessRule
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.HomeSelectors
import org.praha.praha.ui.efficiency.selectors.MainMenuSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSiteSettingsExceptionsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSiteSettingsSelectors

class SettingsSiteSettingsExceptionsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsSiteSettingsExceptionsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                    NavigationStep.Swipe(SettingsSelectors.SITE_SETTINGS_BUTTON),
                    NavigationStep.Click(SettingsSelectors.SITE_SETTINGS_BUTTON),
                    NavigationStep.Swipe(SettingsSiteSettingsSelectors.EXCEPTIONS_BUTTON),
                    NavigationStep.Click(SettingsSiteSettingsSelectors.EXCEPTIONS_BUTTON),
                ),
        )
    }

    override val selectorCatalog = SettingsSiteSettingsExceptionsSelectors

    override fun readinessContract() =
        super.readinessContract()
            .withRule(
                PageReadinessRule(
                    name = "site-exceptions-content-state",
                    profiles = PageReadinessProfiles.IDENTITY_ANCHOR,
                    condition =
                        PageReadinessCondition.anyOf(
                            SettingsSiteSettingsExceptionsSelectors.EMPTY_EXCEPTIONS_LIST,
                            SettingsSiteSettingsExceptionsSelectors.EXCEPTIONS_LIST,
                        ),
                )
            )
}
