/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.LaunchConfig
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.HomeSelectors
import org.praha.praha.ui.efficiency.selectors.StoriesSelectors

class StoriesPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {

    override val pageName = "StoriesPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        // The stories entry point only renders when Pocket is on, so the reachability suite must launch HomePage
        // with it for STORIES_BUTTON to exist.
        builder.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(NavigationStep.Click(HomeSelectors.STORIES_BUTTON)),
            launch = LaunchConfig(isPocketEnabled = true),
        )

        builder.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.Click(StoriesSelectors.BACK_BUTTON)),
        )
    }

    override val selectorCatalog = StoriesSelectors
}
