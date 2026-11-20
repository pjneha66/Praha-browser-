/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.AppAndSystemHelper.clickSystemHomeScreenShortcutAddButton
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityTestRule
import org.praha.praha.helpers.MatcherHelper.itemContainingText
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.clickPageObject
import org.praha.praha.ui.robots.customTabScreen
import org.praha.praha.ui.robots.navigationToolbar
import org.praha.praha.ui.robots.pwaScreen

class PwaTest {
    /* Updated externalLinks.html to v2.0,
      changed the hypertext reference to mozilla-mobile.github.io/testapp/downloads for "External link"
    */
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val externalLinksPWAPage = "https://mozilla-mobile.github.io/testapp/v2.0/externalLinks.html"
    private val shortcutTitle = "TEST_APP"

    @get:Rule(order = 1)
    val composeTestRule = AndroidComposeTestRuleV2(HomeActivityTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/845695
    @Test
    fun externalLinkPWATest() {
        val externalLinkURL = "https://mozilla-mobile.github.io/testapp/downloads"

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
                verifyPageContent("Misc Link Types")
            }
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddAppToHomeScreenButton {
                clickSystemHomeScreenShortcutAddButton()
            }
            .openHomeScreenShortcut(shortcutTitle) {
                clickPageObject(composeTestRule, itemContainingText("External link"))
            }

        customTabScreen(composeTestRule) {
            verifyCustomTabToolbarTitle(externalLinkURL)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/845694
    @Test
    fun appLikeExperiencePWATest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
                verifyPageContent("Misc Link Types")
            }
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddAppToHomeScreenButton {
                clickSystemHomeScreenShortcutAddButton()
            }
            .openHomeScreenShortcut(shortcutTitle) {}

        pwaScreen {
            verifyCustomTabToolbarIsNotDisplayed()
            verifyPwaActivityInCurrentTask()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/834200
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.PwaTest#installPWAFromTheMainMenuTest"],
        bug = 2060905,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun installPWAFromTheMainMenuTest() {
        val pwaPage = "https://mozilla-mobile.github.io/testapp/loginForm"

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pwaPage.toUri()) {
                waitForPageToLoad()
                verifyUrl("mozilla-mobile.github.io/testapp/loginForm")
                verifyPageContent("Login Form")
            }
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddAppToHomeScreenButton {
                clickSystemHomeScreenShortcutAddButton()
            }
            .openHomeScreenShortcut("TEST_APP") {
                mDevice.waitForIdle()
                verifyNavURLBarHidden()
            }
    }
}
