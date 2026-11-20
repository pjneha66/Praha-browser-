/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestAssetHelper.TestAsset
import org.praha.praha.helpers.TestAssetHelper.gcpTestAsset
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.navigationToolbar

/** Tests for Global Privacy Control setting. */
class GlobalPrivacyControlTest {
    private lateinit var gpcPage: TestAsset

    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityIntentTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    @Before
    fun setUp() {
        gpcPage = mockWebServer.gcpTestAsset
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2429327
    @Converted(
        replacedBy =
            [
                "org.praha.praha.ui.efficiency.tests.GlobalPrivacyControlTest#verifyGlobalPrivacyControlInNormalBrowsingTest"
            ],
        bug = 2087418,
        since = "2026-10",
    )
    @Critical
    @Test
    fun testGPCinNormalBrowsing() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openEnhancedTrackingProtectionSubMenu {
                scrollToGCPSettings()
                verifyGPCTextWithSwitchWidget()
                verifyGPCSwitchEnabled(false)
            }
            .goBackToHomeScreen(composeTestRule) {}

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(gpcPage.url) {
                verifyPageContent("GPC not enabled.")
            }
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openEnhancedTrackingProtectionSubMenu {
                scrollToGCPSettings()
                switchGPCToggle()
            }
            .goBackToHomeScreen(composeTestRule) {}

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(gpcPage.url) {
                verifyPageContent("GPC is enabled.")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2429364
    @Test
    fun testGPCinPrivateBrowsing() {
        homeScreen(composeTestRule) {}.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(gpcPage.url) {
                verifyPageContent("GPC is enabled.")
            }
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openEnhancedTrackingProtectionSubMenu {
                scrollToGCPSettings()
                verifyGPCTextWithSwitchWidget()
                verifyGPCSwitchEnabled(false)
                switchGPCToggle()
            }
            .goBack {}
            .goBackToBrowser(composeTestRule) {
                verifyPageContent("GPC is enabled.")
            }
    }
}
