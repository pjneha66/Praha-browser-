/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationArrival
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationOptions
import org.praha.praha.ui.efficiency.selectors.AddToHomeScreenSelectors

class AddToHomeScreenComponent(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) :
    BasePage(composeRule) {
    override val pageName = "AddToHomeScreenComponent"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = pageName,
            to = "BrowserPage",
            steps = listOf(),
            arrival = NavigationArrival.EDGE_COMPLETION,
        )
    }

    override fun navigateToPage(
        url: String,
        forceNavigation: Boolean,
        navigationOptions: NavigationOptions,
    ): AddToHomeScreenComponent {
        super.navigateToPage(
            url = url.ifBlank { "example.com" },
            forceNavigation = forceNavigation,
            navigationOptions = navigationOptions,
        )
        return this
    }

    override val selectorCatalog = AddToHomeScreenSelectors
}
