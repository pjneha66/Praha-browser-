/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.tests

import org.junit.Test
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.Constants
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestAssetHelper.waitingTime
import org.praha.praha.helpers.TestAssetHelper.waitingTimeLong
import org.praha.praha.ui.efficiency.helpers.BaseTest
import org.praha.praha.ui.efficiency.selectors.BrowserPageSelectors
import org.praha.praha.ui.efficiency.selectors.DownloadsSelectors
import org.praha.praha.ui.efficiency.selectors.ToolbarSelectors

class PDFViewerTest : BaseTest() {

    // Converted from legacy PDFViewerTest.verifyPDFFileIsOpenedInTheSameTabTest
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2048140
    @SmokeTest
    @Test
    fun verifyPDFFileIsOpenedInTheSameTabTest() {
        val genericURL = mockWebServer.getGenericAsset(3)

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.browserPage.mozClick(BrowserPageSelectors.PAGE_CONTENT("PDF form file"))
        // The applinks "Stay in Firefox" prompt only appears when the device has an external PDF handler,
        // so click it only if present (legacy clicked it unconditionally).
        on.browserPage.mozClickIfPresent(BrowserPageSelectors.STAY_IN_FIREFOX_PROMPT_BUTTON, timeout = waitingTime)
        on.browserPage.mozVerify(
            BrowserPageSelectors.PAGE_CONTENT("Washington Crossing the Delaware"),
            timeout = waitingTimeLong,
        )
        on.browserPage.mozVerify(ToolbarSelectors.TAB_COUNTER_WITH_COUNT("1"))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2145448
    @Critical
    @Test
    fun verifyPDFViewerDownloadButtonTest() {
        val genericURL = mockWebServer.getGenericAsset(3)
        val downloadFile = "pdfForm.pdf"

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.browserPage.mozClick(BrowserPageSelectors.PAGE_CONTENT("PDF form file"))
        // The applinks "Stay in Firefox" prompt only appears when the device has an external PDF handler,
        // so click it only if present (legacy clicked it unconditionally).
        on.browserPage.mozClickIfPresent(BrowserPageSelectors.STAY_IN_FIREFOX_PROMPT_BUTTON, timeout = waitingTime)
        on.browserPage.clickDownloadPDFButton()
        on.downloads
            .mozVerify(DownloadsSelectors.DOWNLOAD_COMPLETE_SNACKBAR, timeout = waitingTimeLong)
            .mozVerify(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_ACTION_LABEL)
            .mozVerify(DownloadsSelectors.FILE_NAME_TEXT(downloadFile))
            .mozClick(DownloadsSelectors.DOWNLOAD_SNACK_BAR_OPEN_BUTTON)
            .mozVerifyFileOpensInExternalApp(Constants.PackageName.GOOGLE_DOCS)
        on.downloads.verifyDownloadedFileStillExistsOnStorage(downloadFile)
    }
}
