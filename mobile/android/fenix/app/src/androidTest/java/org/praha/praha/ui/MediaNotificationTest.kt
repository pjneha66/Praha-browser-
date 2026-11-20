/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import mozilla.components.concept.engine.mediasession.MediaSession
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.ext.components
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityTestRule
import org.praha.praha.helpers.MatcherHelper
import org.praha.praha.helpers.RetryTestRule
import org.praha.praha.helpers.RetryableComposeTestRule
import org.praha.praha.helpers.TestAssetHelper.audioPageAsset
import org.praha.praha.helpers.TestAssetHelper.videoPageAsset
import org.praha.praha.helpers.TestHelper.appContext
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.browserScreen
import org.praha.praha.ui.robots.clickPageObject
import org.praha.praha.ui.robots.homeScreen
import org.praha.praha.ui.robots.navigationToolbar
import org.praha.praha.ui.robots.notificationShade

/**
 * Tests for verifying basic functionality of media notifications:
 * - video and audio playback system notifications appear and can pause/play the media content
 * - a media notification icon is displayed on the homescreen for the tab playing media content Note: this test only
 *   verifies media notifications, not media itself
 */
class MediaNotificationTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    private val browserStore
        get() = fenixTestRule.browserStore

    @get:Rule(order = 1) val retryTestRule = RetryTestRule(3)

    @get:Rule(order = 2)
    val retryableComposeTestRule = RetryableComposeTestRule {
        AndroidComposeTestRuleV2(HomeActivityTestRule.withDefaultSettingsOverrides()) { it.activity }
    }

    private val composeTestRule
        get() = retryableComposeTestRule.current

    @get:Rule(order = 3) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1347033
    @Converted(
        replacedBy =
            ["org.praha.praha.ui.efficiency.tests.MediaNotificationTest#verifyVideoPlaybackSystemNotificationTest"],
        bug = 2063242,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyVideoPlaybackSystemNotificationTest() {
        val videoTestPage = mockWebServer.videoPageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(videoTestPage.url) {
                mDevice.waitForIdle()
                clickPageObject(composeTestRule, MatcherHelper.itemWithText("Play"))
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
            }
            .openNotificationShade {
                verifySystemNotificationExists(videoTestPage.title)
                clickMediaNotificationControlButton("Pause")
                verifyMediaSystemNotificationButtonState("Play")
            }

        mDevice.pressBack()

        browserScreen(composeTestRule) {
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
            }
            .openTabDrawer(composeTestRule) {
                closeTab()
            }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationDoesNotExist(videoTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2316010
    @Converted(
        replacedBy =
            ["org.praha.praha.ui.efficiency.tests.MediaNotificationTest#verifyVideoPlaybackSystemNotificationTest"],
        bug = 2063242,
        since = "2026-08",
    )
    @SmokeTest
    @Test
    fun verifyAudioPlaybackSystemNotificationTest() {
        val audioTestPage = mockWebServer.audioPageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(audioTestPage.url) {
                clickPageObject(composeTestRule, MatcherHelper.itemWithText("Play"))
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
            }
            .openNotificationShade {
                verifySystemNotificationExists(audioTestPage.title)
                clickMediaNotificationControlButton("Pause")
                verifyMediaSystemNotificationButtonState("Play")
            }

        mDevice.pressBack()

        browserScreen(composeTestRule) {
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
            }
            .openTabDrawer(composeTestRule) {
                closeTab()
            }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationDoesNotExist(audioTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    // TestRail: https://mozilla.testrail.io/index.php?/cases/view/903595
    @Converted(
        replacedBy =
            ["org.praha.praha.ui.efficiency.tests.MediaNotificationTest#mediaSystemNotificationInPrivateModeTest"],
        bug = 2079212,
        since = "2026-10",
    )
    @Critical
    @Test
    fun mediaSystemNotificationInPrivateModeTest() {
        val audioTestPage = mockWebServer.audioPageAsset

        // RetryTestRule does not clear tabs between retries; the tray-wide indicator assertions below need
        // exactly the one private tab this test opens, so drop any leftovers.
        appContext.components.useCases.tabsUseCases.removeAllTabs()

        homeScreen(composeTestRule) {}
            .openTabDrawer {}
            .toggleToPrivateTabs {}
            .openNewTab {}
            .submitQuery(audioTestPage.url.toString()) {
                mDevice.waitForIdle()
                clickPageObject(composeTestRule, MatcherHelper.itemWithText("Play"))
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
            }
            // While playing, the tab carries the "Playing media" indicator.
            .openTabDrawer(composeTestRule) {
                verifyTabMediaIndicator()
            }
            .closeTabDrawer {}
            .openNotificationShade {
                verifySystemNotificationExists("A site is playing media")
                clickMediaNotificationControlButton("Pause")
                verifyMediaSystemNotificationButtonState("Play")
            }

        mDevice.pressBack()

        browserScreen(composeTestRule) {
                assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
            }
            // Pausing clears the PLAYING state, so the indicator is gone.
            .openTabDrawer(composeTestRule) {
                verifyTabMediaIndicatorDoesNotExist()
            }
    }
}
