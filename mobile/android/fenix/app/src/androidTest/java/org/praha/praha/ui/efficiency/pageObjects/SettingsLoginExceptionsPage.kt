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
import org.praha.praha.ui.efficiency.selectors.SettingsLoginExceptionsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsPasswordsSelectors
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors

class SettingsLoginExceptionsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "SettingsLoginExceptionsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        // "Exceptions" is a one-tap option on the Passwords settings screen.
        builder.register(
            from = "SettingsPasswordsPage",
            to = pageName,
            steps = listOf(NavigationStep.Click(SettingsPasswordsSelectors.EXCEPTIONS_OPTION)),
        )

        // Standalone entry from Home for reachability coverage.
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                    NavigationStep.Click(SettingsSelectors.PASSWORDS_BUTTON),
                    NavigationStep.Click(SettingsPasswordsSelectors.EXCEPTIONS_OPTION),
                ),
        )

        // A single "Navigate up" returns from the exceptions list to the Passwords settings screen.
        builder.register(
            from = pageName,
            to = "SettingsPasswordsPage",
            steps = listOf(NavigationStep.Click(SettingsPasswordsSelectors.GO_BACK_BUTTON)),
        )
    }

    override val selectorCatalog = SettingsLoginExceptionsSelectors

    override fun readinessContract() =
        super.readinessContract()
            .withRule(
                PageReadinessRule(
                    name = "login-exceptions-content-state",
                    profiles = PageReadinessProfiles.IDENTITY_ANCHOR,
                    condition =
                        PageReadinessCondition.anyOf(
                            SettingsLoginExceptionsSelectors.EMPTY_LOGIN_EXCEPTIONS_LIST,
                            SettingsLoginExceptionsSelectors.LOGIN_EXCEPTIONS_LIST,
                        ),
                )
            )
}
