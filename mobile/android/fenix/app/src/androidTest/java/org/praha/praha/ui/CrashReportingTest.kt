/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import mozilla.components.lib.crash.store.CrashReportOption
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestHelper.exitMenu
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.navigationToolbar

class CrashReportingTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                isPocketEnabled = false,
                isWallpaperOnboardingEnabled = false,
                crashReportOption = CrashReportOption.Ask,
            )
        ) {
            it.activity
        }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/308906
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CrashReportingTest#closeTabFromCrashedTabReporterTest"],
        bug = 2084953,
        since = "2026-10",
    )
    @Critical
    @Test
    fun closeTabFromCrashedTabReporterTest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {}
            .clickTabCrashedCloseButton {}
            .openTabDrawer {
                verifyNoOpenTabsInNormalBrowsing()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2336134
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CrashReportingTest#restoreTabFromTabCrashedReporterTest"],
        bug = 2084953,
        since = "2026-10",
    )
    @Critical
    @Test
    fun restoreTabFromTabCrashedReporterTest() {
        val website = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {}.enterURLAndEnterToBrowser(website.url) {}
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView()
            }
            .clickTabCrashedRestoreButton {
                verifyPageContent(website.content)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1681928
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.CrashReportingTest#useAppWhileTabIsCrashedTest"],
        bug = 2046392,
        since = "2026-06",
    )
    @SmokeTest
    @Test
    fun useAppWhileTabIsCrashedTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(firstWebPage.url) {
                mDevice.waitForIdle()
            }
            .openTabDrawer(composeTestRule) {}
            .openNewTab {}
            .submitQuery(secondWebPage.url.toString()) {
                waitForPageToLoad()
            }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Ask)
            }
            .openTabDrawer(composeTestRule) {
                verifyExistingOpenTabs(firstWebPage.title)
                verifyExistingOpenTabs(secondWebPage.title)
            }
            .closeTabDrawer {}
            .goToHomescreen {
                verifyExistingTopSitesList()
            }
            .openThreeDotMenu {
                verifySettingsButton()
            }
    }

    @Test
    fun sendCrashReportCheckboxIsCheckedWithAskBeforeSendingCrashReportOptionTest() {
        composeTestRule.activityRule.applySettingsExceptions { it.crashReportOption = CrashReportOption.Ask }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Ask)
            }
    }

    @Test
    fun sendCrashReportCheckboxIsCheckedWithSendAutomaticallyCrashReportOptionTest() {
        composeTestRule.activityRule.applySettingsExceptions { it.crashReportOption = CrashReportOption.Auto }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Auto)
            }
    }

    @Test
    fun sendCrashReportCheckboxIsHiddenWithNeverSendCrashReportsOptionTest() {
        composeTestRule.activityRule.applySettingsExceptions { it.crashReportOption = CrashReportOption.Never }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Never)
            }
    }

    @Test
    fun sendCrashReportCheckboxIsHiddenAfterSelectingNeverSendInSettingsTest() {
        homeScreen(composeTestRule) {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openSettingsSubMenuDataCollection {
                clickTheCrashReportsRadioButton(composeTestRule, "Never send")
                verifyTheCrashReportOptionStates(
                    composeTestRule,
                    isAskBeforeSendingCrashReportsEnabled = false,
                    isNeverSendCrashReportsEnabled = true,
                )
            }

        exitMenu()

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Never)
            }
    }

    @Test
    fun updatingCrashReportingPreferenceIsReflectedTest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Ask)
                toggleTabCrashReporterCheckbox(expectedCheckedState = false)
            }
            .clickTabCrashedCloseButton {}
            .openThreeDotMenu {}
            .clickSettingsButton {}
            .openSettingsSubMenuDataCollection {
                clickTheCrashReportsRadioButton(composeTestRule, "Never send")
                verifyTheCrashReportOptionStates(
                    composeTestRule,
                    isAskBeforeSendingCrashReportsEnabled = false,
                    isNeverSendCrashReportsEnabled = true,
                )
            }

        exitMenu()

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Never)
            }
    }

    @Test
    fun sendCrashReportPreferenceCheckboxResetsForMultipleCrashesTest() {
        val website = mockWebServer.getGenericAsset(1)
        navigationToolbar(composeTestRule) {}.enterURLAndEnterToBrowser(website.url) {}
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Ask)
                toggleTabCrashReporterCheckbox(expectedCheckedState = false)
            }
            .clickTabCrashedRestoreButton {
                verifyPageContent(website.content)
            }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser("about:crashcontent".toUri()) {
                verifyTabCrashReporterView(crashReportOption = CrashReportOption.Ask)
            }
    }
}
