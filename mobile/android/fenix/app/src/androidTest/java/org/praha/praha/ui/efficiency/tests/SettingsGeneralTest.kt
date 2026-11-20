/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.tests

import org.junit.Test
import org.praha.praha.R
import org.praha.praha.customannotations.Critical
import org.praha.praha.customannotations.SmokeTest
import org.praha.praha.helpers.DataGenerationHelper.getStringResource
import org.praha.praha.helpers.TestAssetHelper.loremIpsumAsset
import org.praha.praha.helpers.TestHelper.mDevice
import org.praha.praha.ui.efficiency.helpers.BaseTest
import org.praha.praha.ui.efficiency.selectors.SettingsSelectors
import org.praha.praha.ui.util.FRENCH_FOLLOW_DEVICE_LANGUAGE_OPTION
import org.praha.praha.ui.util.FRENCH_LANGUAGE_HEADER
import org.praha.praha.ui.util.ROMANIAN_LANGUAGE_HEADER

class SettingsGeneralTest : BaseTest() {

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2092697
    @Critical
    @Test
    fun verifyGeneralSettingsItemsTest() {
        on.settings
            .navigateToPage()
            .mozVerifyElementsByGroup(SettingsSelectors.Group.GENERAL_SETTINGS_DEFAULT_VALUES)
            .mozVerifyElementsByGroup(SettingsSelectors.Group.GENERAL_SETTINGS_SECTION)
            .mozVerifyOptionSwitchIsNotChecked(SettingsSelectors.SET_AS_DEFAULT_BROWSER_BUTTON)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/344213
    @SmokeTest
    @Test
    fun verifyFontSizingChangeTest() {
        val webpage = mockWebServer.loremIpsumAsset.url
        val textSizePercentage = 180

        on.settingsAccessibility
            .navigateToPage()
            .verifyFontSizingMenuItems(
                isTheAutomaticFontSizingToggleChecked = true,
                isTheFontSizingSliderEnabled = false,
                isTheZoomOnAllWebsitesToggleChecked = false,
            )
            .clickAutomaticFontSizingToggle()
            .verifyFontSizingMenuItems(
                isTheAutomaticFontSizingToggleChecked = false,
                isTheFontSizingSliderEnabled = true,
                isTheZoomOnAllWebsitesToggleChecked = false,
            )
            .changeTextSizeSlider(textSizePercentage)
            .verifyTextSizePercentage(textSizePercentage)

        on.browserPage.navigateToPage(webpage.toString()).verifyTextSizeOnWebsite(textSizePercentage)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243583
    @SmokeTest
    @Test
    fun changeDefaultBrowserSetting() {
        on.settings
            .navigateToPage()
            .verifyDefaultBrowserToggle(false)
            .clickDefaultBrowserSwitch()
            .verifyAndroidDefaultAppsMenuAppears()

        // Dismiss the system default-apps request.
        mDevice.pressBack()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/516079
    @SmokeTest
    @Test
    fun setAppLanguageDifferentThanSystemLanguageTest() {
        val enLanguageHeaderText = getStringResource(R.string.preferences_language)

        on.settingsLanguage.navigateToPage()
        on.settingsLanguage.selectLanguage("Romanian").verifyLanguageSettingHeaderIsTranslated(ROMANIAN_LANGUAGE_HEADER)
        on.settingsLanguage.selectLanguage("Français").verifyLanguageSettingHeaderIsTranslated(FRENCH_LANGUAGE_HEADER)
        on.settingsLanguage
            .selectLanguage(FRENCH_FOLLOW_DEVICE_LANGUAGE_OPTION)
            .verifyLanguageSettingHeaderIsTranslated(enLanguageHeaderText)
    }
}
