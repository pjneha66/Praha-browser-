/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.emailmasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.helpers.StoreProvider.Companion.fragmentStore
import org.praha.praha.R
import org.praha.praha.e2e.SystemInsetsPaddedFragment
import org.praha.praha.ext.openToBrowser
import org.praha.praha.ext.requireComponents
import org.praha.praha.ext.showToolbar
import org.praha.praha.settings.emailmasks.middleware.EmailMasksNavigationMiddleware
import org.praha.praha.settings.emailmasks.middleware.EmailMasksPreferencesMiddleware
import org.praha.praha.settings.emailmasks.middleware.EmailMasksTelemetryMiddleware
import org.praha.praha.theme.PrahaTheme

/** Fragment host for the Email Masks settings screen. */
class EmailMasksSettingsFragment : Fragment(), SystemInsetsPaddedFragment {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PrahaTheme {
            val store by
                fragmentStore(
                    initialState =
                        EmailMasksState(
                            isSuggestMasksEnabled = requireComponents.emailMasksRepository.isSuggestionEnabled()
                        )
                ) { state ->
                    createEmailMasksStore(state)
                }

            EmailMasksSettingsScreen(store)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_email_masks))
    }

    private fun createEmailMasksStore(initialState: EmailMasksState) =
        EmailMasksStore(
            initialState = initialState,
            middleware =
                listOf(
                    EmailMasksNavigationMiddleware(
                        openTab = { url ->
                            if (isAdded) {
                                findNavController().openToBrowser()
                                requireComponents.useCases.fenixBrowserUseCases.loadUrlOrSearch(
                                    searchTermOrURL = url,
                                    newTab = true,
                                )
                            }
                        }
                    ),
                    EmailMasksPreferencesMiddleware(requireComponents.emailMasksRepository),
                    EmailMasksTelemetryMiddleware(),
                ),
        )
}
