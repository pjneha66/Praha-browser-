/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.helpers.AppAndSystemHelper
import org.praha.praha.helpers.AppAndSystemHelper.clickSystemHomeScreenShortcutAddButton
import org.praha.praha.helpers.DataGenerationHelper
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.TestHelper.restartApp
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.addToHomeScreen
import org.praha.praha.ui.robots.browserScreen
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.navigationToolbar

class SettingsPrivateBrowsingTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    private val pageShortcutName = DataGenerationHelper.generateRandomString(5)

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityIntentTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/555822
    @Converted(
        replacedBy =
            ["org.praha.praha.ui.efficiency.tests.SettingsPrivateBrowsingTest#verifyPrivateBrowsingMenuItemsTest"],
        bug = 2071878,
        since = "2026-09",
    )
    @Critical
    @Test
    fun verifyPrivateBrowsingMenuItemsTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                verifyAddPrivateBrowsingShortcutButton()
                verifyOpenLinksInPrivateTab()
                verifyOpenLinksInPrivateTabOff()
            }
            .goBack {
                verifySettingsView()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/420086
    @Test
    fun launchLinksInAPrivateTabTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                verifyOpenLinksInPrivateTabEnabled()
                clickOpenLinksInPrivateTabSwitch()
            }
            .goBack {}
            .goBack(composeTestRule) {}

        homeScreen(composeTestRule) {
            verifyHomeComponent()
        }

        AppAndSystemHelper.openAppFromExternalLink(composeTestRule, firstWebPage.url.toString())

        browserScreen(composeTestRule) {
                verifyUrl(firstWebPage.url.toString())
            }
            .openTabDrawer(composeTestRule) {
                verifyPrivateBrowsingButtonIsSelected()
            }
            .closeTabDrawer {}
            .goToHomescreen(isPrivateModeEnabled = true) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                clickOpenLinksInPrivateTabSwitch()
                verifyOpenLinksInPrivateTabOff()
            }
            .goBack {}
            .goBack(composeTestRule) {
                verifyHomeComponent()
            }

        // We need to open a different link, otherwise it will open the same session
        AppAndSystemHelper.openAppFromExternalLink(composeTestRule, secondWebPage.url.toString())

        browserScreen(composeTestRule) {
                verifyUrl(secondWebPage.url.toString())
            }
            .openTabDrawer(composeTestRule) {
                verifyNormalBrowsingButtonIsSelected()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/555776
    @Test
    fun launchPageShortcutInPrivateBrowsingTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                verifyOpenLinksInPrivateTabEnabled()
                clickOpenLinksInPrivateTabSwitch()
            }
            .goBack {}
            .goBack(composeTestRule) {}

        homeScreen(composeTestRule) {
            verifyHomeComponent()
        }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(defaultWebPage.url) {}
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddToHomeScreenButton {
                addShortcutName(pageShortcutName)
                clickAddShortcutButton()
                clickSystemHomeScreenShortcutAddButton()
                verifyShortcutAdded(pageShortcutName)
            }

        mDevice.waitForIdle()
        // We need to close the existing tab here, to open a different session
        restartApp(composeTestRule.activityRule)

        browserScreen(composeTestRule) {}
            .openTabDrawer(composeTestRule) {
                verifyNormalBrowsingButtonIsSelected()
                closeTab()
            }

        addToHomeScreen(composeTestRule) {}
            .searchAndOpenHomeScreenShortcut(pageShortcutName) {}
            .openTabDrawer(composeTestRule) {
                verifyPrivateBrowsingButtonIsSelected()
                closeTab()
            }

        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                clickOpenLinksInPrivateTabSwitch()
                verifyOpenLinksInPrivateTabOff()
            }
            .goBack {}
            .goBack(composeTestRule) {}

        homeScreen(composeTestRule) {
            verifyHomeComponent()
        }

        addToHomeScreen(composeTestRule) {}
            .searchAndOpenHomeScreenShortcut(pageShortcutName) {}
            .openTabDrawer(composeTestRule) {
                verifyNormalBrowsingButtonIsSelected()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/414583
    @Test
    fun addPrivateBrowsingShortcutFromSettingsTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openPrivateBrowsingSubMenu {
                cancelPrivateShortcutAddition()
                addPrivateShortcutToHomescreen()
                verifyPrivateBrowsingShortcutIcon()
            }
            .openPrivateBrowsingShortcut(composeTestRule) {}
        homeScreen(composeTestRule) {
            verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = true)
        }
    }
}
