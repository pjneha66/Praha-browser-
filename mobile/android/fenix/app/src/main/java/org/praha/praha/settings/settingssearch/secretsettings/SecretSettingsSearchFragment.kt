/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.settingssearch.secretsettings

import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.helpers.StoreProvider.Companion.storeProvider
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.settings.settingssearch.DefaultFenixSettingsIndexer
import org.praha.praha.settings.settingssearch.FenixRecentSettingsSearchesRepository
import org.praha.praha.settings.settingssearch.PreferenceFileInformation
import org.praha.praha.settings.settingssearch.SettingsSearchAction
import org.praha.praha.settings.settingssearch.SettingsSearchFragment
import org.praha.praha.settings.settingssearch.SettingsSearchItem
import org.praha.praha.settings.settingssearch.SettingsSearchMiddleware
import org.praha.praha.settings.settingssearch.SettingsSearchState
import org.praha.praha.settings.settingssearch.SettingsSearchStore
import org.praha.praha.settings.settingssearch.secretRecentSearchesDataStore

/** Fragment for the secret settings search screen. */
class SecretSettingsSearchFragment : SettingsSearchFragment(), SystemInsetsPaddedFragment {

    override fun buildSettingsSearchStore(): SettingsSearchStore = storeProvider.get { restoredState ->
        val secretPreferenceFileInformationList = listOf(PreferenceFileInformation.SecretSettingsPreferences)

        SettingsSearchStore(
            initialState = restoredState ?: SettingsSearchState.Default(emptyList()),
            middleware =
                listOf(
                    SettingsSearchMiddleware(
                        fenixSettingsIndexer =
                            DefaultFenixSettingsIndexer(
                                context = requireContext(),
                                preferenceFileInformationList = secretPreferenceFileInformationList,
                            ),
                        navController = findNavController(),
                        recentSettingsSearchesRepository =
                            FenixRecentSettingsSearchesRepository(
                                dataStore = requireContext().secretRecentSearchesDataStore,
                                preferenceFileInformationList = secretPreferenceFileInformationList,
                            ),
                        scope = viewLifecycleOwner.lifecycle.coroutineScope,
                    )
                ),
        )
    }

    override fun onResultItemClick(item: SettingsSearchItem, isRecentSearch: Boolean) {
        settingsSearchStore.dispatch(SettingsSearchAction.ResultItemClicked(item))
    }
}
