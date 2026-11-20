/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.AppAndSystemHelper.enableOrDisableBackGestureNavigationOnDevice
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.SearchMockServerRule
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.navigationToolbar

/**
 * Tests for verifying basic functionality of browser navigation and page related interactions
 *
 * Including:
 * - Visiting a URL
 * - Back and Forward navigation
 * - Refresh
 * - Find in page
 */
class NavigationToolbarTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                isWallpaperOnboardingEnabled = false,
                isOpenInAppBannerEnabled = false,
                isMicrosurveyEnabled = false,
                isTermsOfServiceAccepted = true,
            )
        ) {
            it.activity
        }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    @get:Rule val searchMockServerRule = SearchMockServerRule()

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/987326
    // Swipes the nav bar left/right to switch between tabs
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.NavigationToolbarTest#swipeToSwitchTabTest"],
        bug = 2061612,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun swipeToSwitchTabTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        // Disable the back gesture from the edge of the screen on the device.
        enableOrDisableBackGestureNavigationOnDevice(backGestureNavigationEnabled = false)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(firstWebPage.url) {}
            .openTabDrawer(composeTestRule) {}
            .openNewTab {}
            .submitQuery(secondWebPage.url.toString()) {
                swipeNavBarRight(secondWebPage.url.toString())
                verifyUrl(firstWebPage.url.toString())
                swipeNavBarLeft(firstWebPage.url.toString())
                verifyUrl(secondWebPage.url.toString())
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3135066
    @Test
    fun verifyTheToolbarItemsTest() {
        navigationToolbar(composeTestRule) {
            verifyDefaultSearchEngine("Google")
            verifySearchBarPlaceholder("Search or enter address")
            verifyTheTabCounter("0")
            verifyTheMainMenuButton()
        }
        homeScreen(composeTestRule) {}.togglePrivateBrowsingMode()
        navigationToolbar(composeTestRule) {
            verifyDefaultSearchEngine("Google")
            verifySearchBarPlaceholder("Search or enter address")
            verifyTheTabCounter("0", isPrivateBrowsingEnabled = true)
            verifyTheMainMenuButton()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3135067
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.NavigationToolbarTest#verifyTheNewTabButtonTest"],
        bug = 2061612,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyTheNewTabButtonTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(firstPage.url) {
                verifyTabCounter("1")
            }
        navigationToolbar(composeTestRule) {
                verifyTheNewTabButton()
            }
            .clickTheNewTabButton {}
            .submitQuery(secondPage.url.toString()) {
                verifyTabCounter("2")
            }
            .goToHomescreen {}
            .togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(firstPage.url) {
                verifyTabCounter("1", isPrivateBrowsingEnabled = true)
            }
        navigationToolbar(composeTestRule) {
                verifyTheNewTabButton(isPrivateModeEnabled = true)
            }
            .clickTheNewTabButton(isPrivateModeEnabled = true) {}
            .submitQuery(secondPage.url.toString()) {
                verifyTabCounter("2", isPrivateBrowsingEnabled = true)
            }
    }
}
