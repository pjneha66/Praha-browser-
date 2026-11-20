/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestAssetHelper.waitingTime
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.helpers.PageStateTracker
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.BrowserPageSelectors
import org.praha.praha.ui.efficiency.selectors.HistorySelectors
import org.praha.praha.ui.efficiency.selectors.HomeSelectors
import org.praha.praha.ui.efficiency.selectors.MainMenuSelectors

class HistoryPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "HistoryPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.HISTORY_BUTTON),
                ),
        )

        builder.register(
            from = "MainMenuPage",
            to = pageName,
            steps = listOf(NavigationStep.Click(MainMenuSelectors.HISTORY_BUTTON)),
        )

        builder.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.PressBack),
        )
    }

    override val selectorCatalog = HistorySelectors

    /**
     * Open the history row whose URL or title contains [text] by tapping it; the tapped entry loads in the browser.
     * Waits for the engine view and records the move to BrowserPage before returning, because a bare mozClick leaves
     * the navigation layer still believing it is on HistoryPage -- the next on.browserPage navigation would then try to
     * route out of a screen that is already gone and fail its readiness check. The caller should continue on
     * `on.browserPage`.
     */
    fun openHistoryItem(text: String) {
        mozClick(HistorySelectors.HISTORY_ITEM_WITH_TEXT(text))
        mozVerify(BrowserPageSelectors.ENGINE_VIEW, timeout = waitingTime)
        PageStateTracker.currentPageName = "BrowserPage"
    }
}
