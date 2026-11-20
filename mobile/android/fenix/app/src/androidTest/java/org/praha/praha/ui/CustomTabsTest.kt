/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.praha.praha.IntentReceiverActivity
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.AppAndSystemHelper.openAppFromExternalLink
import org.praha.praha.helpers.DataGenerationHelper.createCustomTabIntent
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.MatcherHelper.itemContainingText
import org.praha.praha.helpers.MatcherHelper.itemWithText
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestAssetHelper.pdfFormAsset
import org.praha.praha.helpers.TestAssetHelper.waitingTimeLong
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.browserScreen
import org.praha.praha.ui.robots.clickPageObject
import org.praha.praha.ui.robots.customTabScreen
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.notificationShade

class CustomTabsTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    private val customMenuItem = "TestMenuItem"
    private val customTabActionButton = "CustomActionButton"

    private val loginPage = "https://mozilla-mobile.github.io/testapp/loginForm"

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityIntentTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule
    val intentReceiverActivityTestRule =
        ActivityTestRule(
            IntentReceiverActivity::class.java,
            true,
            false,
        )

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249659
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CustomTabsTest#verifyLoginSaveInCustomTabTest"],
        bug = 2063232,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyLoginSaveInCustomTabTest() {
        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                loginPage.toUri().toString(),
                customMenuItem,
            )
        )

        customTabScreen(composeTestRule) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
            fillAndSubmitLoginCredentials("mozilla", "firefox")
        }

        browserScreen(composeTestRule) {
            verifySaveLoginPromptIsDisplayed()
            clickPageObject(composeTestRule, itemWithText("Save"))
        }

        openAppFromExternalLink(composeTestRule, loginPage)

        browserScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openLoginsAndPasswordSubMenu {}
            .openSavedLogins(composeTestRule) {
                verifySecurityPromptForLogins()
                tapSetupLater()
                verifySavedLoginsSectionUsername("mozilla")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2334762
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CustomTabsTest#copyCustomTabToolbarUrlTest"],
        bug = 2078545,
        since = "2026-10",
    )
    @Critical
    @Test
    fun copyCustomTabToolbarUrlTest() {
        val customTabPage = mockWebServer.getGenericAsset(1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem,
            )
        )

        customTabScreen(composeTestRule) {
            verifyCustomTabUrl(customTabPage.url.toString())
            longClickAndCopyToolbarUrl()
        }

        openAppFromExternalLink(composeTestRule, customTabPage.url.toString())

        browserScreen(composeTestRule) {}
            .openSearch {
                clickClearButton()
                longClickToolbar()
                clickPasteText()
                verifyTypedToolbarText(customTabPage.url.toString(), exists = true)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2334761
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CustomTabsTest#verifyDownloadInACustomTabTest"],
        bug = 2060346,
        since = "2026-08",
        notes =
            "The replacement serves downloads.html from the local mockWebServer instead of " +
                "storage.googleapis.com; same asset and link, no external network dependency.",
    )
    @SmokeTest
    @Test
    fun verifyDownloadInACustomTabTest() {
        val customTabPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
        val downloadFile = "web_icon.png"

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.toUri().toString(),
                customMenuItem,
            )
        )

        customTabScreen(composeTestRule) {
            waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
        }

        browserScreen(composeTestRule) {}
            .clickDownloadLink(downloadFile) {
                verifyDownloadPrompt(composeTestRule)
            }
            .clickDownload(composeTestRule) {
                verifyDownloadCompleteSnackbar(fileName = "web_icon.png")
                waitUntilDownloadSnackbarGone()
            }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Download completed")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249644
    // Verifies the main menu of a custom tab with a custom menu item
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CustomTabsTest#verifyCustomTabMenuItemsTest"],
        bug = 2057412,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun verifyCustomTabMenuItemsTest() {
        val customMenuItem = "TestMenuItem"
        val customTabPage = mockWebServer.getGenericAsset(1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem,
            )
        )

        customTabScreen(composeTestRule) {
                verifyCustomTabCloseButton()
            }
            .openMainMenu {
                verifyCustomTabsMainMenuItems(customMenuItem, true)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249645
    // The test opens a link in a custom tab then sends it to the browser
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CustomTabsTest#openCustomTabInFirefoxTest"],
        bug = 2057416,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun openCustomTabInFirefoxTest() {
        val customTabPage = mockWebServer.getGenericAsset(1)

        intentReceiverActivityTestRule.launchActivity(createCustomTabIntent(customTabPage.url.toString()))

        customTabScreen(composeTestRule) {
                verifyCustomTabCloseButton()
            }
            .openMainMenu {}
            .clickOpenInBrowserButtonFromRedesignedToolbar {
                verifyPageContent(customTabPage.content)
                verifyTabCounter("1")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249643
    @Test
    fun verifyCustomTabViewItemsTest() {
        val customTabPage = mockWebServer.getGenericAsset(1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageUrl = customTabPage.url.toString(),
                customActionButtonDescription = customTabActionButton,
            )
        )

        customTabScreen(composeTestRule) {
            verifyCustomTabCloseButton()
            verifyCustomTabsSiteInfoButton()
            verifyCustomTabToolbarTitle(customTabPage.title)
            verifyCustomTabUrl(customTabPage.url.toString())
            verifyCustomTabActionButton(customTabActionButton)
            verifyMainMenuButton()
            clickCustomTabCloseButton()
        }
        homeScreen(composeTestRule) {
            verifyHomeScreenAppBarItems()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2239544
    @Test
    fun verifyPDFViewerInACustomTabTest() {
        val customTabPage = mockWebServer.getGenericAsset(3)
        val pdfFormResource = mockWebServer.pdfFormAsset
        val pdfTitle = "Untitled document - pdfForm.pdf"
        val downloadFile = "pdfForm.pdf"

        intentReceiverActivityTestRule.launchActivity(createCustomTabIntent(customTabPage.url.toString()))

        customTabScreen(composeTestRule) {
            clickPageObject(composeTestRule, itemWithText("PDF form file"))
            clickPageObject(composeTestRule, itemContainingText("Stay in"))
            waitForPageToLoad()
            verifyPDFReaderToolbarItems()
            verifyCustomTabCloseButton()
            verifyCustomTabsSiteInfoButton()
            verifyCustomTabToolbarTitle(pdfTitle)
            verifyCustomTabUrl(pdfFormResource.url.toString())
            verifyMainMenuButton()
        }
        browserScreen(composeTestRule) {}
            .clickDownloadPDFButton {
                verifyDownloadCompleteSnackbar(fileName = downloadFile)
                waitUntilDownloadSnackbarGone()
            }
        customTabScreen(composeTestRule) {
            clickCustomTabCloseButton()
        }
        homeScreen(composeTestRule) {
            verifyHomeScreenAppBarItems()
        }
    }
}
