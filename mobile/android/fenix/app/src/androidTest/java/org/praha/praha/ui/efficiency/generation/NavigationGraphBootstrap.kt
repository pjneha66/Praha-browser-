/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.generation

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.PageContext
import org.praha.praha.ui.efficiency.navigation.NavigationGraph

/** Builds an immutable planning graph without launching an Activity. Runtime tests use their own PageContext graph. */
object NavigationGraphBootstrap {
    fun buildGraph(): NavigationGraph {
        val composeRule =
            AndroidComposeTestRuleV2(
                HomeActivityIntentTestRule(
                    skipOnboarding = true,
                    isPageLoadTranslationsPromptEnabled = false,
                )
            ) {
                it.activity
            }

        return PageContext(composeRule).navigationGraph
    }
}
