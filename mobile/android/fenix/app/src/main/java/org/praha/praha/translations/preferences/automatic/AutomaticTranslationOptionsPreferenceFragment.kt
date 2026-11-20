/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.translations.preferences.automatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.navArgs
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.observeAsComposableState
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.requireComponents
import org.praha.praha.ext.showToolbar
import org.praha.praha.theme.PrahaTheme

/** A fragment displaying Automatic Translation Options screen. */
class AutomaticTranslationOptionsPreferenceFragment : Fragment(), SystemInsetsPaddedFragment {
    private val args by navArgs<AutomaticTranslationOptionsPreferenceFragmentArgs>()
    private val browserStore: BrowserStore by lazy { requireComponents.core.store }

    override fun onResume() {
        super.onResume()
        args.selectedLanguageDisplayName?.let { showToolbar(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val languageSettings =
            browserStore
                .observeAsComposableState { state ->
                    state.translationEngine.languageSettings
                }
                .value

        val selectedLanguage = languageSettings?.get(args.selectedLanguageCode)

        PrahaTheme {
            selectedLanguage?.let {
                AutomaticTranslationOptionsPreference(
                    selectedOption = getAutomaticTranslationOptionPreference(selectedLanguage),
                    onItemClick = {
                        browserStore.dispatch(
                            TranslationsAction.UpdateLanguageSettingsAction(
                                languageCode = args.selectedLanguageCode,
                                setting = getLanguageSetting(it),
                            )
                        )
                    },
                )
            }
        }
    }
}
