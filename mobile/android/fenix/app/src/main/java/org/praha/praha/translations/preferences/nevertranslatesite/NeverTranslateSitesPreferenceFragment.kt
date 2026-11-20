/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.translations.preferences.nevertranslatesite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.translate.TranslationError
import mozilla.components.lib.state.ext.observeAsComposableState
import org.praha.praha.R
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.requireComponents
import org.praha.praha.ext.showToolbar
import org.praha.praha.theme.PrahaTheme

/** A fragment displaying never translate site items list. */
class NeverTranslateSitesPreferenceFragment : Fragment(), SystemInsetsPaddedFragment {

    private val browserStore: BrowserStore by lazy { requireComponents.core.store }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.never_translate_site_toolbar_title_preference))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PrahaTheme {
            val neverTranslateSites =
                browserStore
                    .observeAsComposableState { state ->
                        state.translationEngine.neverTranslateSites
                    }
                    .value

            val engineError =
                browserStore
                    .observeAsComposableState { state ->
                        state.translationEngine.engineError
                    }
                    .value

            val couldNotLoadNeverTranslateSites = engineError as? TranslationError.CouldNotLoadNeverTranslateSites

            NeverTranslateSitesPreference(
                neverTranslateSitesListPreferences = neverTranslateSites,
                hasNeverTranslateSitesError = couldNotLoadNeverTranslateSites != null || neverTranslateSites == null,
                onItemClick = {
                    findNavController()
                        .navigate(
                            NeverTranslateSitesPreferenceFragmentDirections
                                .actionNeverTranslateSitePreferenceToNeverTranslateSiteDialogPreference(
                                    neverTranslateSiteUrl = it
                                )
                        )
                },
            )
        }
    }
}
