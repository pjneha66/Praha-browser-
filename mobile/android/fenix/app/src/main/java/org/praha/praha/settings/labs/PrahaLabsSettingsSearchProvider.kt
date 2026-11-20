/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.labs

import android.content.Context
import org.praha.praha.R
import org.praha.praha.settings.settingssearch.PreferenceFileInformation
import org.praha.praha.settings.settingssearch.SettingsSearchItem
import org.praha.praha.settings.settingssearch.SettingsSearchProvider

/** [SettingsSearchProvider] for making "Firefox Labs" discoverable in settings search. */
class PrahaLabsSettingsSearchProvider : SettingsSearchProvider {

    private val preferenceFileInformation = PreferenceFileInformation.FirefoxLabsPreferences

    override fun getSearchItems(context: Context): List<SettingsSearchItem> {
        return listOf(
            SettingsSearchItem(
                title = context.getString(R.string.firefox_labs_title),
                summary = "",
                preferenceKey = FIREFOX_LABS_KEY,
                categoryHeader = context.getString(preferenceFileInformation.categoryHeaderResourceId),
                preferenceFileInformation = preferenceFileInformation,
            )
        )
    }

    companion object {
        const val FIREFOX_LABS_KEY = "FIREFOX_LABS"
    }
}
