/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.praha.praha.Config
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.AppAndSystemHelper.assertExternalAppOpens
import org.praha.praha.helpers.AppAndSystemHelper.clickSystemHomeScreenShortcutAddButton
import org.praha.praha.helpers.AppAndSystemHelper.runWithCondition
import org.praha.praha.helpers.Constants.PackageName.GOOGLE_DOCS
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.MatcherHelper.itemContainingText
import org.praha.praha.helpers.MatcherHelper.itemWithText
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestHelper.appName
import org.praha.praha.helpers.TestHelper.clickSnackbarButton
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.browserScreen
import org.praha.praha.ui.robots.clickPageObject
import org.praha.praha.ui.robots.navigationToolbar
import org.praha.praha.ui.robots.notificationShade

class PDFViewerTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    private val downloadTestPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
    private val pdfFileName = "washington.pdf"
    private val pdfFileURL = "storage.googleapis.com/mobile_test_assets/public/washington.pdf"
    private val pdfFileContent = "Washington Crossing the Delaware"

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(HomeActivityIntentTestRule.withDefaultSettingsOverrides()) { it.activity }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2048140
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.PDFViewerTest#verifyPDFFileIsOpenedInTheSameTabTest"],
        bug = 2060844,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyPDFFileIsOpenedInTheSameTabTest() {
        val genericURL = mockWebServer.getGenericAsset(3)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemContainingText("PDF form file"))
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
                verifyPageContent("Washington Crossing the Delaware")
                verifyTabCounter("1")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2145448
    // Download PDF file using the download toolbar button
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.PDFViewerTest#verifyPDFViewerDownloadButtonTest"],
        bug = 2083697,
        since = "2026-10",
    )
    @Critical
    @Test
    fun verifyPDFViewerDownloadButtonTest() {
        val genericURL = mockWebServer.getGenericAsset(3)
        val downloadFile = "pdfForm.pdf"

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemWithText("PDF form file"))
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
            }
            .clickDownloadPDFButton {
                verifyDownloadCompleteSnackbar(fileName = downloadFile)
                clickSnackbarButton(composeTestRule = composeTestRule, "OPEN")
                assertExternalAppOpens(GOOGLE_DOCS)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2283305
    @Test
    fun pdfFindInPageTest() {
        val genericURL = mockWebServer.getGenericAsset(3)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemWithText("PDF form file"))
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
            }
            .openThreeDotMenu {}
            .clickFindInPageButton {
                verifyFindInPageNextButton()
                verifyFindInPagePrevButton()
                verifyFindInPageCloseButton()
                enterFindInPageQuery("l")
                verifyFindInPageResult("1/2")
                clickFindInPageNextButton()
                verifyFindInPageResult("2/2")
                clickFindInPagePrevButton()
                verifyFindInPageResult("1/2")
            }
            .closeFindInPageWithCloseButton(composeTestRule) {
                verifyFindInPageBar(false)
            }
            .openThreeDotMenu {}
            .clickFindInPageButton {
                enterFindInPageQuery("p")
                verifyFindInPageResult("1/1")
            }
            .closeFindInPageWithBackButton(composeTestRule) {
                verifyFindInPageBar(false)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2284297
    @Test
    fun addPDFToHomeScreenTest() {
        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(downloadTestPage.toUri()) {
                clickPageObject(composeTestRule, itemContainingText(pdfFileName))
                verifyUrl(pdfFileURL)
                verifyPageContent(pdfFileContent)
            }
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddToHomeScreenButton {
                verifyShortcutTextFieldTitle(pdfFileName)
                clickAddShortcutButton()
                clickSystemHomeScreenShortcutAddButton()
            }
            .openHomeScreenShortcut(pdfFileName) {
                verifyUrl(pdfFileURL)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2797677
    // Download PDF file using the download toolbar button
    @Test
    fun verifyDownloadedPDFIsOpenedInFirefoxTest() {
        val genericURL = mockWebServer.getGenericAsset(3)
        val downloadFile = "pdfForm.pdf"
        val secondDownloadNotificationTitle = "pdfForm(1).pdf"

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemWithText("PDF form file"))
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
                verifyTabCounter("1")
            }
            .openThreeDotMenu {
                clickTheMoreButton()
            }
            .clickAddToHomeScreenButton {
                verifyShortcutTextFieldTitle("Untitled document")
                addShortcutName("pdfForm")
                clickAddShortcutButton()
                clickSystemHomeScreenShortcutAddButton()
            }
            .openHomeScreenShortcut("pdfForm") {
                verifyTabCounter("1")
            }
            .clickDownloadPDFButton {
                verifyDownloadCompleteSnackbar(fileName = downloadFile)
                clickSnackbarButton(composeTestRule = composeTestRule, "OPEN")
            }
        browserScreen(composeTestRule) {
            selectToAlwaysOpenDownloadedFileWithApp(appName = appName)
            verifyUrl("content://media/external_primary/downloads/")
            verifyTabCounter("2")
        }

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemWithText("PDF form file"))
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
            }
            .clickDownloadPDFButton(confirmDownloadPrompt = false) {
                runWithCondition(Config.channel.isNightlyOrDebug) {
                    clickDownloadAgain()
                }
            }

        mDevice.openNotification()

        notificationShade {
            expandMultipleDownloadNotification(secondDownloadNotificationTitle)
            clickNotification(secondDownloadNotificationTitle)
        }
        browserScreen(composeTestRule) {
                verifyUrl("content://media/external_primary/downloads/")
                verifyTabCounter("3")
            }
            .openThreeDotMenu {}
            .clickDownloadsButton {
                clickDownloadedItem(downloadFile)
            }
        browserScreen(composeTestRule) {
            verifyTabCounter("4")
            verifyUrl("content://media/external_primary/downloads/")
        }
    }
}
