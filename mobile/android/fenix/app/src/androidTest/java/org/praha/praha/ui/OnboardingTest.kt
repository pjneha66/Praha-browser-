/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.test.filters.SdkSuppress
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.praha.praha.customannotations.Converted
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.AppAndSystemHelper.openAppFromExternalLink
import org.praha.praha.helpers.AppAndSystemHelper.runWithLauncherIntent
import org.praha.praha.helpers.FenixTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.helpers.TestAssetHelper.getGenericAsset
import org.praha.praha.helpers.TestHelper.closeApp
import org.praha.praha.helpers.TestHelper.restartApp
import org.praha.praha.helpers.perf.DetectMemoryLeaksRule
import org.praha.praha.ui.robots.browserScreen
import org.praha.praha.ui.robots.homeScreen

class OnboardingTest {
    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule(grantNotifications = false)

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(
                launchActivity = false,
                skipOnboarding = false,
            )
        ) {
            it.activity
        }

    @get:Rule(order = 2) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349493
    @Converted(
        replacedBy = ["org.praha.praha.ui.efficiency.tests.OnboardingTest#verifyTheTermsOfUseOnboardingCardTest"],
        bug = 2057054,
        since = "2026-07",
    )
    @SdkSuppress(minSdkVersion = 29)
    @SmokeTest
    @Test
    fun verifyTheTermsOfUseOnboardingCardTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                verifyTheSetAsDefaultBrowserOnboardingCard()
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3814795
    @Ignore(
        "Disabled temporarily,as the default prompt is being turned off for tests, for more info check: https://bugzilla.mozilla.org/show_bug.cgi?id=2051502"
    )
    @SdkSuppress(minSdkVersion = 29)
    @SmokeTest
    @Test
    fun verifyTheSetAsDefaultBrowserOnboardingCardFunctionalityTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                verifyTheSetAsDefaultBrowserOnboardingCard()
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349492
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun verifyTheOnboardingCardOrderTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()

                clickTheSetAsDefaultBrowserDialogCancelButton()
                verifyTheSetAsDefaultBrowserOnboardingCard()
                clickNotNowOnboardingCardButton()
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3814666
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun verifyTheTermsOfUseOnboardingCardCannotBeDismissedWithoutAcceptingTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                swipeRightTheTermsOfUseOnboardingCard()
                verifyTheTermsOfUseOnboardingCard()
                restartApp(composeTestRule.activityRule)
                verifyTheTermsOfUseOnboardingCard()
                closeApp(composeTestRule.activityRule)
                restartApp(composeTestRule.activityRule)
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349494
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun verifyTheSetAsDefaultBrowserOnboardingCardTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                verifyTheSetAsDefaultBrowserOnboardingCard()
                closeApp(composeTestRule.activityRule)
                restartApp(composeTestRule.activityRule)
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                verifyTheSetAsDefaultBrowserOnboardingCard()
            }
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun verifyAnExternalLinkOpenedDuringOnboardingIsOpenedAfterOnboardingTest() {
        val externalPage = fenixTestRule.mockWebServer.getGenericAsset(1)

        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                verifyTheTermsOfUseOnboardingCard()
                openAppFromExternalLink(composeTestRule, externalPage.url.toString())
                verifyTheTermsOfUseOnboardingCard()
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                clickNotNowOnboardingCardButton()
                clickContinueIfMarketingCardShown()
            }
            browserScreen(composeTestRule) { verifyUrl(externalPage.url.toString()) }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3911762
    @SdkSuppress(minSdkVersion = 29)
    @SmokeTest
    @Test
    fun verifyEdgeToEdgeWallpaperAfterOnboardingTest() {
        runWithLauncherIntent(composeTestRule.activityRule) {
            homeScreen(composeTestRule) {
                clickTheOnboardingCardContinueButton()
                clickTheSetAsDefaultBrowserDialogCancelButton()
                clickNotNowOnboardingCardButton()
                clickContinueIfMarketingCardShown()
            }
            homeScreen(composeTestRule) {
                verifyEdgeToEdgeWallpaperApplied(composeTestRule)
            }
        }
    }
}
