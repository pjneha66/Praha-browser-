/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.test.espresso.Espresso.closeSoftKeyboard
import mozilla.components.feature.prompts.R as promptsR
import org.junit.Rule
import org.junit.Test
import org.praha.praha.helpers.DataGenerationHelper.getStringResource
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityTestRule
import org.praha.praha.helpers.MatcherHelper
import org.praha.praha.helpers.MatcherHelper.itemContainingText
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestAssetHelper.promptAsset
import org.praha.praha.helpers.TestHelper.waitForAppWindowToBeUpdated
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.clickPageObject
import org.praha.praha.ui.robots.navigationToolbar

/**
 * Tests for verifying basic functionality of prompts
 *
 * Including:
 * - beforeunload prompt
 */
class PromptTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule = AndroidComposeTestRuleV2(HomeActivityTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4025097
    @Test
    fun verifyBeforeUnloadPrompt() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)
        val promptWebPage = mockWebServer.promptAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(promptWebPage.url) {
                clickPageObject(composeTestRule, MatcherHelper.itemWithResId("nameInput"))
            }

        navigationToolbar(composeTestRule) {
                closeSoftKeyboard()
                waitForAppWindowToBeUpdated()
            }
            .enterURLAndEnterToBrowser(defaultWebPage.url) {
                verifyBeforeUnloadPromptExists()
            }
    }
}

private fun verifyBeforeUnloadPromptExists() =
    MatcherHelper.assertUIObjectExists(
        itemContainingText(getStringResource(promptsR.string.mozac_feature_prompt_before_unload_dialog_body))
    )
